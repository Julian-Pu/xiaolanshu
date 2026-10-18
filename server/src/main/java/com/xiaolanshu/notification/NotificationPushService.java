package com.xiaolanshu.notification;

import com.xiaolanshu.message.ChatWebSocketHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * 多端实时通知推送：通知落库后，向在线用户推送 WS notify 帧。
 * 离线用户由 REST 拉取兜底（通知列表/未读数接口）。
 * 生产可扩展为厂商推送通道（Android FCM/厂商通道、Web Push），此处复用站内 /ws 连接。
 */
@Service
public class NotificationPushService {

    private final ObjectProvider<ChatWebSocketHandler> wsProvider;
    private final ObjectMapper objectMapper;

    public NotificationPushService(ObjectProvider<ChatWebSocketHandler> wsProvider,
                                   ObjectMapper objectMapper) {
        this.wsProvider = wsProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * 推送一条新通知给在线用户；未在线忽略。
     * 载荷：{"type":"notify","notification":{"id","type","refId","content","createdAt","actorId"}}
     * 客户端收到后刷新通知列表/未读数（完整数据走 REST）。
     */
    public void push(long receiverId, long actorId, String type, long refId, String content, String createdAt, long id) {
        ChatWebSocketHandler handler = wsProvider.getIfAvailable();
        if (handler == null) {
            return;
        }
        try {
            Map<String, Object> notif = Map.of(
                    "id", id,
                    "actorId", actorId,
                    "type", type,
                    "refId", refId,
                    "content", content,
                    "createdAt", createdAt);
            handler.pushToUser(receiverId,
                    objectMapper.writeValueAsString(Map.of("type", "notify", "notification", notif)));
        } catch (Exception ignored) {
        }
    }
}
