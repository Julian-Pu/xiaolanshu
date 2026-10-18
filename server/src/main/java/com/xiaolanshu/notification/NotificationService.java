package com.xiaolanshu.notification;

import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import com.xiaolanshu.user.dto.UserBrief;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final JdbcTemplate jdbc;

    public NotificationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record NotificationView(
            Long id,
            String type,
            Long refId,
            String content,
            boolean isRead,
            String createdAt,
            UserBrief actor
    ) {
    }

    public PageResult<NotificationView> list(long userId, String type, PageParams pp) {
        Long total;
        List<Map<String, Object>> rows;
        if (type == null || type.isBlank()) {
            total = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM notifications WHERE user_id = ?", Long.class, userId);
            rows = jdbc.queryForList("""
                    SELECT n.*, u.nickname AS actor_nickname, u.avatar AS actor_avatar
                    FROM notifications n LEFT JOIN users u ON u.id = n.actor_id
                    WHERE n.user_id = ? ORDER BY n.created_at DESC LIMIT ? OFFSET ?
                    """, userId, pp.size(), pp.offset());
        } else {
            total = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = ?", Long.class, userId, type);
            rows = jdbc.queryForList("""
                    SELECT n.*, u.nickname AS actor_nickname, u.avatar AS actor_avatar
                    FROM notifications n LEFT JOIN users u ON u.id = n.actor_id
                    WHERE n.user_id = ? AND n.type = ? ORDER BY n.created_at DESC LIMIT ? OFFSET ?
                    """, userId, type, pp.size(), pp.offset());
        }

        List<NotificationView> views = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            UserBrief actor = null;
            Object actorId = row.get("actor_id");
            if (actorId != null) {
                actor = new UserBrief(
                        ((Number) actorId).longValue(),
                        row.get("actor_nickname") == null ? "用户" : (String) row.get("actor_nickname"),
                        (String) row.get("actor_avatar"),
                        false);
            }
            Object refId = row.get("ref_id");
            views.add(new NotificationView(
                    ((Number) row.get("id")).longValue(),
                    (String) row.get("type"),
                    refId == null ? null : ((Number) refId).longValue(),
                    (String) row.get("content"),
                    ((Number) row.get("is_read")).intValue() == 1,
                    (String) row.get("created_at"),
                    actor));
        }
        return PageResult.of(views, total, pp.page(), pp.size());
    }

    /** 未读通知数（含 type 过滤） */
    public int unreadCount(long userId, String type) {
        if (type == null || type.isBlank()) {
            Long c = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0", Long.class, userId);
            return c == null ? 0 : c.intValue();
        }
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0 AND type = ?",
                Long.class, userId, type);
        return c == null ? 0 : c.intValue();
    }

    @Transactional
    public int readAll(long userId) {
        return jdbc.update("UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0", userId);
    }
}
