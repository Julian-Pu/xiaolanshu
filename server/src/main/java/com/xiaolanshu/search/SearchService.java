package com.xiaolanshu.search;

import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.post.PostService;
import com.xiaolanshu.post.dto.PostCardView;
import com.xiaolanshu.user.dto.UserView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final JdbcTemplate jdbc;
    private final PostService postService;

    public SearchService(JdbcTemplate jdbc, PostService postService) {
        this.jdbc = jdbc;
        this.postService = postService;
    }

    public record SearchResult(List<PostCardView> posts, List<UserView> users, List<String> snippets) {
    }

    /** type: post | user | all */
    public SearchResult search(String q, String type, PageParams pp, Long viewerId) {
        String like = "%" + q + "%";
        List<PostCardView> posts = new ArrayList<>();
        List<UserView> users = new ArrayList<>();
        List<String> snippets = new ArrayList<>();

        // 记录热搜词（仅非空、长度<=30 的搜索词；user 单查也记，供全站热搜）
        recordHotWord(q);

        // 拼音纠错：查询含拉丁字母时，匹配标题/昵称的全拼与首字母（演示规模内存索引；
        // 生产可替换为 ES/OpenSearch 的拼音分词器）
        PinyinHits hits = pinyinHits(q);

        if ("post".equals(type) || "all".equals(type)) {
            // 相关性排序：标题命中×3 + 正文命中×1 + 话题命中×2 + 热度因子，替代旧的纯热度排序
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT p.*,
                        (CASE WHEN p.title LIKE ? THEN 3 ELSE 0 END
                         + CASE WHEN p.content LIKE ? THEN 1 ELSE 0 END
                         + CASE WHEN t.name LIKE ? THEN 2 ELSE 0 END
                         + (p.view_count * 0.1 + p.like_count * 2 + p.collect_count * 1.5 + p.comment_count * 3)) AS score
                    FROM posts p LEFT JOIN topics t ON t.id = p.topic_id
                    WHERE p.status = 'published' AND (p.title LIKE ? OR p.content LIKE ? OR t.name LIKE ? OR p.id IN (%s))
                    ORDER BY score DESC LIMIT ? OFFSET ?
                    """.formatted(placeholders(hits.postIds.size())),
                    relArgs(like, like, like, hits.postIds, pp.size(), pp.offset()));
            for (Map<String, Object> row : rows) {
                snippets.add(snippetOf(row, q));
            }
            posts = postService.buildCards(rows, viewerId);
        }
        if ("user".equals(type) || "all".equals(type)) {
            List<Map<String, Object>> rows = jdbc.queryForList("""
                    SELECT * FROM users WHERE nickname LIKE ? OR id IN (%s)
                    ORDER BY follower_count DESC LIMIT ? OFFSET ?
                    """.formatted(placeholders(hits.userIds.size())),
                    args(like, hits.userIds, pp.size(), pp.offset()));            for (Map<String, Object> row : rows) {
                users.add(new UserView(
                        ((Number) row.get("id")).longValue(),
                        (String) row.get("nickname"),
                        (String) row.get("avatar"),
                        (String) row.get("bio"),
                        (String) row.get("gender"),
                        ((Number) row.get("following_count")).intValue(),
                        ((Number) row.get("follower_count")).intValue(),
                        ((Number) row.get("post_count")).intValue(),
                        (String) row.get("created_at"),
                        false));
            }
        }
        return new SearchResult(posts, users, snippets);
    }

    /** 高亮片段：正文/标题命中词前后各截取一段（纯文本，不做标签包裹）；拼音命中无片段返回 null */
    private static String snippetOf(Map<String, Object> row, String q) {
        String content = (String) row.get("content");
        String title = (String) row.get("title");
        String text = (content != null && !content.isBlank()) ? content : (title == null ? "" : title);
        int idx = text.indexOf(q);
        if (idx < 0) {
            idx = text.toLowerCase(java.util.Locale.ROOT).indexOf(q.toLowerCase(java.util.Locale.ROOT));
        }
        if (idx < 0) {
            // 正文未命中时回退标题（标题命中也展示片段）
            text = title == null ? "" : title;
            idx = text.indexOf(q);
            if (idx < 0) {
                idx = text.toLowerCase(java.util.Locale.ROOT).indexOf(q.toLowerCase(java.util.Locale.ROOT));
            }
        }
        if (idx < 0) {
            return null;
        }
        int start = Math.max(0, idx - 24);
        int end = Math.min(text.length(), idx + q.length() + 40);
        String seg = text.substring(start, end).trim();
        return (start > 0 ? "…" : "") + seg + (end < text.length() ? "…" : "");
    }

    /** 热搜词 Top 10（按次数倒序，同次数按最近更新倒序） */
    public List<Map<String, Object>> hot() {
        return jdbc.queryForList("""
                SELECT word, cnt FROM hot_words
                ORDER BY cnt DESC, updated_at DESC LIMIT 10
                """);
    }

    // ---- 拼音纠错 ----

    private record PinyinHits(List<Long> postIds, List<Long> userIds) {
        static PinyinHits empty() {
            return new PinyinHits(List.of(), List.of());
        }
    }

    /** 把查询词转小写拼音键：全拼串 + 首字母串；非纯字母时返回 null（无需拼音匹配） */
    private static String pinyinKey(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        boolean hasLetter = false;
        for (char c : q.toCharArray()) {
            if (Character.isLetter(c) && c < 128) {
                hasLetter = true;
                break;
            }
        }
        if (!hasLetter) {
            return null;
        }
        return q.toLowerCase(java.util.Locale.ROOT).replace(" ", "");
    }

    /** 内存索引：标题/昵称 → 全拼+首字母；演示规模直接构建，生产换 ES */
    private PinyinHits pinyinHits(String q) {
        String key = pinyinKey(q);
        if (key == null) {
            return PinyinHits.empty();
        }
        List<Long> postIds = new ArrayList<>();
        List<Long> userIds = new ArrayList<>();

        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT id, title FROM posts WHERE status = 'published' AND title IS NOT NULL")) {
            String title = (String) row.get("title");
            if (title.isBlank()) {
                continue;
            }
            String full = PinyinUtil.toPinyin(title, "").toLowerCase(java.util.Locale.ROOT);
            String initials = initialsOf(title);
            if (full.contains(key) || initials.contains(key)) {
                postIds.add(((Number) row.get("id")).longValue());
            }
        }
        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT id, nickname FROM users WHERE nickname IS NOT NULL")) {
            String nickname = (String) row.get("nickname");
            if (nickname.isBlank()) {
                continue;
            }
            String full = PinyinUtil.toPinyin(nickname, "").toLowerCase(java.util.Locale.ROOT);
            String initials = initialsOf(nickname);
            if (full.contains(key) || initials.contains(key)) {
                userIds.add(((Number) row.get("id")).longValue());
            }
        }
        return new PinyinHits(postIds, userIds);
    }

    private static String initialsOf(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(PinyinUtil.toPinyin(c).charAt(0));
        }
        return sb.toString().toLowerCase(java.util.Locale.ROOT);
    }

    private static String placeholders(int n) {
        if (n == 0) {
            return "0"; // IN (0) 恒不匹配，避免空占位符
        }
        return String.join(",", java.util.Collections.nCopies(n, "?"));
    }

    /** 动态拼 SQL 参数：相关性排序（title/content/topic 三个 LIKE + 三个 WHERE LIKE + 拼音 id + 分页） */
    private static Object[] relArgs(String tLike, String cLike, String topicLike, List<Long> ids, int size, int offset) {
        List<Object> all = new ArrayList<>();
        all.add(tLike);
        all.add(cLike);
        all.add(topicLike);
        all.add(tLike);
        all.add(cLike);
        all.add(topicLike);
        all.addAll(ids);
        all.add(size);
        all.add(offset);
        return all.toArray();
    }

    /** 动态拼 SQL 参数：LIKE 参数 ×2，拼音 id 列表，分页参数 */
    private static Object[] args(String like, String like2, List<Long> ids, int size, int offset) {
        List<Object> all = new ArrayList<>();
        all.add(like);
        all.add(like2);
        all.addAll(ids);
        all.add(size);
        all.add(offset);
        return all.toArray();
    }

    /** 动态拼 SQL 参数：单 LIKE 参数，拼音 id 列表，分页参数（用户搜索） */
    private static Object[] args(String like, List<Long> ids, int size, int offset) {
        List<Object> all = new ArrayList<>();
        all.add(like);
        all.addAll(ids);
        all.add(size);
        all.add(offset);
        return all.toArray();
    }

    private void recordHotWord(String q) {
        if (q == null || q.isBlank() || q.length() > 30) {
            return;
        }
        String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        int updated = jdbc.update(
                "UPDATE hot_words SET cnt = cnt + 1, updated_at = ? WHERE word = ?", now, q);
        if (updated == 0) {
            try {
                jdbc.update("INSERT INTO hot_words (word, cnt, updated_at) VALUES (?, 1, ?)", q, now);
            } catch (org.springframework.dao.DuplicateKeyException ignored) {
                // 并发首次写入冲突：再累加一次
                jdbc.update("UPDATE hot_words SET cnt = cnt + 1, updated_at = ? WHERE word = ?", now, q);
            }
        }
    }
}
