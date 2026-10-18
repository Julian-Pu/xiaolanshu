package com.xiaolanshu.comment;

import com.xiaolanshu.comment.dto.CommentView;
import com.xiaolanshu.common.ApiException;
import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import com.xiaolanshu.user.dto.UserBrief;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentService {

    private final JdbcTemplate jdbc;
    private final com.xiaolanshu.interest.InterestService interestService;
    private final com.xiaolanshu.audit.AuditService auditService;
    private final com.xiaolanshu.notification.NotificationPushService notificationPush;

    public CommentService(JdbcTemplate jdbc, com.xiaolanshu.interest.InterestService interestService,
                          com.xiaolanshu.audit.AuditService auditService,
                          com.xiaolanshu.notification.NotificationPushService notificationPush) {
        this.jdbc = jdbc;
        this.interestService = interestService;
        this.auditService = auditService;
        this.notificationPush = notificationPush;
    }

    public PageResult<CommentView> list(long postId, PageParams pp) {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM comments WHERE post_id = ? AND parent_id IS NULL", Long.class, postId);
        List<Map<String, Object>> roots = jdbc.queryForList("""
                SELECT c.*, u.nickname AS user_nickname, u.avatar AS user_avatar
                FROM comments c JOIN users u ON u.id = c.user_id
                WHERE c.post_id = ? AND c.parent_id IS NULL
                ORDER BY c.created_at ASC LIMIT ? OFFSET ?
                """, postId, pp.size(), pp.offset());

        List<CommentView> result = new ArrayList<>();
        if (!roots.isEmpty()) {
            List<Long> rootIds = new ArrayList<>();
            for (Map<String, Object> row : roots) {
                rootIds.add(((Number) row.get("id")).longValue());
            }
            Map<Long, List<CommentView>> repliesMap = loadReplies(rootIds);
            for (Map<String, Object> row : roots) {
                long id = ((Number) row.get("id")).longValue();
                result.add(toView(row, repliesMap.getOrDefault(id, List.of())));
            }
        }
        return PageResult.of(result, total, pp.page(), pp.size());
    }

    @Transactional
    public CommentView add(long postId, long userId, String content, Long parentId) {
        // 内容安全：评论前文本机审
        auditService.check(userId, "comment", content);
        Map<String, Object> post = postRow(postId);
        if (post == null) {
            throw ApiException.notFound("笔记不存在");
        }
        long postOwnerId = ((Number) post.get("user_id")).longValue();

        Long replyTo = null;
        if (parentId != null) {
            Map<String, Object> parent = commentRow(parentId);
            if (parent == null || ((Number) parent.get("post_id")).longValue() != postId) {
                throw ApiException.badRequest("回复的评论不存在");
            }
            replyTo = ((Number) parent.get("user_id")).longValue();
        }

        String now = Instant.now().toString();
        jdbc.update("""
                INSERT INTO comments (post_id, user_id, parent_id, reply_to_user_id, content, like_count, created_at)
                VALUES (?, ?, ?, ?, ?, 0, ?)
                """, postId, userId, parentId, replyTo, content, now);
        jdbc.update("UPDATE posts SET comment_count = comment_count + 1 WHERE id = ?", postId);
        interestService.learn(userId, postId);

        long commentId = jdbc.queryForObject(
                "SELECT id FROM comments WHERE user_id = ? AND created_at = ?", Long.class, userId, now);
        Map<String, Object> inserted = commentRow(commentId);
        Map<String, Object> authorRow = userRow(userId);

        // 通知：评论/回复
        if (parentId != null) {
            if (replyTo != null && replyTo != userId) {
                notifyUser(replyTo, userId, "comment", postId, "回复了你的评论");
            }
        }
        if (postOwnerId != userId && (replyTo == null || replyTo != postOwnerId)) {
            notifyUser(postOwnerId, userId, "comment", postId, "评论了你的笔记");
        }

        UserBrief user = new UserBrief(userId, (String) authorRow.get("nickname"), (String) authorRow.get("avatar"), false);
        return new CommentView(commentId, content, 0, now, user, null, List.of());
    }

    @Transactional
    public void delete(long commentId, long userId) {
        Map<String, Object> comment = commentRow(commentId);
        if (comment == null) {
            throw ApiException.notFound("评论不存在");
        }
        long postId = ((Number) comment.get("post_id")).longValue();
        Map<String, Object> post = postRow(postId);
        long postOwnerId = post == null ? -1 : ((Number) post.get("user_id")).longValue();
        if (((Number) comment.get("user_id")).longValue() != userId && postOwnerId != userId) {
            throw ApiException.forbidden("无权删除该评论");
        }
        jdbc.update("DELETE FROM comments WHERE id = ?", commentId);
        // 删除评论后重算计数，保证一致性
        jdbc.update("""
                UPDATE posts SET comment_count =
                    (SELECT COUNT(*) FROM comments WHERE post_id = ?) WHERE id = ?
                """, postId, postId);
    }

    // ---- helpers ----

    private Map<Long, List<CommentView>> loadReplies(List<Long> rootIds) {
        String placeholders = String.join(",", java.util.Collections.nCopies(rootIds.size(), "?"));
        String sql = """
                SELECT c.*, u.nickname AS user_nickname, u.avatar AS user_avatar,
                       ru.nickname AS reply_nickname
                FROM comments c
                JOIN users u ON u.id = c.user_id
                LEFT JOIN users ru ON ru.id = c.reply_to_user_id
                WHERE c.parent_id IN (__IDS__)
                ORDER BY c.created_at ASC
                """.replace("__IDS__", placeholders);
        List<Map<String, Object>> replies = jdbc.queryForList(sql, rootIds.toArray());

        Map<Long, List<CommentView>> map = new HashMap<>();
        for (Map<String, Object> row : replies) {
            long parentId = ((Number) row.get("parent_id")).longValue();
            map.computeIfAbsent(parentId, k -> new ArrayList<>()).add(toView(row, List.of()));
        }
        return map;
    }

    private CommentView toView(Map<String, Object> row, List<CommentView> replies) {
        long userId = ((Number) row.get("user_id")).longValue();
        UserBrief replyTo = null;
        Object replyToId = row.get("reply_to_user_id");
        Object replyNickname = row.get("reply_nickname");
        if (replyToId != null) {
            replyTo = new UserBrief(((Number) replyToId).longValue(),
                    replyNickname == null ? "用户" : (String) replyNickname, null, false);
        }
        return new CommentView(
                ((Number) row.get("id")).longValue(),
                (String) row.get("content"),
                ((Number) row.get("like_count")).intValue(),
                (String) row.get("created_at"),
                new UserBrief(userId, (String) row.get("user_nickname"), (String) row.get("user_avatar"), false),
                replyTo,
                replies);
    }

    private Map<String, Object> postRow(long postId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM posts WHERE id = ?", postId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> commentRow(long commentId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM comments WHERE id = ?", commentId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<String, Object> userRow(long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM users WHERE id = ?", userId);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private void notifyUser(long receiverId, long actorId, String type, long refId, String content) {
        String now = Instant.now().toString();
        jdbc.update("""
                INSERT INTO notifications (user_id, actor_id, type, ref_id, content, is_read, created_at)
                VALUES (?, ?, ?, ?, ?, 0, ?)
                """, receiverId, actorId, type, refId, content, now);
        // 多端实时推送
        Long id = jdbc.queryForObject(
                "SELECT id FROM notifications WHERE user_id = ? AND created_at = ?", Long.class, receiverId, now);
        if (id != null) {
            notificationPush.push(receiverId, actorId, type, refId, content, now, id);
        }
    }
}
