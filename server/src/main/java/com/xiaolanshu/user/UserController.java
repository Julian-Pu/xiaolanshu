package com.xiaolanshu.user;

import com.xiaolanshu.common.ApiResponse;
import com.xiaolanshu.common.CurrentUser;
import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import com.xiaolanshu.post.PostService;
import com.xiaolanshu.post.dto.PostCardView;
import com.xiaolanshu.user.dto.UpdateProfileRequest;
import com.xiaolanshu.user.dto.UserView;
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

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PostService postService;

    public UserController(UserService userService, PostService postService) {
        this.userService = userService;
        this.postService = postService;
    }

    @GetMapping("/{id}")
    public ApiResponse<UserView> profile(@PathVariable long id, HttpServletRequest request) {
        return ApiResponse.ok(userService.profile(id, CurrentUser.optional(request)));
    }

    @PutMapping("/me")
    public ApiResponse<UserView> updateMe(HttpServletRequest request, @Valid @RequestBody UpdateProfileRequest req) {
        return ApiResponse.ok(userService.updateMe(CurrentUser.require(request), req));
    }

    /** 修改密码（设置页） */
    @PostMapping("/me/password")
    public ApiResponse<Void> changePassword(HttpServletRequest request, @Valid @RequestBody com.xiaolanshu.user.dto.ChangePasswordRequest req) {
        userService.changePassword(CurrentUser.require(request), req);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/follow")
    public ApiResponse<Void> follow(@PathVariable long id, HttpServletRequest request) {
        userService.follow(id, CurrentUser.require(request));
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}/follow")
    public ApiResponse<Void> unfollow(@PathVariable long id, HttpServletRequest request) {
        userService.unfollow(id, CurrentUser.require(request));
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/following")
    public ApiResponse<PageResult<UserView>> following(
            @PathVariable long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(userService.followingList(id, PageParams.of(page, size)));
    }

    @GetMapping("/{id}/followers")
    public ApiResponse<PageResult<UserView>> followers(
            @PathVariable long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(userService.followersList(id, PageParams.of(page, size)));
    }

    @GetMapping("/{id}/posts")
    public ApiResponse<PageResult<PostCardView>> posts(
            @PathVariable long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        return ApiResponse.ok(postService.userPosts(id, PageParams.of(page, size), CurrentUser.optional(request)));
    }

    /** 我的收藏列表（个人主页"收藏" tab） */
    @GetMapping("/me/collects")
    public ApiResponse<PageResult<PostCardView>> myCollects(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        return ApiResponse.ok(postService.collects(CurrentUser.require(request), PageParams.of(page, size)));
    }

    /** 我的赞过列表（个人主页"赞过" tab） */
    @GetMapping("/me/likes")
    public ApiResponse<PageResult<PostCardView>> myLikes(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        return ApiResponse.ok(postService.likes(CurrentUser.require(request), PageParams.of(page, size)));
    }

    /** 我的浏览历史（个人主页"浏览记录"入口） */
    @GetMapping("/me/history")
    public ApiResponse<PageResult<PostCardView>> myHistory(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        return ApiResponse.ok(postService.history(CurrentUser.require(request), PageParams.of(page, size)));
    }
}
