package com.xiaolanshu.common;

import com.xiaolanshu.audit.AuditService;
import com.xiaolanshu.common.ApiException;
import com.xiaolanshu.feed.FeedHotCacheService;
import com.xiaolanshu.feed.RecommendationMaterialTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理接口（运维用）：全部需要请求头 X-Admin-Key 校验（密钥经 XIAOLANSHU_ADMIN_KEY 注入）。
 * POST /api/admin/reconcile-counts    手动触发计数对账（对账后驱逐缓存）
 * POST /api/admin/rebuild-material    手动重建推荐离线物料池
 * POST /api/admin/warm-hot-feed       手动预热推荐热点榜（Redis）
 * GET  /api/admin/audit/logs          内容安全审核日志（最近 50 条）
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CountReconcileTask reconcileTask;
    private final AuditService auditService;
    private final RecommendationMaterialTask materialTask;
    private final FeedHotCacheService feedHotCache;
    private final String adminKey;

    public AdminController(CountReconcileTask reconcileTask, AuditService auditService,
                           RecommendationMaterialTask materialTask, FeedHotCacheService feedHotCache,
                           @Value("${xiaolanshu.admin-key:dev-admin-key}") String adminKey) {
        this.reconcileTask = reconcileTask;
        this.auditService = auditService;
        this.materialTask = materialTask;
        this.feedHotCache = feedHotCache;
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
}
