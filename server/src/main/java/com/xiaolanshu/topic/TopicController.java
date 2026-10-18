package com.xiaolanshu.topic;

import com.xiaolanshu.common.ApiResponse;
import com.xiaolanshu.common.CurrentUser;
import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import com.xiaolanshu.post.dto.PostCardView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public ApiResponse<List<TopicService.TopicView>> list() {
        return ApiResponse.ok(topicService.list());
    }

    @GetMapping("/{id}/posts")
    public ApiResponse<PageResult<PostCardView>> posts(
            @PathVariable long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        return ApiResponse.ok(topicService.topicPosts(id, PageParams.of(page, size), CurrentUser.optional(request)));
    }
}
