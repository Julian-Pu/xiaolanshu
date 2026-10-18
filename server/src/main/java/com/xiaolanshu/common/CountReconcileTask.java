package com.xiaolanshu.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 计数对账任务（技术债 2 缓解）：
 * 定时重算 posts 的 like/collect/comment 计数与 topics 的 post_count，
 * 消除极端并发下事务内 update 的计数漂移。
 * 增强：对账前先找出计数与明细不一致的笔记，修正后**驱逐其 Redis 热点缓存**，
 * 避免 stale 计数在缓存 TTL 内继续对外服务。
 * - 每小时整点执行（可配置 xiaolanshu.reconcile.cron）
 * - view_count 无独立明细表（浏览时自增），不在对账范围
 */
@Component
public class CountReconcileTask {

    private static final Logger log = LoggerFactory.getLogger(CountReconcileTask.class);

    private final JdbcTemplate jdbc;
    private final PostCacheService postCache;

    public CountReconcileTask(JdbcTemplate jdbc, PostCacheService postCache) {
        this.jdbc = jdbc;
        this.postCache = postCache;
    }

    @Scheduled(cron = "${xiaolanshu.reconcile.cron:0 0 * * * *}")
    public void scheduledReconcile() {
        Map<String, Object> r = reconcileWithCache();
        long changed = ((Number) r.get("changedRows")).longValue();
        if (changed > 0) {
            log.info("[reconcile] 计数对账完成，修正 {} 条记录，驱逐 {} 条缓存", changed, r.get("evictedCount"));
        }
    }

    /** 重算计数 + 驱逐变更笔记缓存；返回 {changedRows, evictedCount} */
    public Map<String, Object> reconcileWithCache() {
        // 1) 找出计数与明细不一致的笔记（对账前快照）
        List<Long> dirtyIds = jdbc.queryForList("""
                SELECT p.id FROM posts p
                WHERE p.like_count    != (SELECT COUNT(*) FROM likes    WHERE post_id = p.id)
                   OR p.collect_count != (SELECT COUNT(*) FROM collects WHERE post_id = p.id)
                   OR p.comment_count != (SELECT COUNT(*) FROM comments WHERE post_id = p.id)
                """, Long.class);
        // 2) 按明细重算
        long changed = jdbc.update("""
                UPDATE posts p SET
                    like_count    = (SELECT COUNT(*) FROM likes    WHERE post_id = p.id),
                    collect_count = (SELECT COUNT(*) FROM collects WHERE post_id = p.id),
                    comment_count = (SELECT COUNT(*) FROM comments WHERE post_id = p.id)
                """);
        changed += jdbc.update("""
                UPDATE topics t SET
                    post_count = (SELECT COUNT(*) FROM posts p WHERE p.topic_id = t.id AND p.status = 'published')
                """);
        // 3) 驱逐不一致笔记的热点缓存（消除 stale 计数）
        int evicted = 0;
        for (Long id : dirtyIds) {
            postCache.evict(id);
            evicted++;
        }
        return Map.of("changedRows", changed, "evictedCount", evicted);
    }

    /** 兼容旧调用：仅返回受影响行数 */
    public long reconcile() {
        return ((Number) reconcileWithCache().get("changedRows")).longValue();
    }
}
