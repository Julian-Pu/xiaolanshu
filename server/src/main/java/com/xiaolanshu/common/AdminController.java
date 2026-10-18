package com.xiaolanshu.common;

import com.xiaolanshu.audit.AuditService;
import com.xiaolanshu.common.ApiException;
import com.xiaolanshu.feed.FeedHotCacheService;
import com.xiaolanshu.feed.RecommendationMaterialTask;
import com.xiaolanshu.post.PostService;
import com.xiaolanshu.report.ReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理接口（运维 + 管理后台）：全部需要请求头 X-Admin-Key 校验（密钥经 XIAOLANSHU_ADMIN_KEY 注入）。
 * POST /api/admin/reconcile-counts    手动触发计数对账（对账后驱逐缓存）
 * POST /api/admin/rebuild-material    手动重建推荐离线物料池
 * POST /api/admin/warm-hot-feed       手动预热推荐热点榜（Redis）
 * GET  /api/admin/audit/logs          内容安全审核日志（最近 50 条）
 * GET  /api/admin/stats               概览统计（用户/笔记/待审核举报/评论）
 * GET  /api/admin/reports             举报列表（status 可空 = 全部）
 * POST /api/admin/reports/{id}/review 举报审核（APPROVE 删笔记 / IGNORE 忽略）
 * GET  /api/admin/posts               笔记管理列表（分页）
 * DELETE /api/admin/posts/{id}        强制删除笔记（软删）
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CountReconcileTask reconcileTask;
    private final AuditService auditService;
    private final RecommendationMaterialTask materialTask;
    private final FeedHotCacheService feedHotCache;
    private final ReportService reportService;
    private final PostService postService;
    private final JdbcTemplate jdbc;
    private final String adminKey;

    public AdminController(CountReconcileTask reconcileTask, AuditService auditService,
                           RecommendationMaterialTask materialTask, FeedHotCacheService feedHotCache,
                           ReportService reportService, PostService postService, JdbcTemplate jdbc,
                           @Value("${xiaolanshu.admin-key:dev-admin-key}") String adminKey) {
        this.reconcileTask = reconcileTask;
        this.auditService = auditService;
        this.materialTask = materialTask;
        this.feedHotCache = feedHotCache;
        this.reportService = reportService;
        this.postService = postService;
        this.jdbc = jdbc;
        this.adminKey = adminKey;
    }

    private void requireAdminKey(String key) {
        if (key == null || !key.equals(adminKey)) {
            throw ApiException.forbidden("管理密钥错误或缺失（请求头 X-Admin-Key）");
        }
    }

    @PostMapping("/reconcile-counts")
    public Map<String, Object> reconcileCounts(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdminKey(key);
        Map<String, Object> r = reconcileTask.reconcileWithCache();
        return Map.of("code", 0, "message", "ok", "data", r);
    }

    @PostMapping("/rebuild-material")
    public Map<String, Object> rebuildMaterial(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdminKey(key);
        Map<String, Object> r = materialTask.rebuildAll();
        return Map.of("code", 0, "message", "ok", "data", r);
    }

    @PostMapping("/warm-hot-feed")
    public Map<String, Object> warmHotFeed(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdminKey(key);
        Map<String, Object> r = feedHotCache.warm();
        return Map.of("code", 0, "message", "ok", "data", r);
    }

    @GetMapping("/audit/logs")
    public Map<String, Object> auditLogs(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdminKey(key);
        List<Map<String, Object>> logs = auditService.recentLogs();
        return Map.of("code", 0, "message", "ok", "data", Map.of("list", logs));
    }

    /** 概览统计 */
    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdminKey(key);
        Long userCount = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        Long postCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM posts WHERE status = 'published'", Long.class);
        Long pendingReports = jdbc.queryForObject(
                "SELECT COUNT(*) FROM reports WHERE status = 'PENDING'", Long.class);
        Long commentCount = jdbc.queryForObject("SELECT COUNT(*) FROM comments", Long.class);
        return Map.of("code", 0, "message", "ok", "data", Map.of(
                "userCount", nvl(userCount),
                "postCount", nvl(postCount),
                "pendingReports", nvl(pendingReports),
                "commentCount", nvl(commentCount)));
    }

    /** 统计报表：近 7 天内容增长 + 热门笔记/话题（演示版 SQL 聚合；生产可换 OLAP/ES 物化视图） */
    @GetMapping("/analytics")
    public Map<String, Object> analytics(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        requireAdminKey(key);
        String since = java.time.Instant.now().minus(java.time.Duration.ofDays(7))
                .toString().substring(0, 10);
        List<Map<String, Object>> dailyPosts = jdbc.queryForList("""
                SELECT LEFT(created_at, 10) AS d, COUNT(*) AS cnt
                FROM posts WHERE status = 'published' AND created_at >= ?
                GROUP BY LEFT(created_at, 10) ORDER BY d
                """, since);
        List<Map<String, Object>> dailyUsers = jdbc.queryForList("""
                SELECT LEFT(created_at, 10) AS d, COUNT(*) AS cnt
                FROM users WHERE created_at >= ?
                GROUP BY LEFT(created_at, 10) ORDER BY d
                """, since);
        List<Map<String, Object>> hotPosts = jdbc.queryForList("""
                SELECT id, title, like_count, collect_count, comment_count, view_count,
                       (like_count * 3 + collect_count * 2 + comment_count * 5 + view_count * 0.2) AS score
                FROM posts WHERE status = 'published'
                ORDER BY score DESC LIMIT 8
                """);
        List<Map<String, Object>> hotTopics = jdbc.queryForList("""
                SELECT name, post_count FROM topics
                ORDER BY post_count DESC LIMIT 8
                """);
        return Map.of("code", 0, "message", "ok", "data", Map.of(
                "dailyPosts", dailyPosts,
                "dailyUsers", dailyUsers,
                "hotPosts", hotPosts,
                "hotTopics", hotTopics));
    }

    /** 举报列表：?status=PENDING|APPROVED|IGNORED（缺省全部），分页 ?page&size */
    @GetMapping("/reports")
    public Map<String, Object> reports(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        requireAdminKey(key);
        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 1);
        String where = (status == null || status.isBlank()) ? "" : " WHERE r.status = ?";
        Long total = status == null || status.isBlank()
                ? jdbc.queryForObject("SELECT COUNT(*) FROM reports", Long.class)
                : jdbc.queryForObject("SELECT COUNT(*) FROM reports WHERE status = ?", Long.class, status);
        List<Map<String, Object>> list = reportService.list(status, size, (page - 1) * size);
        return Map.of("code", 0, "message", "ok", "data",
                Map.of("list", list, "page", page, "size", size, "total", nvl(total)));
    }

    /** 举报审核：body { action: APPROVE | IGNORE } */
    @PostMapping("/reports/{id}/review")
    public Map<String, Object> reviewReport(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                            @PathVariable long id,
                                            @RequestBody Map<String, String> body) {
        requireAdminKey(key);
        reportService.review(id, body.getOrDefault("action", ""));
        return Map.of("code", 0, "message", "ok", "data", Map.of("id", id));
    }

    /** 笔记管理列表（含作者昵称，分页） */
    @GetMapping("/posts")
    public Map<String, Object> posts(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        requireAdminKey(key);
        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 1);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM posts", Long.class);
        List<Map<String, Object>> list = jdbc.queryForList("""
                SELECT p.id, p.title, p.status, p.like_count, p.collect_count, p.comment_count,
                       p.view_count, p.created_at, u.nickname AS author_nickname
                FROM posts p JOIN users u ON u.id = p.user_id
                ORDER BY p.created_at DESC LIMIT ? OFFSET ?
                """, size, (page - 1) * size);
        return Map.of("code", 0, "message", "ok", "data",
                Map.of("list", list, "page", page, "size", size, "total", nvl(total)));
    }

    /** 强制删除笔记（软删 + 计数回退） */
    @DeleteMapping("/posts/{id}")
    public Map<String, Object> deletePost(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                          @PathVariable long id) {
        requireAdminKey(key);
        postService.forceDelete(id);
        return Map.of("code", 0, "message", "ok", "data", Map.of("id", id));
    }

    private static long nvl(Long v) {
        return v == null ? 0L : v;
    }
}
