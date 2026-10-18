package com.xiaolanshu.config;

import com.xiaolanshu.common.CurrentUser;
import com.xiaolanshu.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 可选登录：解析 Bearer Token 注入 userId，游客放行（是否登录由各接口用 CurrentUser 判定） */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                request.setAttribute(CurrentUser.ATTR, jwtUtil.parseUserId(header.substring(7)));
            } catch (Exception ignored) {
                // 无效/过期 token 按游客处理
            }
        }
        return true;
    }
}
