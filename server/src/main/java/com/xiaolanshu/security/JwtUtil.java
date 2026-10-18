package com.xiaolanshu.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** JWT 签发与解析（HS256） */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiresMillis;

    public JwtUtil(@Value("${xiaolanshu.jwt.secret}") String secret,
                   @Value("${xiaolanshu.jwt.expires-days}") long expiresDays) {
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
