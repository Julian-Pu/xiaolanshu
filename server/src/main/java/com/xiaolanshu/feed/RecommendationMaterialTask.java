package com.xiaolanshu.feed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 推荐离线物料池任务：
 * 定期（每小时）为每个用户预计算 TopN 推荐候选（召回+排序产物）落 recommend_candidates 表，
 * feed 推荐流在线直读池子；演示用 Spring @Scheduled，生产替换为 Spark/Flink 离线任务。
 * 手动触发：POST /api/admin/rebuild-material
 */
@Component
public class RecommendationMaterialTask {

    private static final Logger log = LoggerFactory.getLogger(RecommendationMaterialTask.class);

    private final JdbcTemplate jdbc;
    private final FeedService feedService;
    private final int poolSize;

    public RecommendationMaterialTask(JdbcTemplate jdbc, FeedService feedService,
                                      @Value("${xiaolanshu.recommend.pool-size:50}") int poolSize) {
        this.jdbc = jdbc;
        this.feedService = feedService;
        this.poolSize = poolSize;
    }

    @Scheduled(cron = "0 0 * * * *") // 每小时整点重建
    public void scheduledRebuild() {
        try {
            Map<String, Object> r = rebuildAll();
            log.info("[recommend] 物料池定时重建完成: users={} candidates={}",
                    r.get("users"), r.get("candidates"));
        } catch (Exception e) {
            log.error("[recommend] 物料池定时重建失败", e);
        }
    }

    /** 全量重建：为所有用户生成候选池 */
    public Map<String, Object> rebuildAll() {
        List<Long> userIds = jdbc.queryForList("SELECT id FROM users", Long.class);
        int total = 0;
        for (Long uid : userIds) {
            total += rebuildFor(uid);
        }
        log.info("[recommend] 物料池全量重建: users={} candidates={}", userIds.size(), total);
        return Map.of("users", userIds.size(), "candidates", total);
    }

    private int rebuildFor(long userId) {
        List<FeedService.RankedItem> items = feedService.rankedItems(userId);
        jdbc.update("DELETE FROM recommend_candidates WHERE user_id = ?", userId);
        String builtAt = Instant.now().toString();
        int n = Math.min(items.size(), poolSize);
        for (int i = 0; i < n; i++) {
            FeedService.RankedItem it = items.get(i);
            jdbc.update("""
                    INSERT INTO recommend_candidates (user_id, post_id, score, reason, built_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, userId, it.postId(), it.score(), it.reason(), builtAt);
        }
        return n;
    }
}
