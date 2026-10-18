package com.xiaolanshu.message;

import com.xiaolanshu.common.ApiResponse;
import com.xiaolanshu.common.CurrentUser;
import com.xiaolanshu.common.PageParams;
import com.xiaolanshu.common.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public record StartConversationRequest(
            @jakarta.validation.constraints.NotNull(message = "userId 不能为空") Long userId
    ) {
    }

    public record SendMessageRequest(
            @NotBlank(message = "消息内容不能为空")
            @Size(max = 500, message = "消息最长 500 字") String content
    ) {
    }

    @GetMapping
    public ApiResponse<List<MessageService.ConversationView>> conversations(HttpServletRequest request) {
        return ApiResponse.ok(messageService.conversations(CurrentUser.require(request)));
    }

    @PostMapping
    public ApiResponse<MessageService.ConversationView> start(
            HttpServletRequest request, @Valid @RequestBody StartConversationRequest req) {
        return ApiResponse.ok(messageService.getOrCreate(CurrentUser.require(request), req.userId()));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<PageResult<MessageService.MessageView>> messages(
            @PathVariable long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request) {
        return ApiResponse.ok(messageService.messages(id, CurrentUser.require(request), PageParams.of(page, size)));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<MessageService.MessageView> send(
            @PathVariable long id,
            HttpServletRequest request,
            @Valid @RequestBody SendMessageRequest req) {
        return ApiResponse.ok(messageService.send(id, CurrentUser.require(request), req.content()));
    }

    /** 标记会话中对方消息为已读（WebSocket read 帧的 REST 兜底） */
    @PostMapping("/{id}/read")
    public ApiResponse<java.util.Map<String, Object>> markRead(
            @PathVariable long id,
            HttpServletRequest request) {
        long changed = messageService.markRead(id, CurrentUser.require(request));
        return ApiResponse.ok(java.util.Map.of("changed", changed));
    }
}
