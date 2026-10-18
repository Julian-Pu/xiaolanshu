package com.xiaolanshu.interest;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 用户兴趣画像（推荐个性化 v1）：
 * 从点赞 / 收藏 / 评论 / 发布等正向交互中学习笔记话题，形成 tag→score 兴趣分。
 * 推荐流按兴趣标签加权，实现"千人千面"的初版。
 */
@Service
public class InterestService {

    private final JdbcTemplate jdbc;

    public InterestService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 学习用户兴趣：对某篇笔记所属话题加分 */
    @Transactional
    public void learn(long userId, Long postId) {
        if (userId <= 0 || postId == null) {
            return;
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT t.name AS tag FROM posts p JOIN topics t ON t.id = p.topic_id WHERE p.id = ?",
                postId);
        for (Map<String, Object> row : rows) {
            String tag = (String) row.get("tag");
            if (tag == null || tag.isBlank()) {
                continue;
            }
            jdbc.update("""
                    INSERT INTO interests (user_id, tag, score) VALUES (?, ?, 1)
                    ON DUPLICATE KEY UPDATE score = score + 1
                    """, userId, tag);
        }
    }

    /** 用户兴趣 Top 标签（score 降序） */
    public List<String> topTags(long userId, int limit) {
        return jdbc.queryForList(
                        "SELECT tag FROM interests WHERE user_id = ? ORDER BY score DESC, tag ASC LIMIT ?",
                        userId, limit)
                .stream()
                .map(r -> (String) r.get("tag"))
                .toList();
    }
}
