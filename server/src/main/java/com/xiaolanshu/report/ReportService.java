package com.xiaolanshu.report;

import com.xiaolanshu.common.ApiException;
import com.xiaolanshu.common.PostCacheService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 举报服务：
 * - 用户侧：举报笔记（同一用户同一笔记仅保留一条 PENDING，重复举报返回已提交）
 * - 管理侧：举报列表 / 审核（APPROVED → 笔记置 deleted；IGNORED → 忽略）
 */
@Service
public class ReportService {

    private final JdbcTemplate jdbc;
    private final PostCacheService postCache;

    public ReportService(JdbcTemplate jdbc, PostCacheService postCache) {
        this.jdbc = jdbc;
        this.postCache = postCache;
    }

    @Transactional
    public Map<String, Object> create(long userId, Long postId, String reason, String detail) {
        List<Map<String, Object>> postRows = jdbc.queryForList(
                "SELECT id FROM posts WHERE id = ? AND status = 'published'", postId);
        if (postRows.isEmpty()) {
            throw ApiException.notFound("笔记不存在或已删除");
        }
        Long existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM reports WHERE post_id = ? AND user_id = ? AND status = 'PENDING'",
                Long.class, postId, userId);
        if (existing != null && existing > 0) {
            return Map.of("id", -1L, "duplicated", true);
        }
        String now = Instant.now().toString();
        jdbc.update("""
                INSERT INTO reports (post_id, user_id, reason, detail, status, created_at)
                VALUES (?, ?, ?, ?, 'PENDING', ?)
                """, postId, userId, reason, detail, now);
        Long id = jdbc.queryForObject(
                "SELECT id FROM reports WHERE post_id = ? AND user_id = ? AND created_at = ?",
                Long.class, postId, userId, now);
        return Map.of("id", id == null ? 0L : id, "duplicated", false);
    }

    /** 举报列表（管理端）：status 可空 = 全部 */
    public List<Map<String, Object>> list(String status, int limit, int offset) {
        String where = "";
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            where = " WHERE r.status = ?";
            args.add(status);
        }
        args.add(limit);
        args.add(offset);
        return jdbc.queryForList("""
                SELECT r.id, r.post_id, r.reason, r.detail, r.status, r.created_at, r.handled_at,
                       p.title AS post_title, p.status AS post_status,
                       u.nickname AS reporter_nickname
                FROM reports r
                JOIN posts p ON p.id = r.post_id
                JOIN users u ON u.id = r.user_id
                """ + where + " ORDER BY r.created_at DESC LIMIT ? OFFSET ?", args.toArray());
    }

    @Transactional
    public void review(long reportId, String action) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM reports WHERE id = ?", reportId);
        if (rows.isEmpty()) {
            throw ApiException.notFound("举报记录不存在");
        }
        Map<String, Object> report = rows.get(0);
        if (!"PENDING".equals(report.get("status"))) {
            throw ApiException.badRequest("该举报已处理");
        }
        String now = Instant.now().toString();
        if ("APPROVE".equals(action)) {
            // 举报成立：笔记软删 + 计数回退
            long postId = ((Number) report.get("post_id")).longValue();
            List<Map<String, Object>> postRows = jdbc.queryForList("SELECT * FROM posts WHERE id = ?", postId);
            if (!postRows.isEmpty() && "published".equals(postRows.get(0).get("status"))) {
                Map<String, Object> post = postRows.get(0);
                jdbc.update("UPDATE posts SET status = 'deleted', updated_at = ? WHERE id = ?", now, postId);
                long authorId = ((Number) post.get("user_id")).longValue();
                jdbc.update("UPDATE users SET post_count = GREATEST(post_count - 1, 0) WHERE id = ?", authorId);
                Number topicId = (Number) post.get("topic_id");
                if (topicId != null) {
                    jdbc.update("UPDATE topics SET post_count = GREATEST(post_count - 1, 0) WHERE id = ?", topicId.longValue());
                }
                try {
                    postCache.evict(postId);
                } catch (Exception ignored) {
                    // 缓存驱逐失败不影响审核主流程
                }
            }
            jdbc.update("UPDATE reports SET status = 'APPROVED', handled_at = ? WHERE id = ?", now, reportId);
        } else if ("IGNORE".equals(action)) {
            jdbc.update("UPDATE reports SET status = 'IGNORED', handled_at = ? WHERE id = ?", now, reportId);
        } else {
            throw ApiException.badRequest("审核动作仅支持 APPROVE / IGNORE");
        }
    }
}
