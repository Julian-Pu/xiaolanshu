package com.xiaolanshu.interaction;

import com.xiaolanshu.common.ApiException;
import com.xiaolanshu.interest.InterestService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class InteractionService {

    private final JdbcTemplate jdbc;
    private final InterestService interestService;
    private final com.xiaolanshu.notification.NotificationPushService notificationPush;

    public InteractionService(JdbcTemplate jdbc, InterestService interestService,
                              com.xiaolanshu.notification.NotificationPushService notificationPush) {
        this.jdbc = jdbc;
        this.interestService = interestService;
        this.notificationPush = notificationPush;
    }

    public record LikeResult(boolean liked, int likeCount) {
    }

    public record CollectResult(boolean collected, int collectCount) {
    }

    @Transactional
    public LikeResult like(long postId, long userId) {
        requirePost(postId);
        if (exists("likes", userId, postId)) {
            throw ApiException.conflict("已点赞");
        }
        jdbc.update("INSERT INTO likes (user_id, post_id, created_at) VALUES (?, ?, ?)",
                userId, postId, Instant.now().toString());
        jdbc.update("UPDATE posts SET like_count = like_count + 1 WHERE id = ?", postId);
        notifyOwner(postId, userId, "like", "赞了你的笔记");
        interestService.learn(userId, postId);
        return new LikeResult(true, count("likes", postId));
    }

    @Transactional
    public LikeResult unlike(long postId, long userId) {
        if (!exists("likes", userId, postId)) {
            throw ApiException.conflict("尚未点赞");
        }
        jdbc.update("DELETE FROM likes WHERE user_id = ? AND post_id = ?", userId, postId);
        jdbc.update("UPDATE posts SET like_count = GREATEST(like_count - 1, 0) WHERE id = ?", postId);
        return new LikeResult(false, count("likes", postId));
    }

    @Transactional
    public CollectResult collect(long postId, long userId) {
        requirePost(postId);
        if (exists("collects", userId, postId)) {
            throw ApiException.conflict("已收藏");
        }
        jdbc.update("INSERT INTO collects (user_id, post_id, created_at) VALUES (?, ?, ?)",
                userId, postId, Instant.now().toString());
        jdbc.update("UPDATE posts SET collect_count = collect_count + 1 WHERE id = ?", postId);
        notifyOwner(postId, userId, "collect", "收藏了你的笔记");
        interestService.learn(userId, postId);
        return new CollectResult(true, count("collects", postId));
    }

    @Transactional
    public CollectResult uncollect(long postId, long userId) {
        if (!exists("collects", userId, postId)) {
            throw ApiException.conflict("尚未收藏");
        }
        jdbc.update("DELETE FROM collects WHERE user_id = ? AND post_id = ?", userId, postId);
        jdbc.update("UPDATE posts SET collect_count = GREATEST(collect_count - 1, 0) WHERE id = ?", postId);
        return new CollectResult(false, count("collects", postId));
    }

    // ---- helpers ----

    private void requirePost(long postId) {
        ListRow row = row("SELECT id, user_id, status FROM posts WHERE id = ?", postId);
        if (row == null || "deleted".equals(row.get("status"))) {
            throw ApiException.notFound("笔记不存在");
        }
    }

    private void notifyOwner(long postId, long actorId, String type, String content) {
        Long ownerId = jdbc.queryForObject("SELECT user_id FROM posts WHERE id = ?", Long.class, postId);
        if (ownerId == null || ownerId == actorId) {
            return;
        }
        String now = Instant.now().toString();
        jdbc.update("""
                INSERT INTO notifications (user_id, actor_id, type, ref_id, content, is_read, created_at)
                VALUES (?, ?, ?, ?, ?, 0, ?)
                """, ownerId, actorId, type, postId, content, now);
        // 多端实时推送：在线端（Web/App）经 WS 收 notify 帧即时刷新
        Long id = jdbc.queryForObject(
                "SELECT id FROM notifications WHERE user_id = ? AND created_at = ?", Long.class, ownerId, now);
        if (id != null) {
            notificationPush.push(ownerId, actorId, type, postId, content, now, id);
        }
    }

    private boolean exists(String table, long userId, long postId) {
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE user_id = ? AND post_id = ?", Long.class, userId, postId);
        return c != null && c > 0;
    }

    private int count(String table, long postId) {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE post_id = ?", Long.class, postId);
        return c == null ? 0 : c.intValue();
    }

    private ListRow row(String sql, Object... args) {
        var rows = jdbc.queryForList(sql, args);
        if (rows.isEmpty()) {
            return null;
        }
        return new ListRow(rows.get(0));
    }

    /** 轻量 Map 包装，避免每个模块都重复 import */
    public static class ListRow {
        private final java.util.Map<String, Object> map;

        ListRow(java.util.Map<String, Object> map) {
            this.map = map;
        }

        public Object get(String key) {
            return map.get(key);
        }
    }
}
