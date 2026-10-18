package com.xiaolanshu.post;

import com.xiaolanshu.common.ApiResponse;
import com.xiaolanshu.common.CurrentUser;
import com.xiaolanshu.post.dto.CreatePostRequest;
import com.xiaolanshu.post.dto.PostCardView;
import com.xiaolanshu.post.dto.PostDetailView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/{id}")
    public ApiResponse<PostDetailView> detail(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.ok(postService.detail(id, CurrentUser.optional(request)));
    }

    /** 相关推荐（猜你喜欢） */
    @GetMapping("/{id}/related")
    public ApiResponse<List<PostCardView>> related(
            @PathVariable long id,
            @RequestParam(defaultValue = "6") int limit,
            HttpServletRequest request) {
        return ApiResponse.ok(postService.related(id, CurrentUser.optional(request), Math.min(Math.max(limit, 1), 20)));
    }

    @PostMapping
    public ApiResponse<PostDetailView> create(HttpServletRequest request, @Valid @RequestBody CreatePostRequest req) {
        return ApiResponse.ok(postService.create(CurrentUser.require(request), req));
    }

    /** 编辑笔记（仅作者） */
    @PutMapping("/{id}")
    public ApiResponse<PostDetailView> update(@PathVariable long id, HttpServletRequest request,
                                              @Valid @RequestBody CreatePostRequest req) {
        return ApiResponse.ok(postService.update(id, CurrentUser.require(request), req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id, HttpServletRequest request) {
        postService.delete(id, CurrentUser.require(request));
        return ApiResponse.ok(null);
    }
}
