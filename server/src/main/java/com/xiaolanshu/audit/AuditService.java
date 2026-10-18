package com.xiaolanshu.audit;

import com.xiaolanshu.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 内容安全审核：文本机审拦截 + 审核日志（供人工复核） */
@Service
public class AuditService {

    private final JdbcTemplate jdbc;
    private final SensitiveWordFilter filter;
    private final TransactionTemplate tx;

    public AuditService(JdbcTemplate jdbc, SensitiveWordFilter filter,
                        PlatformTransactionManager txManager) {
        this.jdbc = jdbc;
        this.filter = filter;
        this.tx = new TransactionTemplate(txManager);
        // REQUIRES_NEW：日志独立提交，绝不跟随外层业务事务回滚
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 文本机审入口：命中敏感词时记录日志并抛出 400（发布/编辑/评论统一走此方法）。
     * 审核日志在独立事务中先提交，随后抛出的业务异常只回滚外层业务事务，
     * 留痕日志保留供人工复核。
     */
    public void check(long userId, String action, String text) {
        List<String> hits = filter.findHits(text);
        if (hits.isEmpty()) {
            return;
        }
        String hit = hits.get(0);
        String snippet = text == null ? "" : text.substring(0, Math.min(text.length(), 200));
        tx.executeWithoutResult(status ->
                jdbc.update("""
                        INSERT INTO audit_logs (user_id, action, hit_word, content_snippet, created_at)
                        VALUES (?, ?, ?, ?, ?)
                        """, userId, action, hit, snippet, Instant.now().toString()));
        throw ApiException.badRequest("内容包含敏感词「" + hit + "」，请修改后重试");
    }

    /** 最近 50 条审核日志（管理接口） */
    public List<Map<String, Object>> recentLogs() {
        return jdbc.queryForList("""
                SELECT l.*, u.nickname AS user_nickname
                FROM audit_logs l LEFT JOIN users u ON u.id = l.user_id
                ORDER BY l.created_at DESC LIMIT 50
                """);
    }
}
