package com.xiaolanshu.feed;

import com.xiaolanshu.common.ApiResponse;
import com.xiaolanshu.common.CurrentUser;
import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import com.xiaolanshu.post.dto.PostCardView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public ApiResponse<PageResult<PostCardView>> feed(
            @RequestParam(defaultValue = "recommend") String tab,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        return ApiResponse.ok(feedService.feed(tab, topicId, CurrentUser.optional(request), PageParams.of(page, size)));
    }
}
