package com.xiaolanshu.message;

import com.xiaolanshu.common.ApiException;
import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import com.xiaolanshu.user.dto.UserBrief;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class MessageService {

    private final JdbcTemplate jdbc;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ObjectMapper objectMapper;

    public MessageService(JdbcTemplate jdbc, ChatWebSocketHandler chatWebSocketHandler, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.objectMapper = objectMapper;
    }

    public record ConversationView(Long id, String lastMessage, String updatedAt, UserBrief peer, long unreadCount) {
    }

    public record MessageView(
            Long id,
            Long conversationId,
            Long senderId,
            String content,
            boolean isRead,
            String createdAt
    ) {
    }

    public List<ConversationView> conversations(long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT * FROM conversations
                WHERE user_a = ? OR user_b = ?
                ORDER BY updated_at DESC
                """, userId, userId);
        List<ConversationView> views = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            long a = ((Number) row.get("user_a")).longValue();
            long b = ((Number) row.get("user_b")).longValue();
            long peerId = a == userId ? b : a;
            Map<String, Object> peer = userRow(peerId);
            long convId = ((Number) row.get("id")).longValue();
            Long unread = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM messages
                    WHERE conversation_id = ? AND sender_id != ? AND is_read = 0
                    """, Long.class, convId, userId);
            views.add(new ConversationView(
                    convId,
                    (String) row.get("last_message"),
                    (String) row.get("updated_at"),
                    peer.isEmpty()
                            ? new UserBrief(peerId, "用户", null, false)
                            : new UserBrief(peerId, (String) peer.get("nickname"), (String) peer.get("avatar"), false),
                    unread));
        }
        return views;
    }

    @Transactional
    public ConversationView getOrCreate(long userId, long peerId) {
        if (peerId == userId) {
            throw ApiException.badRequest("不能和自己聊天");
        }
        if (userRow(peerId).isEmpty()) {
            throw ApiException.notFound("用户不存在");
        }
        long a = Math.min(userId, peerId);
        long b = Math.max(userId, peerId);
        Map<String, Object> conv = conversationByPair(a, b);
        if (conv == null) {
            String now = Instant.now().toString();
            try {
                jdbc.update("INSERT INTO conversations (user_a, user_b, last_message, updated_at) VALUES (?, ?, NULL, ?)",
                        a, b, now);
            } catch (DuplicateKeyException ignored) {
                // 并发重复创建，读已存在会话
            }
            conv = conversationByPair(a, b);
        }
        long convId = ((Number) conv.get("id")).longValue();
        Map<String, Object> peer = userRow(peerId);
        return new ConversationView(
                convId,
                (String) conv.get("last_message"),
                (String) conv.get("updated_at"),
                new UserBrief(peerId, (String) peer.get("nickname"), (String) peer.get("avatar"), false),
                0);
    }

    @Transactional
    public PageResult<MessageView> messages(long conversationId, long userId, PageParams pp) {
        Map<String, Object> conv = requireMember(conversationId, userId);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM messages WHERE conversation_id = ?", Long.class, conversationId);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT * FROM messages WHERE conversation_id = ?
                ORDER BY id DESC LIMIT ? OFFSET ?
                """, conversationId, pp.size(), pp.offset());
        Collections.reverse(rows);

        List<MessageView> views = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            views.add(new MessageView(
                    ((Number) row.get("id")).longValue(),
                    conversationId,
                    ((Number) row.get("sender_id")).longValue(),
                    (String) row.get("content"),
                    ((Number) row.get("is_read")).intValue() == 1,
                    (String) row.get("created_at")));
        }

        // 标记对方消息已读
        jdbc.update("""
                UPDATE messages SET is_read = 1
                WHERE conversation_id = ? AND sender_id != ? AND is_read = 0
                """, conversationId, userId);
        return PageResult.of(views, total, pp.page(), pp.size());
    }

    /** 标记会话中对方发来的消息为已读（REST 兜底 + WS read 帧共用） */
    public long markRead(long conversationId, long userId) {
        Map<String, Object> conv = requireMember(conversationId, userId);
        int changed = jdbc.update("""
                UPDATE messages SET is_read = 1
                WHERE conversation_id = ? AND sender_id != ? AND is_read = 0
                """, conversationId, userId);
        return changed;
    }

    /** 会话对端用户 id（用于 WS typing/read 转发；非成员抛 403） */
    public long peerId(long conversationId, long userId) {
        Map<String, Object> conv = requireMember(conversationId, userId);
        long a = ((Number) conv.get("user_a")).longValue();
        long b = ((Number) conv.get("user_b")).longValue();
        return a == userId ? b : a;
    }

    @Transactional
    public MessageView send(long conversationId, long userId, String content) {
        Map<String, Object> conv = requireMember(conversationId, userId);
        String now = Instant.now().toString();
        jdbc.update("""
                INSERT INTO messages (conversation_id, sender_id, content, type, is_read, created_at)
                VALUES (?, ?, ?, 'text', 0, ?)
                """, conversationId, userId, content, now);
        jdbc.update("UPDATE conversations SET last_message = ?, updated_at = ? WHERE id = ?",
                content, now, conversationId);
        long messageId = jdbc.queryForObject(
                "SELECT id FROM messages WHERE conversation_id = ? AND created_at = ?",
                Long.class, conversationId, now);
        MessageView view = new MessageView(messageId, conversationId, userId, content, false, now);

        // WebSocket 实时推送给会话对端
        long a = ((Number) conv.get("user_a")).longValue();
        long b = ((Number) conv.get("user_b")).longValue();
        long peerId = a == userId ? b : a;
        try {
            chatWebSocketHandler.pushToUser(peerId, objectMapper.writeValueAsString(
                    Map.of("type", "message", "conversationId", conversationId, "message", view)));
        } catch (Exception ignored) {
            // 推送失败不影响消息落库（离线由 REST 拉取兜底）
        }
        return view;
    }

    // ---- helpers ----

    private Map<String, Object> conversationByPair(long a, long b) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM conversations WHERE user_a = ? AND user_b = ?", a, b);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> requireMember(long conversationId, long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM conversations WHERE id = ?", conversationId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("会话不存在");
        }
        Map<String, Object> conv = rows.get(0);
        long a = ((Number) conv.get("user_a")).longValue();
        long b = ((Number) conv.get("user_b")).longValue();
        if (a != userId && b != userId) {
            throw ApiException.forbidden("无权访问该会话");
        }
        return conv;
    }

    private Map<String, Object> userRow(long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, nickname, avatar FROM users WHERE id = ?", userId);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }
}
