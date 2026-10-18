package com.xiaolanshu.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 计数对账任务：定时重算 posts 的 like/collect/comment 计数与 topics 的 post_count，
 * 消除极端并发下的计数漂移（事务内 update 的兜底）。
 * - 每小时整点执行（生产可按需调整 cron）
 * - view_count 无独立明细表（浏览时自增），不在对账范围
 */
@Component
public class CountReconcileTask {

    private static final Logger log = LoggerFactory.getLogger(CountReconcileTask.class);

    private final JdbcTemplate jdbc;

    public CountReconcileTask(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(cron = "${xiaolanshu.reconcile.cron:0 0 * * * *}")
    public void scheduledReconcile() {
        long changed = reconcile();
        if (changed > 0) {
            log.info("[reconcile] 计数对账完成，修正 {} 条记录", changed);
        }
    }

    /** 重算计数，返回受影响行数（公开给管理接口手动触发/演示） */
    public long reconcile() {
        long changed = 0;
        changed += jdbc.update("""
                UPDATE posts p SET
                    like_count    = (SELECT COUNT(*) FROM likes    WHERE post_id = p.id),
                    collect_count = (SELECT COUNT(*) FROM collects WHERE post_id = p.id),
                    comment_count = (SELECT COUNT(*) FROM comments WHERE post_id = p.id)
                """);
        changed += jdbc.update("""
                UPDATE topics t SET
                    post_count = (SELECT COUNT(*) FROM posts p WHERE p.topic_id = t.id AND p.status = 'published')
                """);
        return changed;
    }
}
