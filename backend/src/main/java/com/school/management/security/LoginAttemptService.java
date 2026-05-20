package com.school.management.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录失败计数 / 账户锁定 — 防暴力破解 (S4, 2026-05-20 安全审计 P3).
 *
 * <p>每个用户名维护一个 Redis 失败计数 {@code login_fail:{username}}, 连续失败
 * 达到 {@link #MAX_ATTEMPTS} 次即锁定 {@link #LOCK_DURATION}; 登录成功清零.
 *
 * <p>Redis 不可用时降级为"不锁定" (可用性优先 — 登录本身仍校验密码, 不会因
 * 计数器故障而把所有人挡在门外).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    /** 连续失败达到该次数即锁定. */
    private static final int MAX_ATTEMPTS = 5;
    /** 锁定时长 (也是失败计数的滑动窗口). */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final RedisTemplate<String, Object> redisTemplate;

    private String key(String username) {
        return "login_fail:" + username;
    }

    /** 该用户名当前是否处于锁定状态. */
    public boolean isLocked(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        try {
            Object v = redisTemplate.opsForValue().get(key(username));
            return v != null && Integer.parseInt(v.toString()) >= MAX_ATTEMPTS;
        } catch (Exception e) {
            log.warn("[LoginAttempt] 读取失败计数异常, 降级为不锁定: {}", e.getMessage());
            return false;
        }
    }

    /** 记录一次登录失败 — 计数 +1 并刷新 15 分钟窗口. */
    public void recordFailure(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        try {
            String k = key(username);
            Object cur = redisTemplate.opsForValue().get(k);
            int count = cur == null ? 0 : Integer.parseInt(cur.toString());
            redisTemplate.opsForValue().set(k, count + 1, LOCK_DURATION);
        } catch (Exception e) {
            log.warn("[LoginAttempt] 记录失败计数异常: {}", e.getMessage());
        }
    }

    /** 登录成功 — 清零失败计数. */
    public void reset(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(key(username));
        } catch (Exception e) {
            log.warn("[LoginAttempt] 清零失败计数异常: {}", e.getMessage());
        }
    }

    /** 锁定时长 (分钟) — 供错误提示文案用. */
    public long lockMinutes() {
        return LOCK_DURATION.toMinutes();
    }
}
