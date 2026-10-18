package com.xiaolanshu.comment;

import com.xiaolanshu.comment.dto.CommentView;
import com.xiaolanshu.common.ApiResponse;
import com.xiaolanshu.common.CurrentUser;
import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    public record AddCommentRequest(
            @NotBlank(message = "评论内容不能为空")
            @Size(max = 1000, message = "评论最长 1000 字") String content,
            Long parentId
    ) {
    }

    @GetMapping("/api/posts/{postId}/comments")
    public ApiResponse<PageResult<CommentView>> list(
            @PathVariable long postId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(commentService.list(postId, PageParams.of(page, size)));
    }

    @PostMapping("/api/posts/{postId}/comments")
    public ApiResponse<CommentView> add(
            @PathVariable long postId,
            HttpServletRequest request,
            @Valid @RequestBody AddCommentRequest req) {
        return ApiResponse.ok(commentService.add(postId, CurrentUser.require(request), req.content(), req.parentId()));
    }

    @DeleteMapping("/api/comments/{id}")
    public ApiResponse<Void> delete(@PathVariable long id, HttpServletRequest request) {
        commentService.delete(id, CurrentUser.require(request));
        return ApiResponse.ok(null);
    }
}
