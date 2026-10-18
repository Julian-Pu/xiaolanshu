package com.xiaolanshu.common;

import jakarta.servlet.http.HttpServletRequest;

/** 从请求取当前登录用户（由 AuthInterceptor 注入 request attribute "userId"） */
public final class CurrentUser {

    public static final String ATTR = "userId";

    private CurrentUser() {
    }

    /** 必登录接口使用：未登录抛 401 */
    public static long require(HttpServletRequest req) {
        Long id = (Long) req.getAttribute(ATTR);
        if (id == null) {
            throw ApiException.unauthorized("请先登录");
        }
        return id;
    }

    /** 可选登录接口使用：游客返回 null */
    public static Long optional(HttpServletRequest req) {
        return (Long) req.getAttribute(ATTR);
    }
}
