package com.xiaolanshu.auth;

import com.xiaolanshu.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短信验证码服务（开发模式：未接入运营商短信通道）。
 * <p>
 * 存储策略：
 * 1. 优先写 Redis（key: sms:code:{phone}，TTL 默认 5 分钟）——满足生产形态；
 * 2. Redis 连接失败（本地未装 Redis）时自动降级进程内存存储，并打印一次告警。
 * <p>
 * 验证码始终打印到后端控制台日志（模拟短信下发）；debug=true 时还会随接口响应返回，
 * 便于开发联调与自动化测试。生产环境请置 debug=false 并接入真实短信网关。
 */
@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);
    private static final String KEY_PREFIX = "sms:code:";
    private static final long RESEND_INTERVAL_MS = 60_000; // 同一手机号 60 秒内不可重复发送
    private static final int MAX_FAIL_ATTEMPTS = 5;         // 连续验证失败上限（防爆破）
    private static final long FAIL_LOCK_MS = 5 * 60_000L;   // 超过上限锁定 5 分钟

    private final StringRedisTemplate redis;
    private final boolean redisEnabled;
    private final boolean debug;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    /** Redis 不可用时的内存兜底：phoneKey -> (验证码, 过期时间戳) */
    private final ConcurrentHashMap<String, Entry> memory = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastSentAt = new ConcurrentHashMap<>();
    /** 防爆破：phone -> 连续失败次数；锁定：phone -> 锁定期限时间戳 */
    private final ConcurrentHashMap<String, Integer> failCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lockUntil = new ConcurrentHashMap<>();
    private volatile boolean warnedRedis = false;

    private record Entry(String code, long expireAt) {
    }

    public SmsCodeService(
            ObjectProvider<StringRedisTemplate> redisProvider,
            @Value("${xiaolanshu.sms.redis-enabled:true}") boolean redisEnabled,
            @Value("${xiaolanshu.sms.debug:true}") boolean debug,
            @Value("${xiaolanshu.sms.ttl-minutes:5}") long ttlMinutes) {
        this.redis = redisProvider.getIfAvailable();
        this.redisEnabled = redisEnabled;
        this.debug = debug;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    /**
     * 发送验证码：生成 6 位随机码 → 存储 → 打印控制台日志 → 返回。
     * 防爆破：手机号被锁定（连续失败超限）期间拒绝发送。
     *
     * @return debug 模式返回明文验证码（便于联调），生产模式返回 null
     */
    public String send(String phone) {
        long now = System.currentTimeMillis();
        Long lock = lockUntil.get(phone);
        if (lock != null && now < lock) {
            long left = (lock - now) / 1000;
            throw ApiException.tooManyRequests("验证尝试失败过多，请 " + (left / 60 + 1) + " 分钟后再试");
        }
        Long last = lastSentAt.get(phone);
        if (last != null && now - last < RESEND_INTERVAL_MS) {
            throw ApiException.tooManyRequests("发送过于频繁，请 60 秒后再试");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        store(phone, code);
        lastSentAt.put(phone, now);

        // 模拟运营商短信下发：验证码打印到后端控制台日志，便于在 App 上输入
        log.info("========== 【小蓝书】短信验证码 ==========");
        log.info("手机号：{}    验证码：{}    {} 分钟内有效", phone, code, ttl.toMinutes());
        log.info("当前为开发模式，未接入真实短信通道；请勿向他人泄露验证码。");
        log.info("==========================================");
        return debug ? code : null;
    }

    /** 校验验证码：匹配则立即作废（一次性）；失败累计，连续 MAX_FAIL_ATTEMPTS 次后锁定 5 分钟（防爆破） */
    public boolean verify(String phone, String code) {
        if (phone == null || code == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long lock = lockUntil.get(phone);
        if (lock != null && now < lock) {
            return false; // 锁定中直接拒绝
        }
        String key = KEY_PREFIX + phone;
        String stored = read(key);
        if (stored != null && stored.equals(code)) {
            remove(key);
            failCount.remove(phone);
            lockUntil.remove(phone);
            return true;
        }
        // 失败计数 + 锁定
        int fails = failCount.merge(phone, 1, Integer::sum);
        if (fails >= MAX_FAIL_ATTEMPTS) {
            lockUntil.put(phone, now + FAIL_LOCK_MS);
            failCount.remove(phone);
            log.warn("[security] 手机号 {} 验证码连续失败 {} 次，已锁定 5 分钟（防爆破）", phone, MAX_FAIL_ATTEMPTS);
        }
        return false;
    }

    private void store(String phone, String code) {
        String key = KEY_PREFIX + phone;
        if (redisEnabled && redis != null) {
            try {
                redis.opsForValue().set(key, code, ttl);
                return;
            } catch (Exception e) {
                warnRedis(e);
            }
        }
        memory.put(key, new Entry(code, System.currentTimeMillis() + ttl.toMillis()));
    }

    private String read(String key) {
        if (redisEnabled && redis != null) {
            try {
                String v = redis.opsForValue().get(key);
                if (v != null) {
                    return v;
                }
            } catch (Exception e) {
                warnRedis(e);
            }
        }
        Entry entry = memory.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expireAt()) {
            memory.remove(key);
            return null;
        }
        return entry.code();
    }

    private void remove(String key) {
        if (redisEnabled && redis != null) {
            try {
                redis.delete(key);
            } catch (Exception ignored) {
                // 内存兜底删除
            }
        }
        memory.remove(key);
    }

    private void warnRedis(Exception e) {
        if (!warnedRedis) {
            warnedRedis = true;
            log.warn("Redis 不可用（{}），短信验证码已降级为进程内存存储（重启后失效）。如需 Redis 持久存储，请启动 Redis 服务。", e.getMessage());
        }
    }
}
