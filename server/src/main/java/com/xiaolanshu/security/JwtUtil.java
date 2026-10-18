package com.xiaolanshu.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** JWT 签发与解析（HS256）。密钥支持环境变量注入（XIAOLANSHU_JWT_SECRET） */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private static final String DEFAULT_SECRET = "change-me-to-a-long-random-secret-key-at-least-32-bytes";

    private final SecretKey key;
    private final long expiresMillis;

    public JwtUtil(@Value("${xiaolanshu.jwt.secret}") String secret,
                   @Value("${xiaolanshu.jwt.expires-days}") long expiresDays) {
        int len = secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length;
        if (len < 32) {
            log.warn("[security] JWT 密钥长度 {} 字节（过短），生产环境必须通过环境变量 XIAOLANSHU_JWT_SECRET 注入 ≥32 字节高熵随机串！", len);
        } else if (DEFAULT_SECRET.equals(secret)) {
            log.warn("[security] JWT 密钥仍为仓库默认值，生产环境禁止使用！请用 `openssl rand -base64 48` 生成后通过 XIAOLANSHU_JWT_SECRET 注入。");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiresMillis = expiresDays * 24L * 3600_000L;
    }

    public String sign(long userId, String phone) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("phone", phone)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiresMillis))
                .signWith(key)
                .compact();
    }

    /** 解析 token 中的用户 id；无效或过期抛 JwtException */
    public long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }
}
