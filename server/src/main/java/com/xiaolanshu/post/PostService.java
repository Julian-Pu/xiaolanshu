package com.xiaolanshu.post;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.xiaolanshu.common.ApiException;
import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import com.xiaolanshu.post.dto.CreatePostRequest;
import com.xiaolanshu.post.dto.PostCardView;
import com.xiaolanshu.post.dto.PostDetailView;
import com.xiaolanshu.topic.dto.TopicBrief;
import com.xiaolanshu.user.dto.UserBrief;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PostService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final com.xiaolanshu.interest.InterestService interestService;
    private final com.xiaolanshu.common.PostCacheService postCache;
    private final com.xiaolanshu.upload.ImageProcessor imageProcessor;
    private final com.xiaolanshu.audit.AuditService auditService;

    public PostService(JdbcTemplate jdbc, ObjectMapper objectMapper,
                       com.xiaolanshu.interest.InterestService interestService,
                       com.xiaolanshu.common.PostCacheService postCache,
                       com.xiaolanshu.upload.ImageProcessor imageProcessor,
                       com.xiaolanshu.audit.AuditService auditService) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.interestService = interestService;
        this.postCache = postCache;
        this.imageProcessor = imageProcessor;
        this.auditService = auditService;
    }

    @Transactional
    public PostDetailView create(long userId, CreatePostRequest req) {
        if (req.title() == null && req.content() == null) {
            throw ApiException.badRequest("标题和正文至少填写一项");
        }
        // 内容安全：发布前文本机审（标题 + 正文）
        auditService.check(userId, "publish", (req.title() == null ? "" : req.title())
                + (req.content() == null ? "" : req.content()));
        Long topicId = req.topicId();
        if (topicId != null && topicExists(topicId) == 0) {
            throw ApiException.notFound("话题不存在");
        }
        String now = Instant.now().toString();
        String images = toJson(req.images());
        jdbc.update("""
                INSERT INTO posts (user_id, title, content, images, video_url, video_cover, topic_id, status,
                                   like_count, collect_count, comment_count, view_count, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'published', 0, 0, 0, 0, ?, ?)
                """, userId, req.title(), req.content(), images, req.videoUrl(), req.videoCover(),
                topicId, now, now);
        jdbc.update("UPDATE users SET post_count = post_count + 1 WHERE id = ?", userId);
        if (topicId != null) {
            jdbc.update("UPDATE topics SET post_count = post_count + 1 WHERE id = ?", topicId);
        }
        long postId = jdbc.queryForObject(
                "SELECT id FROM posts WHERE user_id = ? AND created_at = ?", Long.class, userId, now);
        interestService.learn(userId, postId);
        return detail(postId, userId);
    }

    @Transactional
    public PostDetailView update(long id, long userId, CreatePostRequest req) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM posts WHERE id = ?", id);
        if (rows.isEmpty() || "deleted".equals(rows.get(0).get("status"))) {
            throw ApiException.notFound("笔记不存在");
        }
        Map<String, Object> post = rows.get(0);
        if (((Number) post.get("user_id")).longValue() != userId) {
            throw ApiException.forbidden("只能编辑自己的笔记");
        }
        Long topicId = req.topicId();
        if (topicId != null && topicExists(topicId) == 0) {
            throw ApiException.notFound("话题不存在");
        }
        // 媒体字段未传时保留原值（Web 编辑只改文本的场景）
        String images = req.images() != null ? toJson(req.images()) : (String) post.get("images");
        String videoUrl = req.videoUrl() != null ? req.videoUrl() : (String) post.get("video_url");
        String videoCover = req.videoCover() != null ? req.videoCover() : (String) post.get("video_cover");
        // 内容安全：编辑后文本机审（命中即拦截并保留原内容）
        auditService.check(userId, "edit", (req.title() == null ? "" : req.title())
                + (req.content() == null ? "" : req.content()));
        jdbc.update("""
                UPDATE posts SET title = ?, content = ?, images = ?, video_url = ?, video_cover = ?,
                                   topic_id = ?, updated_at = ?
                WHERE id = ?
                """, req.title(), req.content(), images, videoUrl, videoCover,
                topicId, Instant.now().toString(), id);
        postCache.evict(id);
        return detail(id, userId);
    }

    public PostDetailView detail(long id, Long viewerId) {
        // 未登录公共详情走 Redis 热点缓存（TTL 短），命中时仅自增浏览数后直接返回
        if (viewerId == null) {
            String cached = postCache.get(id);
            if (cached != null) {
                jdbc.update("UPDATE posts SET view_count = view_count + 1 WHERE id = ?", id);
                try {
                    return objectMapper.readValue(cached, PostDetailView.class);
                } catch (Exception ignored) {
                    // 缓存反序列化失败则忽略，走正常查库
                }
            }
            PostDetailView built = buildDetail(id, null);
            try {
                postCache.put(id, objectMapper.writeValueAsString(built));
            } catch (Exception ignored) {
            }
            return built;
        }
        return buildDetail(id, viewerId);
    }

    /** 组装详情（登录视图含 isLiked/isCollected，不缓存） */
    private PostDetailView buildDetail(long id, Long viewerId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM posts WHERE id = ?", id);
        if (rows.isEmpty() || "deleted".equals(rows.get(0).get("status"))) {
            throw ApiException.notFound("笔记不存在");
        }
        Map<String, Object> post = rows.get(0);
        jdbc.update("UPDATE posts SET view_count = view_count + 1 WHERE id = ?", id);
        // 记录浏览历史（登录用户；用于"我的浏览记录" + 推荐流排斥已读）
        if (viewerId != null) {
            jdbc.update("""
                    INSERT INTO read_posts (user_id, post_id, viewed_at) VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE viewed_at = VALUES(viewed_at)
                    """, viewerId, id, Instant.now().toString());
        }
        long authorId = ((Number) post.get("user_id")).longValue();

        Map<String, Object> authorRow = userRow(authorId);
        boolean authorFollowing = viewerId != null && isFollowing(viewerId, authorId);
        UserBrief author = new UserBrief(
                authorId,
                (String) authorRow.get("nickname"),
                (String) authorRow.get("avatar"),
                authorFollowing);

        TopicBrief topic = null;
        Number topicId = (Number) post.get("topic_id");
        if (topicId != null) {
            Map<String, Object> t = topicRow(topicId.longValue());
            if (t != null) {
                topic = new TopicBrief(((Number) t.get("id")).longValue(), (String) t.get("name"));
            }
        }

        boolean isLiked = viewerId != null && exists("likes", viewerId, id);
        boolean isCollected = viewerId != null && exists("collects", viewerId, id);

        return new PostDetailView(
                id,
                (String) post.get("title"),
                (String) post.get("content"),
                coverOf(post),
                parseImages(post),
                (String) post.get("video_url"),
                (String) post.get("video_cover"),
                num(post, "like_count"),
                num(post, "collect_count"),
                num(post, "comment_count"),
                num(post, "view_count"),
                (String) post.get("created_at"),
                author,
                topic,
                isLiked,
                isCollected);
    }

    @Transactional
    public void delete(long id, long userId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM posts WHERE id = ?", id);
        if (rows.isEmpty() || "deleted".equals(rows.get(0).get("status"))) {
            throw ApiException.notFound("笔记不存在");
        }
        Map<String, Object> post = rows.get(0);
        if (((Number) post.get("user_id")).longValue() != userId) {
            throw ApiException.forbidden("只能删除自己的笔记");
        }
        jdbc.update("UPDATE posts SET status = 'deleted', updated_at = ? WHERE id = ?", Instant.now().toString(), id);
        jdbc.update("UPDATE users SET post_count = GREATEST(post_count - 1, 0) WHERE id = ?", userId);
        Number topicId = (Number) post.get("topic_id");
        if (topicId != null) {
            jdbc.update("UPDATE topics SET post_count = GREATEST(post_count - 1, 0) WHERE id = ?", topicId.longValue());
        }
        postCache.evict(id);
    }

    public PageResult<PostCardView> userPosts(long userId, PageParams pp, Long viewerId) {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM posts WHERE user_id = ? AND status = 'published'", Long.class, userId);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT * FROM posts WHERE user_id = ? AND status = 'published'
                ORDER BY created_at DESC LIMIT ? OFFSET ?
                """, userId, pp.size(), pp.offset());
        return PageResult.of(buildCards(rows, viewerId), total, pp.page(), pp.size());
    }

    /**
     * 相关推荐（详情页"猜你喜欢"）：同话题优先 → 同作者 → 全站热度，排除自身。
     * 开发期直查库；数据量大后可改为离线计算相关关系（协同过滤 / 同话题池）预热缓存。
     */
    public List<PostCardView> related(long postId, Long viewerId, int limit) {
        List<Map<String, Object>> info = jdbc.queryForList(
                "SELECT topic_id, user_id FROM posts WHERE id = ? AND status = 'published'", postId);
        if (info.isEmpty()) {
            return List.of();
        }
        Map<String, Object> row0 = info.get(0);
        Number topicId = (Number) row0.get("topic_id");
        long authorId = ((Number) row0.get("user_id")).longValue();
        List<Map<String, Object>> rows;
        if (topicId != null) {
            rows = jdbc.queryForList("""
                    SELECT * FROM (
                        SELECT p.*, 1 AS rk FROM posts p WHERE p.status='published' AND p.topic_id=? AND p.id<>?
                        UNION ALL
                        SELECT p.*, 2 AS rk FROM posts p WHERE p.status='published' AND p.user_id=? AND p.id<>?
                        UNION ALL
                        SELECT p.*, 3 AS rk FROM posts p WHERE p.status='published' AND p.id<>?
                    ) t
                    ORDER BY t.rk, (t.like_count*3 + t.collect_count*2 + t.comment_count*5 + t.view_count*0.2) DESC
                    LIMIT ?
                    """, topicId.longValue(), postId, authorId, postId, postId, limit * 3);
        } else {
            rows = jdbc.queryForList("""
                    SELECT * FROM (
                        SELECT p.*, 2 AS rk FROM posts p WHERE p.status='published' AND p.user_id=? AND p.id<>?
                        UNION ALL
                        SELECT p.*, 3 AS rk FROM posts p WHERE p.status='published' AND p.id<>?
                    ) t
                    ORDER BY t.rk, (t.like_count*3 + t.collect_count*2 + t.comment_count*5 + t.view_count*0.2) DESC
                    LIMIT ?
                    """, authorId, postId, postId, limit * 3);
        }
        // 多取 3 倍再按 rk 优先级去重（同一笔记可能同时命中同话题/同作者/全站），保证结果无重复
        return buildCards(deduplicate(rows, limit), viewerId);
    }

    /** 按出现顺序（rk 升序）去重并截断，保留优先级最高的首次出现 */
    private List<Map<String, Object>> deduplicate(List<Map<String, Object>> rows, int limit) {
        java.util.LinkedHashSet<Long> seen = new java.util.LinkedHashSet<>();
        List<Map<String, Object>> dedup = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            if (seen.add(id)) {
                dedup.add(row);
                if (dedup.size() >= limit) {
                    break;
                }
            }
        }
        return dedup;
    }

    /** 我的收藏列表（按收藏时间倒序） */
    public PageResult<PostCardView> collects(long userId, PageParams pp) {
        return cardsByInteraction("collects", userId, pp);
    }

    /** 我的赞过列表（按点赞时间倒序） */
    public PageResult<PostCardView> likes(long userId, PageParams pp) {
        return cardsByInteraction("likes", userId, pp);
    }

    /** 我的浏览历史（按最近浏览倒序） */
    public PageResult<PostCardView> history(long userId, PageParams pp) {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM read_posts WHERE user_id = ?", Long.class, userId);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.* FROM read_posts rp JOIN posts p ON p.id = rp.post_id
                WHERE rp.user_id = ? AND p.status = 'published'
                ORDER BY rp.viewed_at DESC LIMIT ? OFFSET ?
                """, userId, pp.size(), pp.offset());
        return PageResult.of(buildCards(rows, userId), total, pp.page(), pp.size());
    }

    /** 按互动表（collects/likes，仅内部常量调用）组装分页卡片 */
    private PageResult<PostCardView> cardsByInteraction(String table, long userId, PageParams pp) {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " c JOIN posts p ON p.id = c.post_id"
                        + " WHERE c.user_id = ? AND p.status = 'published'", Long.class, userId);
        String sql = "SELECT p.* FROM " + table + " c JOIN posts p ON p.id = c.post_id"
                + " WHERE c.user_id = ? AND p.status = 'published'"
                + " ORDER BY c.created_at DESC LIMIT ? OFFSET ?";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, userId, pp.size(), pp.offset());
        return PageResult.of(buildCards(rows, userId), total, pp.page(), pp.size());
    }

    /** 批量组装卡片：作者 / 话题 / 登录态互动状态 / 关注状态 */
    public List<PostCardView> buildCards(List<Map<String, Object>> rows, Long viewerId) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<Long> authorIds = new HashSet<>();
        Set<Long> topicIds = new HashSet<>();
        for (Map<String, Object> row : rows) {
            authorIds.add(((Number) row.get("user_id")).longValue());
            Number topicId = (Number) row.get("topic_id");
            if (topicId != null) {
                topicIds.add(topicId.longValue());
            }
        }
        Map<Long, Map<String, Object>> users = batchUsers(authorIds);
        Map<Long, TopicBrief> topics = batchTopics(topicIds);

        Set<Long> liked = new HashSet<>();
        Set<Long> collected = new HashSet<>();
        Set<Long> following = new HashSet<>();
        if (viewerId != null) {
            liked = postIdsIn("likes", viewerId, rows);
            collected = postIdsIn("collects", viewerId, rows);
            following = followingIds(viewerId, authorIds);
        }

        List<PostCardView> cards = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            long postId = ((Number) row.get("id")).longValue();
            long authorId = ((Number) row.get("user_id")).longValue();
            Map<String, Object> authorRow = users.getOrDefault(authorId, Map.of());
            Number topicId = (Number) row.get("topic_id");
            cards.add(new PostCardView(
                    postId,
                    (String) row.get("title"),
                    coverOf(row),
                    imageProcessor.thumbnailOf(coverOf(row)),
                    parseImages(row),
                    (String) row.get("video_url"),
                    num(row, "like_count"),
                    num(row, "collect_count"),
                    num(row, "comment_count"),
                    num(row, "view_count"),
                    (String) row.get("created_at"),
                    new UserBrief(authorId,
                            str(authorRow, "nickname", "未知用户"),
                            (String) authorRow.get("avatar"),
                            following.contains(authorId)),
                    topicId != null ? topics.get(topicId.longValue()) : null,
                    liked.contains(postId),
                    collected.contains(postId)));
        }
        return cards;
    }

    // ---- helpers ----

    private String toJson(List<String> images) {
        try {
            return images == null ? "[]" : objectMapper.writeValueAsString(images);
        } catch (Exception e) {
            throw ApiException.badRequest("图片参数不合法");
        }
    }

    private List<String> parseImages(Map<String, Object> row) {
        String images = (String) row.get("images");
        if (images == null || images.isBlank()) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(images, new TypeReference<>() {
            });
            return list == null ? List.of() : list;
        } catch (Exception e) {
            return List.of();
        }
    }

    private String coverOf(Map<String, Object> row) {
        String videoCover = (String) row.get("video_cover");
        if (videoCover != null && !videoCover.isBlank()) {
            return videoCover;
        }
        List<String> images = parseImages(row);
        return images.isEmpty() ? null : images.get(0);
    }

    private int num(Map<String, Object> row, String key) {
        Number n = (Number) row.get(key);
        return n == null ? 0 : n.intValue();
    }

    private String str(Map<String, Object> row, String key, String def) {
        Object v = row.get(key);
        return v == null ? def : (String) v;
    }

    private boolean exists(String table, long userId, long postId) {
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE user_id = ? AND post_id = ?", Long.class, userId, postId);
        return c != null && c > 0;
    }

    private boolean isFollowing(long viewerId, long targetId) {
        Long c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM follows WHERE user_id = ? AND follow_user_id = ?", Long.class, viewerId, targetId);
        return c != null && c > 0;
    }

    private long topicExists(long id) {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM topics WHERE id = ?", Long.class, id);
        return c == null ? 0 : c;
    }

    /** 供其他服务校验笔记存在性 */
    public Map<String, Object> getPostRow(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM posts WHERE id = ?", id);
        if (rows.isEmpty()) {
            throw ApiException.notFound("笔记不存在");
        }
        return rows.get(0);
    }

    private Map<String, Object> userRow(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM users WHERE id = ?", id);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private Map<String, Object> topicRow(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, name FROM topics WHERE id = ?", id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Map<Long, Map<String, Object>> batchUsers(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM users WHERE id IN (" + placeholders + ")", ids.toArray());
        Map<Long, Map<String, Object>> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            map.put(((Number) row.get("id")).longValue(), row);
        }
        return map;
    }

    private Map<Long, TopicBrief> batchTopics(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, name FROM topics WHERE id IN (" + placeholders + ")", ids.toArray());
        Map<Long, TopicBrief> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            map.put(((Number) row.get("id")).longValue(),
                    new TopicBrief(((Number) row.get("id")).longValue(), (String) row.get("name")));
        }
        return map;
    }

    private Set<Long> postIdsIn(String table, long userId, List<Map<String, Object>> rows) {
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            ids.add(((Number) row.get("id")).longValue());
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT post_id FROM " + table + " WHERE user_id = ? AND post_id IN (" + placeholders + ")",
                concat(List.of(userId), ids).toArray());
        Set<Long> result = new HashSet<>();
        for (Map<String, Object> row : list) {
            result.add(((Number) row.get("post_id")).longValue());
        }
        return result;
    }

    private Set<Long> followingIds(long viewerId, Set<Long> targetIds) {
        if (targetIds.isEmpty()) {
            return Set.of();
        }
        String placeholders = String.join(",", Collections.nCopies(targetIds.size(), "?"));
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT follow_user_id FROM follows WHERE user_id = ? AND follow_user_id IN (" + placeholders + ")",
                concat(List.of(viewerId), new ArrayList<>(targetIds)).toArray());
        Set<Long> result = new HashSet<>();
        for (Map<String, Object> row : list) {
            result.add(((Number) row.get("follow_user_id")).longValue());
        }
        return result;
    }

    private static List<Object> concat(List<Long> a, List<Long> b) {
        List<Object> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }
}
