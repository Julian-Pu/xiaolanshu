package com.xiaolanshu.notification;

import com.xiaolanshu.common.ApiResponse;
import com.xiaolanshu.common.CurrentUser;
import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResult<NotificationService.NotificationView>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        return ApiResponse.ok(notificationService.list(CurrentUser.require(request), type, PageParams.of(page, size)));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> unreadCount(
            @RequestParam(required = false) String type,
            HttpServletRequest request) {
        return ApiResponse.ok(notificationService.unreadCount(CurrentUser.require(request), type));
    }

    @PostMapping("/read-all")
    public ApiResponse<Integer> readAll(HttpServletRequest request) {
        return ApiResponse.ok(notificationService.readAll(CurrentUser.require(request)));
    }
}
