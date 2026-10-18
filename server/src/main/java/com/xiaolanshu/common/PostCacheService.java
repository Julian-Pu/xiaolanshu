package com.xiaolanshu.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 笔记热点缓存（Redis 可选，本地无 Redis 时自动降级为不缓存）：
 * - key 规则：cache:post:{id}，存 PostDetailView 的 JSON，TTL 默认 120 秒
 * - 仅缓存「未登录公共详情」（登录用户的 isLiked/isCollected 为个性化字段，不缓存）
 * - 写操作（编辑/删除）主动失效；互动计数变化最多滞后一个 TTL，由对账任务兜底
 */
@Service
public class PostCacheService {

    private static final Logger log = LoggerFactory.getLogger(PostCacheService.class);
    private static final String KEY_PREFIX = "cache:post:";

    private final StringRedisTemplate redis;
    private final boolean redisEnabled;
    private final Duration ttl;
    private volatile boolean warnedRedis = false;

    public PostCacheService(
            ObjectProvider<StringRedisTemplate> redisProvider,
            @Value("${xiaolanshu.cache.redis-enabled:true}") boolean redisEnabled,
            @Value("${xiaolanshu.cache.ttl-seconds:120}") long ttlSeconds) {
        this.redis = redisProvider.getIfAvailable();
        this.redisEnabled = redisEnabled;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public boolean available() {
        return redisEnabled && redis != null;
    }

    /** 命中缓存返回 JSON，未命中返回 null（Redis 异常时降级为 null，不阻断主流程） */
    public String get(long postId) {
        if (!available()) {
            return null;
        }
        try {
            return redis.opsForValue().get(KEY_PREFIX + postId);
        } catch (Exception e) {
            warnOnce(e);
            return null;
        }
    }

    public void put(long postId, String json) {
        if (!available() || json == null) {
            return;
        }
        try {
            redis.opsForValue().set(KEY_PREFIX + postId, json, ttl);
        } catch (Exception e) {
            warnOnce(e);
        }
    }

    public void evict(long postId) {
        if (!available()) {
            return;
        }
        try {
            redis.delete(KEY_PREFIX + postId);
        } catch (Exception e) {
            warnOnce(e);
        }
    }

    private void warnOnce(Exception e) {
        if (!warnedRedis) {
            warnedRedis = true;
            log.warn("Redis 缓存不可用，已降级为直查数据库: {}", e.getMessage());
        }
    }
}
