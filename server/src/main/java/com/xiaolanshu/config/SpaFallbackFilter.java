package com.xiaolanshu.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** SPA 回退：非 /api、/uploads 且无扩展名的路径，若静态文件不存在则转发到 index.html（支持前端路由刷新） */
@Component
@Order(1)
public class SpaFallbackFilter extends OncePerRequestFilter {

    private final Path distRoot;

    public SpaFallbackFilter(@Value("${xiaolanshu.web-dist}") String webDist) {
        this.distRoot = new File(webDist).getAbsoluteFile().toPath();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (distRoot == null || !Files.exists(distRoot.resolve("index.html"))) {
            // 网页端未构建：仅提供 API
            chain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        boolean api = uri.startsWith("/api") || uri.startsWith("/uploads") || uri.startsWith("/h2-console")
                || uri.startsWith("/ws"); // /ws 为 WebSocket 握手端点，不得被 SPA 回退吞掉
        boolean hasExt = uri.substring(uri.lastIndexOf('/') + 1).contains(".");
        if (!api && !hasExt) {
            Path target = distRoot.resolve(uri.substring(1));
            if (!Files.exists(target) || Files.isDirectory(target)) {
                request.getRequestDispatcher("/index.html").forward(request, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
