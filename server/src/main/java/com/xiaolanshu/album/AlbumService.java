package com.xiaolanshu.album;

import com.xiaolanshu.common.ApiException;
import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import com.xiaolanshu.album.dto.AlbumView;
import com.xiaolanshu.post.PostService;
import com.xiaolanshu.post.dto.PostCardView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AlbumService {

    private final JdbcTemplate jdbc;
    private final PostService postService;

    public AlbumService(JdbcTemplate jdbc, PostService postService) {
        this.jdbc = jdbc;
        this.postService = postService;
    }

    /** 我的专辑列表（含笔记数） */
    public List<AlbumView> list(long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT a.id, a.name, a.created_at, COUNT(ap.post_id) AS cnt
                FROM albums a LEFT JOIN album_posts ap ON ap.album_id = a.id
                WHERE a.user_id = ?
                GROUP BY a.id, a.name, a.created_at
                ORDER BY a.created_at DESC
                """, userId);
        List<AlbumView> views = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            views.add(new AlbumView(
                    ((Number) row.get("id")).longValue(),
                    (String) row.get("name"),
                    ((Number) row.get("cnt")).intValue(),
                    (String) row.get("created_at")));
        }
        return views;
    }

    @Transactional
    public AlbumView create(long userId, String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.badRequest("专辑名称不能为空");
        }
        if (trimmed.length() > 50) {
            throw ApiException.badRequest("专辑名称最长 50 字");
        }
        String now = Instant.now().toString();
        jdbc.update("INSERT INTO albums (user_id, name, created_at) VALUES (?, ?, ?)", userId, trimmed, now);
        Long id = jdbc.queryForObject(
                "SELECT id FROM albums WHERE user_id = ? AND created_at = ?", Long.class, userId, now);
        return new AlbumView(id, trimmed, 0, now);
    }

    @Transactional
    public void delete(long userId, long albumId) {
        getOwned(userId, albumId);
        jdbc.update("DELETE FROM album_posts WHERE album_id = ?", albumId);
        jdbc.update("DELETE FROM albums WHERE id = ?", albumId);
    }

    /** 专辑内笔记（分页） */
    public PageResult<PostCardView> posts(long userId, long albumId, PageParams pp) {
        getOwned(userId, albumId);
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM album_posts WHERE album_id = ?", Long.class, albumId);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.* FROM album_posts ap JOIN posts p ON p.id = ap.post_id
                WHERE ap.album_id = ? AND p.status = 'published'
                ORDER BY ap.created_at DESC LIMIT ? OFFSET ?
                """, albumId, pp.size(), pp.offset());
        return PageResult.of(postService.buildCards(rows, userId), total, pp.page(), pp.size());
    }

    /** 把笔记加入专辑（幂等） */
    @Transactional
    public void addPost(long userId, long albumId, long postId) {
        getOwned(userId, albumId);
        postService.getPostRow(postId); // 笔记必须存在
        jdbc.update("""
                INSERT INTO album_posts (album_id, post_id, created_at) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE post_id = post_id
                """, albumId, postId, Instant.now().toString());
    }

    /** 从专辑移除笔记（幂等） */
    @Transactional
    public void removePost(long userId, long albumId, long postId) {
        getOwned(userId, albumId);
        jdbc.update("DELETE FROM album_posts WHERE album_id = ? AND post_id = ?", albumId, postId);
    }

    private Map<String, Object> getOwned(long userId, long albumId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM albums WHERE id = ? AND user_id = ?", albumId, userId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("专辑不存在");
        }
        return rows.get(0);
    }
}
