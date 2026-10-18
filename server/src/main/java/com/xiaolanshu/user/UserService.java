package com.xiaolanshu.user;

import com.xiaolanshu.common.ApiException;
import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import com.xiaolanshu.user.dto.ChangePasswordRequest;
import com.xiaolanshu.user.dto.UpdateProfileRequest;
import com.xiaolanshu.user.dto.UserView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final com.xiaolanshu.notification.NotificationPushService notificationPush;

    public UserService(JdbcTemplate jdbc,
                       com.xiaolanshu.notification.NotificationPushService notificationPush) {
        this.jdbc = jdbc;
        this.notificationPush = notificationPush;
    }

    public UserView profile(long id, Long viewerId) {
        Map<String, Object> row = getRow(id);
        boolean isFollowing = viewerId != null && isFollowing(viewerId, id);
        return toView(row, isFollowing);
    }

    @Transactional
    public UserView updateMe(long userId, UpdateProfileRequest req) {
        Map<String, Object> current = getRow(userId);
        String nickname = req.nickname() != null ? req.nickname() : (String) current.get("nickname");
        String bio = req.bio() != null ? req.bio() : (String) current.get("bio");
        String avatar = req.avatar() != null ? req.avatar() : (String) current.get("avatar");
        String gender = req.gender() != null ? req.gender() : (String) current.get("gender");
        jdbc.update("UPDATE users SET nickname = ?, bio = ?, avatar = ?, gender = ?, updated_at = ? WHERE id = ?",
                nickname, bio, avatar, gender, Instant.now().toString(), userId);
        return toView(getRow(userId), false);
    }

    /** 修改密码：校验原密码后更新 password_hash */
    @Transactional
    public void changePassword(long userId, ChangePasswordRequest req) {
        Map<String, Object> row = getRow(userId);
        String stored = (String) row.get("password_hash");
        if (stored == null || !encoder.matches(req.oldPassword(), stored)) {
            throw ApiException.badRequest("原密码错误");
        }
        jdbc.update("UPDATE users SET password_hash = ?, updated_at = ? WHERE id = ?",
                encoder.encode(req.newPassword()), Instant.now().toString(), userId);
    }

    @Transactional
    public void follow(long targetId, long userId) {
        if (targetId == userId) {
            throw ApiException.badRequest("不能关注自己");
        }
        getRow(targetId); // 目标必须存在
        Long exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM follows WHERE user_id = ? AND follow_user_id = ?", Long.class, userId, targetId);
        if (exists != null && exists > 0) {
            throw ApiException.conflict("已关注该用户");
        }
        String now = Instant.now().toString();
        jdbc.update("INSERT INTO follows (user_id, follow_user_id, created_at) VALUES (?, ?, ?)", userId, targetId, now);
        jdbc.update("UPDATE users SET following_count = following_count + 1 WHERE id = ?", userId);
        jdbc.update("UPDATE users SET follower_count = follower_count + 1 WHERE id = ?", targetId);
        jdbc.update("""
                INSERT INTO notifications (user_id, actor_id, type, ref_id, content, is_read, created_at)
                VALUES (?, ?, 'follow', ?, '关注了你', 0, ?)
                """, targetId, userId, userId, now);
        // 多端实时推送
        Long id = jdbc.queryForObject(
                "SELECT id FROM notifications WHERE user_id = ? AND created_at = ?", Long.class, targetId, now);
        if (id != null) {
            notificationPush.push(targetId, userId, "follow", userId, "关注了你", now, id);
        }
    }

    @Transactional
    public void unfollow(long targetId, long userId) {
        Long exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM follows WHERE user_id = ? AND follow_user_id = ?", Long.class, userId, targetId);
        if (exists == null || exists == 0) {
            throw ApiException.conflict("尚未关注该用户");
        }
        jdbc.update("DELETE FROM follows WHERE user_id = ? AND follow_user_id = ?", userId, targetId);
        jdbc.update("UPDATE users SET following_count = GREATEST(following_count - 1, 0) WHERE id = ?", userId);
        jdbc.update("UPDATE users SET follower_count = GREATEST(follower_count - 1, 0) WHERE id = ?", targetId);
    }

    public PageResult<UserView> followingList(long userId, PageParams pp) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM follows WHERE user_id = ?", Long.class, userId);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT u.* FROM follows f JOIN users u ON u.id = f.follow_user_id
                WHERE f.user_id = ? ORDER BY f.created_at DESC LIMIT ? OFFSET ?
                """, userId, pp.size(), pp.offset());
        return PageResult.of(toViews(rows), total, pp.page(), pp.size());
    }

    public PageResult<UserView> followersList(long userId, PageParams pp) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM follows WHERE follow_user_id = ?", Long.class, userId);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT u.* FROM follows f JOIN users u ON u.id = f.user_id
                WHERE f.follow_user_id = ? ORDER BY f.created_at DESC LIMIT ? OFFSET ?
                """, userId, pp.size(), pp.offset());
        return PageResult.of(toViews(rows), total, pp.page(), pp.size());
    }

    // ---- helpers ----

    private Map<String, Object> getRow(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM users WHERE id = ?", id);
        if (rows.isEmpty()) {
            throw ApiException.notFound("用户不存在");
        }
        return rows.get(0);
    }

    private boolean isFollowing(long viewerId, long targetId) {
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM follows WHERE user_id = ? AND follow_user_id = ?", Long.class, viewerId, targetId);
        return c != null && c > 0;
    }

    private List<UserView> toViews(List<Map<String, Object>> rows) {
        List<UserView> views = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            views.add(toView(row, false));
        }
        return views;
    }

    UserView toView(Map<String, Object> row, boolean isFollowing) {
        return new UserView(
                ((Number) row.get("id")).longValue(),
                (String) row.get("nickname"),
                (String) row.get("avatar"),
                (String) row.get("bio"),
                (String) row.get("gender"),
                ((Number) row.get("following_count")).intValue(),
                ((Number) row.get("follower_count")).intValue(),
                ((Number) row.get("post_count")).intValue(),
                (String) row.get("created_at"),
                isFollowing
        );
    }
}
