package com.xiaolanshu.feed;

import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import com.xiaolanshu.interest.InterestService;
import com.xiaolanshu.post.PostService;
import com.xiaolanshu.post.dto.PostCardView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FeedService {

    private final JdbcTemplate jdbc;
    private final PostService postService;
    private final InterestService interestService;
    private final FeedHotCacheService feedHotCache;

    public FeedService(JdbcTemplate jdbc, PostService postService, InterestService interestService,
                       FeedHotCacheService feedHotCache) {
        this.jdbc = jdbc;
        this.postService = postService;
        this.interestService = interestService;
        this.feedHotCache = feedHotCache;
    }

    /**
     * 信息流：
     * - following：关注者笔记，时间倒序
     * - topic：指定话题（频道）下笔记，时间倒序
     * - recommend：推荐流（个性化 v2 = 时间衰减热度 + 兴趣标签加权 + 协同过滤 + 排斥已读）
     *   score = (like*3 + collect*2 + comment*5 + view*0.2) / (发布小时数 + 2)^1.5
     *   ① 兴趣标签 boost：命中用户兴趣 Top 标签 ×2.0…×1.2（v1）
     *   ② 协同过滤 boost：与我点赞/收藏过相同笔记的"相似用户"还互动过的其他笔记，
     *      按相似用户数加权 ×1.0…×2.2（ItemCF 简化版，冷启动时无互动则不生效，回落到热度+兴趣）
     *   ③ 排斥已读：登录用户看过的笔记不再出现
     */
    public PageResult<PostCardView> feed(String tab, Long topicId, Long viewerId, PageParams pp) {
        if ("following".equals(tab)) {
            if (viewerId == null) {
                return PageResult.of(List.of(), 0, pp.page(), pp.size());
            }
            Long total = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM posts p
                    JOIN follows f ON f.follow_user_id = p.user_id AND f.user_id = ?
                    WHERE p.status = 'published'
                    """, Long.class, viewerId);
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT p.* FROM posts p
                    JOIN follows f ON f.follow_user_id = p.user_id AND f.user_id = ?
                    WHERE p.status = 'published'
                    ORDER BY p.created_at DESC LIMIT ? OFFSET ?
                    """, viewerId, pp.size(), pp.offset());
            return PageResult.of(postService.buildCards(rows, viewerId), total, pp.page(), pp.size());
        }

        if ("topic".equals(tab)) {
            if (topicId == null) {
                return PageResult.of(List.of(), 0, pp.page(), pp.size());
            }
            Long total = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM posts WHERE status = 'published' AND topic_id = ?", Long.class, topicId);
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT * FROM posts WHERE status = 'published' AND topic_id = ?
                    ORDER BY created_at DESC LIMIT ? OFFSET ?
                    """, topicId, pp.size(), pp.offset());
            return PageResult.of(postService.buildCards(rows, viewerId), total, pp.page(), pp.size());
        }

        // 推荐：先读离线物料池（预计算候选，按分直读）；池缺失/过期则实时计算兜底
        List<Map<String, Object>> pool = poolRows(viewerId, pp);
        if (!pool.isEmpty() && poolFresh(viewerId)) {
            Long total = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM recommend_candidates WHERE user_id = ?", Long.class, viewerId == null ? 0 : viewerId);
            return PageResult.of(postService.buildCards(pool, viewerId), total == null ? 0 : total, pp.page(), pp.size());
        }
        // 兜底1：Redis 热点榜（全局热度 TopN 预热，ZREVRANGE 分页 + SQL 取行，无全量排序）
        List<Map<String, Object>> hot = hotRows(viewerId, pp);
        if (hot != null) {
            long total = feedHotCache.hotCount();
            return PageResult.of(postService.buildCards(hot, viewerId), total, pp.page(), pp.size());
        }
        // 兜底2（最后防线：仅本地无 Redis 且物料池过期时触发）：实时全量排序
        List<Map<String, Object>> ranked = rankedRows(viewerId);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM posts WHERE status = 'published'", Long.class);
        int from = Math.min(pp.offset(), ranked.size());
        int to = Math.min(from + pp.size(), ranked.size());
        return PageResult.of(postService.buildCards(ranked.subList(from, to), viewerId), total, pp.page(), pp.size());
    }

    /**
     * Redis 热点榜取行：热度 id 列表（ZREVRANGE 分页）→ SQL 取卡片行 → 按榜序重排。
     * 返回 null 表示热点榜不可用/未预热（调用方回落到全量排序最后防线）。
     */
    public List<Map<String, Object>> hotRows(Long viewerId, PageParams pp) {
        List<Long> ids = feedHotCache.hotIds(pp);
        if (ids.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.*, t.name AS topic_name FROM posts p
                LEFT JOIN topics t ON t.id = p.topic_id
                WHERE p.id IN (%s) AND p.status = 'published'
                """.formatted(placeholders(ids.size())), ids.toArray());
        Map<Long, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> r : rows) {
            byId.put(((Number) r.get("id")).longValue(), r);
        }
        List<Map<String, Object>> ordered = new ArrayList<>();
        for (Long id : ids) {
            Map<String, Object> r = byId.get(id);
            if (r != null) {
                ordered.add(r);
            }
        }
        return ordered;
    }

    /** 从离线物料池取当前用户的候选（JOIN posts 保留卡片所需字段） */
    public List<Map<String, Object>> poolRows(Long viewerId, PageParams pp) {        if (viewerId == null) {
            return List.of();
        }
        return jdbc.queryForList("""
                SELECT p.*, t.name AS topic_name FROM recommend_candidates rc
                JOIN posts p ON p.id = rc.post_id
                LEFT JOIN topics t ON t.id = p.topic_id
                WHERE rc.user_id = ?
                ORDER BY rc.score DESC LIMIT ? OFFSET ?
                """, viewerId, pp.size(), pp.offset());
    }

    /** 物料池是否新鲜（最近 6 小时内重建过） */
    public boolean poolFresh(Long viewerId) {
        if (viewerId == null) {
            return false;
        }
        List<String> ts = jdbc.queryForList(
                "SELECT MAX(built_at) FROM recommend_candidates WHERE user_id = ?", String.class, viewerId);
        if (ts.isEmpty() || ts.get(0) == null) {
            return false;
        }
        try {
            long built = Instant.parse(ts.get(0)).toEpochMilli();
            return System.currentTimeMillis() - built < 6 * 3_600_000L;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 实时召回 + 排序（物料池生成与 feed 兜底共用同一逻辑）：
     * 全部已发布笔记 → 排除已读/自己 → 时间衰减热度 × 兴趣加成 × 协同加成 → 降序。
     * 返回已排序的 posts 行（含 topic_name 列）。
     */
    public List<Map<String, Object>> rankedRows(Long viewerId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.*, t.name AS topic_name FROM posts p
                LEFT JOIN topics t ON t.id = p.topic_id
                WHERE p.status = 'published'
                """);
        Map<String, Double> boosts = new HashMap<>();
        Map<Long, Integer> cfScores = new HashMap<>();
        Set<Long> excluded = new HashSet<>();   // 已读 + 自己发布的笔记
        if (viewerId != null) {
            List<Long> readIds = jdbc.queryForList(
                    "SELECT post_id FROM read_posts WHERE user_id = ?", Long.class, viewerId);
            excluded.addAll(readIds);
            List<Long> mine = jdbc.queryForList(
                    "SELECT id FROM posts WHERE user_id = ?", Long.class, viewerId);
            excluded.addAll(mine);
            rows.removeIf(r -> excluded.contains(((Number) r.get("id")).longValue()));
            List<String> tags = interestService.topTags(viewerId, 5);
            for (int i = 0; i < tags.size(); i++) {
                boosts.put(tags.get(i), 2.0 - i * 0.2);
            }
            cfScores.putAll(itemCf(viewerId, excluded));
        }
        long now = System.currentTimeMillis();
        rows.sort((a, b) -> Double.compare(
                score(b, now) * boost(b, boosts) * cfBoost(b, cfScores),
                score(a, now) * boost(a, boosts) * cfBoost(a, cfScores)));
        return rows;
    }

    /**
     * 离线物料池产物：每个候选笔记的最终分与主导因素（供定时任务写 recommend_candidates）。
     * 与实时推荐 rankedRows 使用完全相同的召回 + 排序逻辑。
     */
    public record RankedItem(long postId, double score, String reason) {
    }

    public List<RankedItem> rankedItems(Long viewerId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.*, t.name AS topic_name FROM posts p
                LEFT JOIN topics t ON t.id = p.topic_id
                WHERE p.status = 'published'
                """);
        Map<String, Double> boosts = new HashMap<>();
        Map<Long, Integer> cfScores = new HashMap<>();
        Set<Long> excluded = new HashSet<>();
        if (viewerId != null) {
            List<Long> readIds = jdbc.queryForList(
                    "SELECT post_id FROM read_posts WHERE user_id = ?", Long.class, viewerId);
            excluded.addAll(readIds);
            List<Long> mine = jdbc.queryForList(
                    "SELECT id FROM posts WHERE user_id = ?", Long.class, viewerId);
            excluded.addAll(mine);
            rows.removeIf(r -> excluded.contains(((Number) r.get("id")).longValue()));
            List<String> tags = interestService.topTags(viewerId, 5);
            for (int i = 0; i < tags.size(); i++) {
                boosts.put(tags.get(i), 2.0 - i * 0.2);
            }
            cfScores.putAll(itemCf(viewerId, excluded));
        }
        long now = System.currentTimeMillis();
        rows.sort((a, b) -> Double.compare(
                score(b, now) * boost(b, boosts) * cfBoost(b, cfScores),
                score(a, now) * boost(a, boosts) * cfBoost(a, cfScores)));
        List<RankedItem> items = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            double s = score(row, now) * boost(row, boosts) * cfBoost(row, cfScores);
            items.add(new RankedItem(((Number) row.get("id")).longValue(), s,
                    dominantReason(row, boosts, cfScores)));
        }
        return items;
    }

    private String dominantReason(Map<String, Object> row, Map<String, Double> boosts,
                                  Map<Long, Integer> cfScores) {
        if (cfBoost(row, cfScores) > 1.0) {
            return "cf";
        }
        if (boost(row, boosts) > 1.0) {
            return "interest";
        }
        return "hot";
    }

    /**
     * 协同过滤（ItemCF 简化版）：
     * 1) 取我最近互动的笔记（点赞/收藏并集，最多 20 篇）
     * 2) 找"相似用户"：与我互动过相同笔记的其他用户
     * 3) 相似用户互动过的其他笔记按人数计数 → postId → 相似用户数
     * 冷启动：我没有任何点赞/收藏时返回空 Map，推荐回落到"热度 + 兴趣标签"。
     */
    private Map<Long, Integer> itemCf(long viewerId, Set<Long> excluded) {
        List<Long> myIds = jdbc.queryForList("""
                SELECT post_id FROM likes WHERE user_id = ?
                UNION SELECT post_id FROM collects WHERE user_id = ?
                ORDER BY post_id DESC LIMIT 20
                """, Long.class, viewerId, viewerId);
        if (myIds.isEmpty()) {
            return Map.of();
        }
        List<Long> similar = jdbc.queryForList("""
                SELECT DISTINCT u.user_id FROM (
                    SELECT user_id FROM likes WHERE post_id IN (%s)
                    UNION SELECT user_id FROM collects WHERE post_id IN (%s)
                ) u WHERE u.user_id <> ?
                """.formatted(placeholders(myIds.size()), placeholders(myIds.size())),
                Long.class, joinParams(myIds, myIds, List.of(viewerId)));
        if (similar.isEmpty()) {
            return Map.of();
        }
        List<Map<String, Object>> cand = jdbc.queryForList("""
                SELECT post_id, COUNT(DISTINCT user_id) AS sim_cnt FROM (
                    SELECT user_id, post_id FROM likes WHERE user_id IN (%s)
                    UNION SELECT user_id, post_id FROM collects WHERE user_id IN (%s)
                ) c WHERE post_id NOT IN (%s)
                GROUP BY post_id
                """.formatted(placeholders(similar.size()), placeholders(similar.size()),
                        placeholders(myIds.size())),
                joinParams(similar, similar, myIds));
        Map<Long, Integer> scores = new HashMap<>();
        for (Map<String, Object> row : cand) {
            long postId = ((Number) row.get("post_id")).longValue();
            if (excluded.contains(postId)) {
                continue;
            }
            scores.put(postId, ((Number) row.get("sim_cnt")).intValue());
        }
        return scores;
    }

    private String placeholders(int n) {
        return String.join(",", java.util.Collections.nCopies(n, "?"));
    }

    @SafeVarargs
    private Object[] joinParams(List<Long>... groups) {
        List<Object> all = new ArrayList<>();
        for (List<Long> g : groups) {
            all.addAll(g);
        }
        return all.toArray();
    }

    /** 兴趣加成：命中用户兴趣标签则放大热度分，否则 ×1 */
    private double boost(Map<String, Object> row, Map<String, Double> boosts) {
        Object topicName = row.get("topic_name");
        if (topicName == null || boosts.isEmpty()) {
            return 1.0;
        }
        return boosts.getOrDefault(topicName.toString(), 1.0);
    }

    /** 协同加成：相似用户越多放大越大，1 人 ×1.4 → 3 人及以上封顶 ×2.2 */
    private double cfBoost(Map<String, Object> row, Map<Long, Integer> cfScores) {
        if (cfScores.isEmpty()) {
            return 1.0;
        }
        int sim = cfScores.getOrDefault(((Number) row.get("id")).longValue(), 0);
        if (sim <= 0) {
            return 1.0;
        }
        return 1.0 + Math.min(sim, 3) * 0.4;
    }

    /** 时间衰减热度分：互动量 / (发布小时数 + 2)^1.5 */
    private double score(Map<String, Object> row, long now) {
        double heat = num(row, "like_count") * 3
                + num(row, "collect_count") * 2
                + num(row, "comment_count") * 5
                + num(row, "view_count") * 0.2;
        long ageMs;
        try {
            ageMs = now - Instant.parse((String) row.get("created_at")).toEpochMilli();
        } catch (Exception e) {
            ageMs = 0;
        }
        double hours = ageMs / 3_600_000.0;
        return heat / Math.pow(hours + 2, 1.5);
    }

    private int num(Map<String, Object> row, String key) {
        Number n = (Number) row.get(key);
        return n == null ? 0 : n.intValue();
    }
}
