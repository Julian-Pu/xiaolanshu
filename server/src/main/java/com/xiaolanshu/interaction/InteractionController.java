package com.xiaolanshu.interaction;

import com.xiaolanshu.common.ApiResponse;
import com.xiaolanshu.common.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping("/{id}/like")
    public ApiResponse<InteractionService.LikeResult> like(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.ok(interactionService.like(id, CurrentUser.require(request)));
    }

    @DeleteMapping("/{id}/like")
    public ApiResponse<InteractionService.LikeResult> unlike(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.ok(interactionService.unlike(id, CurrentUser.require(request)));
    }

    @PostMapping("/{id}/collect")
    public ApiResponse<InteractionService.CollectResult> collect(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.ok(interactionService.collect(id, CurrentUser.require(request)));
    }

    @DeleteMapping("/{id}/collect")
    public ApiResponse<InteractionService.CollectResult> uncollect(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.ok(interactionService.uncollect(id, CurrentUser.require(request)));
    }
}
