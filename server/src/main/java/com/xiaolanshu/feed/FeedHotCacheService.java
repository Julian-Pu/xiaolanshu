package com.xiaolanshu.feed;

import com.xiaolanshu.common.PageParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 推荐热点榜预热（技术债 1 缓解：请求链路不再全量排序）：
 * 离线任务用与物料池一致的排序逻辑（无用户视角 = 纯时间衰减热度）算出全局 TopN，
 * 写入 Redis ZSET（key feed:hot:v1，member=postId，score=热度分），TTL 6 小时；
 * feed 推荐在「物料池缺失/过期」时先读热点榜（ZREVRANGE 分页 + SQL 取行），
 * 全量内存排序仅作为「无 Redis 且池过期」的最后防线。
 * 生产可扩展为 Spark/Flink 离线预热；此处演示用 Spring @Scheduled（每小时 20 分，与物料池错开）。
 */
@Component
public class FeedHotCacheService {

    private static final Logger log = LoggerFactory.getLogger(FeedHotCacheService.class);
    private static final String KEY = "feed:hot:v1";

    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectProvider<FeedService> feedProvider;
    private final boolean redisEnabled;
    private final int topN;
    private final Duration ttl;
    private volatile boolean warned = false;

    public FeedHotCacheService(
            ObjectProvider<StringRedisTemplate> redisProvider,
            ObjectProvider<FeedService> feedProvider,
            @Value("${xiaolanshu.cache.redis-enabled:true}") boolean redisEnabled,
            @Value("${xiaolanshu.feed.hot-top-n:200}") int topN,
            @Value("${xiaolanshu.feed.hot-ttl-seconds:21600}") long ttlSeconds) {
        this.redisProvider = redisProvider;
        this.feedProvider = feedProvider;
        this.redisEnabled = redisEnabled;
        this.topN = topN;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public boolean available() {
        return redisEnabled && redisProvider.getIfAvailable() != null;
    }

    private StringRedisTemplate redis() {
        return redisProvider.getIfAvailable();
    }

    /** 预热：全量排序只在离线任务内发生（低频），写入 Redis ZSET 供在线分页直读 */
    public Map<String, Object> warm() {
        if (!available()) {
            return Map.of("warmed", 0, "note", "redis-unavailable");
        }
        FeedService feed = feedProvider.getIfAvailable();
        if (feed == null) {
            return Map.of("warmed", 0, "note", "feed-service-unavailable");
        }
        try {
            List<FeedService.RankedItem> items = feed.rankedItems(null);
            StringRedisTemplate r = redis();
            r.delete(KEY);
            int n = Math.min(items.size(), topN);
            for (int i = 0; i < n; i++) {
                FeedService.RankedItem it = items.get(i);
                r.opsForZSet().add(KEY, String.valueOf(it.postId()), it.score());
            }
            if (n > 0) {
                r.expire(KEY, ttl);
            }
            log.info("[feed-hot] 热点榜预热完成: top={}", n);
            return Map.of("warmed", n, "size", n);
        } catch (Exception e) {
            warnOnce(e);
            return Map.of("warmed", 0, "note", "redis-error:" + e.getMessage());
        }
    }

    @Scheduled(cron = "${xiaolanshu.feed.hot-cron:0 20 * * * *}")
    public void scheduledWarm() {
        warm();
    }

    /** 按页取热点榜 postId（ZREVRANGE 分页，无全量排序） */
    public List<Long> hotIds(PageParams pp) {
        if (!available()) {
            return List.of();
        }
        try {
            Set<String> ids = redis().opsForZSet()
                    .reverseRange(KEY, pp.offset(), pp.offset() + pp.size() - 1);
            return ids.stream().map(Long::valueOf).toList();
        } catch (Exception e) {
            warnOnce(e);
            return List.of();
        }
    }

    /** 热点榜总条数（分页 total 用） */
    public long hotCount() {
        if (!available()) {
            return 0;
        }
        try {
            Long c = redis().opsForZSet().zCard(KEY);
            return c == null ? 0 : c;
        } catch (Exception e) {
            warnOnce(e);
            return 0;
        }
    }

    private void warnOnce(Exception e) {
        if (!warned) {
            warned = true;
            log.warn("Redis 热点榜不可用，已降级: {}", e.getMessage());
        }
    }
}
