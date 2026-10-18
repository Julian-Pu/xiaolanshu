package com.xiaolanshu.topic;

import com.xiaolanshu.common.ApiException;
import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import com.xiaolanshu.post.PostService;
import com.xiaolanshu.post.dto.PostCardView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TopicService {

    private final JdbcTemplate jdbc;
    private final PostService postService;

    public TopicService(JdbcTemplate jdbc, PostService postService) {
        this.jdbc = jdbc;
        this.postService = postService;
    }

    public record TopicView(Long id, String name, String cover, int postCount) {
    }

    public List<TopicView> list() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM topics ORDER BY post_count DESC, id ASC");
        List<TopicView> views = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            views.add(new TopicView(
                    ((Number) row.get("id")).longValue(),
                    (String) row.get("name"),
                    (String) row.get("cover"),
                    ((Number) row.get("post_count")).intValue()));
        }
        return views;
    }

    public PageResult<PostCardView> topicPosts(long topicId, PageParams pp, Long viewerId) {
        Long exists = jdbc.queryForObject("SELECT COUNT(*) FROM topics WHERE id = ?", Long.class, topicId);
        if (exists == null || exists == 0) {
            throw ApiException.notFound("话题不存在");
        }
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM posts WHERE topic_id = ? AND status = 'published'", Long.class, topicId);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT * FROM posts WHERE topic_id = ? AND status = 'published'
                ORDER BY created_at DESC LIMIT ? OFFSET ?
                """, topicId, pp.size(), pp.offset());
        return PageResult.of(postService.buildCards(rows, viewerId), total, pp.page(), pp.size());
    }
}
