package com.school.management.service;

import com.school.management.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 登录失败次数限制测试
 * 验证登录限制常量配置正确
 */
@DisplayName("登录失败次数限制配置测试")
class AuthServiceLoginLimitTest {

    @Test
    @DisplayName("最大登录尝试次数应为5次")
    void shouldHaveMaxLoginAttemptsOf5() {
        int maxAttempts = (int) ReflectionTestUtils.getField(AuthServiceImpl.class, "MAX_LOGIN_ATTEMPTS");
        assertEquals(5, maxAttempts, "最大登录尝试次数应为5次");
    }

    @Test
    @DisplayName("锁定时长应为30分钟")
    void shouldHaveLockoutDurationOf30Minutes() {
        int lockoutMinutes = (int) ReflectionTestUtils.getField(AuthServiceImpl.class, "LOCKOUT_DURATION_MINUTES");
        assertEquals(30, lockoutMinutes, "锁定时长应为30分钟");
    }

    @Test
    @DisplayName("登录尝试缓存键前缀应正确")
    void shouldHaveCorrectLoginAttemptKeyPrefix() {
        String keyPrefix = (String) ReflectionTestUtils.getField(AuthServiceImpl.class, "LOGIN_ATTEMPT_KEY");
        assertEquals("login:attempt:", keyPrefix, "登录尝试缓存键前缀应为 login:attempt:");
    }

    @Test
    @DisplayName("Redis 失败阈值应为3次")
    void shouldHaveRedisFailureThreshold() {
        int threshold = (int) ReflectionTestUtils.getField(AuthServiceImpl.class, "MAX_REDIS_FAILURES_BEFORE_STRICT_MODE");
        assertEquals(3, threshold, "Redis 失败阈值应为3次");
    }
}
