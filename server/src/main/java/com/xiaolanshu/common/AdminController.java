package com.xiaolanshu.common;

import com.xiaolanshu.audit.AuditService;
import com.xiaolanshu.feed.RecommendationMaterialTask;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理接口（演示/运维用，仅限本机联调）：
 * POST /api/admin/reconcile-counts    手动触发计数对账
 * POST /api/admin/rebuild-material    手动重建推荐离线物料池
 * GET  /api/admin/audit/logs          内容安全审核日志（最近 50 条）
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CountReconcileTask reconcileTask;
    private final AuditService auditService;
    private final RecommendationMaterialTask materialTask;

    public AdminController(CountReconcileTask reconcileTask, AuditService auditService,
                           RecommendationMaterialTask materialTask) {
        this.reconcileTask = reconcileTask;
        this.auditService = auditService;
        this.materialTask = materialTask;
    }

    @PostMapping("/reconcile-counts")
    public Map<String, Object> reconcileCounts() {
        long changed = reconcileTask.reconcile();
        return Map.of("code", 0, "message", "ok", "data", Map.of("changedRows", changed));
    }

    @PostMapping("/rebuild-material")
    public Map<String, Object> rebuildMaterial() {
        Map<String, Object> r = materialTask.rebuildAll();
        return Map.of("code", 0, "message", "ok", "data", r);
    }

    @GetMapping("/audit/logs")
    public Map<String, Object> auditLogs() {
        List<Map<String, Object>> logs = auditService.recentLogs();
        return Map.of("code", 0, "message", "ok", "data", Map.of("list", logs));
    }
}
