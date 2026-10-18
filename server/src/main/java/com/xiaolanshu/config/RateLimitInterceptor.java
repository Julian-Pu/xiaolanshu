package com.xiaolanshu.config;

import com.xiaolanshu.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** 简单固定窗口限流（按 IP，每分钟 N 次）。生产可替换为 Redis 分布式限流 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final long WINDOW_MS = 60_000L;

    private final int max;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitInterceptor(@Value("${xiaolanshu.rate-limit.max-per-minute}") int max) {
        this.max = max;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String ip = clientIp(request);
        long now = System.currentTimeMillis();
        Window w = windows.compute(ip, (key, old) ->
                old == null || now - old.startedAt() >= WINDOW_MS
                        ? new Window(new AtomicInteger(0), now)
                        : old);
        if (w.count().incrementAndGet() > max) {
            throw ApiException.tooManyRequests("请求过于频繁，请稍后再试");
        }
        return true;
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Window(AtomicInteger count, long startedAt) {
    }
}
