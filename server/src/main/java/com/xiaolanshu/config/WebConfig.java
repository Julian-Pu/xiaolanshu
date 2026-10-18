package com.xiaolanshu.config;

import com.xiaolanshu.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(AuthInterceptor authInterceptor, RateLimitInterceptor rateLimitInterceptor) {
        this.authInterceptor = authInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Value("${xiaolanshu.upload-dir}")
    private String uploadDir;

    @Value("${xiaolanshu.web-dist}")
    private String webDist;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 上传图片
        File uploadRoot = new File(uploadDir).getAbsoluteFile();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadRoot.toURI().toString());
        // 网页端构建产物（存在时由后端同端口托管，SpaFallbackFilter 兜底前端路由）
        File dist = new File(webDist).getAbsoluteFile();
        if (dist.isDirectory()) {
            registry.addResourceHandler("/**")
                    .addResourceLocations(dist.toURI().toString());
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
