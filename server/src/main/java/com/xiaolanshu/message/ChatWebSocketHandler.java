package com.xiaolanshu.message;

import com.xiaolanshu.security.JwtUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 私信 WebSocket 处理器：/ws 端点。
 * 协议（JSON 文本帧）：
 *   C→S: {"type":"auth","token":"<jwt>"}   连接后立即认证
 *        {"type":"ping"}                   心跳
 *        {"type":"typing","conversationId":N}   我正在输入（转发对端）
 *        {"type":"read","conversationId":N}     标记该会话对方消息已读（转发对端 ack）
 *   S→C: {"type":"auth_ok","userId":...}
 *        {"type":"pong"}
 *        {"type":"message","conversationId":...,"message":{...}}  对方发来新私信
 *        {"type":"typing","conversationId":N,"senderId":...}      对方正在输入
 *        {"type":"read_ack","conversationId":N,"senderId":...}    对方已读回执
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<MessageService> messageServiceProvider;
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(JwtUtil jwtUtil, ObjectMapper objectMapper,
                                ObjectProvider<MessageService> messageServiceProvider) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.messageServiceProvider = messageServiceProvider;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        try {
            var node = objectMapper.readTree(payload);
            String type = node.path("type").asText("");
            if ("auth".equals(type)) {
                long userId = jwtUtil.parseUserId(node.path("token").asText());
                WebSocketSession old = sessions.put(userId, session);
                if (old != null && old.isOpen() && old != session) {
                    old.close(CloseStatus.NORMAL);
                }
                session.getAttributes().put("userId", userId);
                session.sendMessage(new TextMessage(
                        objectMapper.writeValueAsString(Map.of("type", "auth_ok", "userId", userId))));
            } else if ("ping".equals(type)) {
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            } else if ("typing".equals(type) || "read".equals(type)) {
                Object uid = session.getAttributes().get("userId");
                if (uid instanceof Long userId && node.has("conversationId")) {
                    long convId = node.path("conversationId").asLong();
                    MessageService ms = messageServiceProvider.getIfAvailable();
                    if (ms != null) {
                        long peerId = ms.peerId(convId, userId);
                        if ("typing".equals(type)) {
                            pushToUser(peerId, objectMapper.writeValueAsString(
                                    Map.of("type", "typing", "conversationId", convId, "senderId", userId)));
                        } else {
                            ms.markRead(convId, userId);
                            pushToUser(peerId, objectMapper.writeValueAsString(
                                    Map.of("type", "read_ack", "conversationId", convId, "senderId", userId)));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // 非法帧 / 过期 token：忽略
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object uid = session.getAttributes().get("userId");
        if (uid instanceof Long id) {
            sessions.remove(id, session);
        }
    }

    /** 向在线用户推送 JSON 文本；未在线则忽略（离线消息由 REST 拉取兜底） */
    public void pushToUser(long userId, String payload) {
        WebSocketSession s = sessions.get(userId);
        if (s != null && s.isOpen()) {
            try {
                s.sendMessage(new TextMessage(payload));
            } catch (Exception ignored) {
            }
        }
    }
}
