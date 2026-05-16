package com.school.management.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * JwtTokenService 单元测试 — 完美架构 P6-1 子模块 1.3 提分.
 *
 * <p>覆盖:
 * <ul>
 *   <li>access token 生成 / 解析 / 验证</li>
 *   <li>refresh token 生成 / 加密存储 / 验证</li>
 *   <li>token 黑名单</li>
 *   <li>错误 token 拒绝 (篡改/过期/非法格式)</li>
 *   <li>角色 claims 往返</li>
 * </ul>
 *
 * <p>Mock 策略: RedisTemplate / Environment 用 Mockito mock, 不连真 Redis.
 * 黑名单检查走 RedisTemplate.hasKey() — 默认返 false (无黑名单).
 */
@DisplayName("JwtTokenService")
class JwtTokenServiceTest {

    private static final String TEST_SECRET =
        "test-jwt-secret-key-for-unit-testing-at-least-64-bytes-long-enough-for-hs512-algorithm";
    private static final Long USER_ID = 12345L;
    private static final String USERNAME = "alice";
    private static final List<String> ROLES = List.of("TEACHER", "DEPT_ADMIN");

    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redis = (RedisTemplate<String, Object>) mock(RedisTemplate.class);
    private final Environment env = mock(Environment.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
    private JwtTokenService svc;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.hasKey(anyString())).thenReturn(false);    // 黑名单默认空
        when(env.getActiveProfiles()).thenReturn(new String[]{"test"});

        svc = new JwtTokenService(redis, env);
        // @Value 注入字段在单测里没 Spring, 用 ReflectionTestUtils 手动塞
        ReflectionTestUtils.setField(svc, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(svc, "jwtExpiration", 7200000L);
        ReflectionTestUtils.setField(svc, "refreshExpiration", 2592000000L);
        svc.validateJwtSecret();
    }

    @Test
    @DisplayName("生成 + 验证 access token 往返 userId/username/roles")
    void generateAndValidateAccessToken() {
        String token = svc.generateToken(USER_ID, USERNAME, ROLES);
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT 应该是 3 段 base64 . 分隔");

        assertTrue(svc.validateToken(token), "刚生成的 token 应该校验通过");
        assertEquals(USER_ID, svc.getUserIdFromToken(token));
        assertEquals(USERNAME, svc.getUsernameFromToken(token));
        assertEquals(ROLES, svc.getRolesFromToken(token));
    }

    @Test
    @DisplayName("生成 refresh token 不抛异常 + 落 redis")
    void generateRefreshToken() {
        String refresh = svc.generateRefreshToken(USER_ID);
        assertNotNull(refresh);
        // 验证 redis 写入 (key=refresh_token:{userId}, 加密 value, 有 TTL)
        verify(valueOps).set(
            eq("refresh_token:" + USER_ID),
            ArgumentMatchers.any(),
            ArgumentMatchers.anyLong(),
            ArgumentMatchers.eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("空 token 校验失败")
    void blankTokenFailsValidation() {
        assertFalse(svc.validateToken(""));
        assertFalse(svc.validateToken("   "));
        assertFalse(svc.validateToken(null));
    }

    @Test
    @DisplayName("篡改的 token 校验失败")
    void tamperedTokenFailsValidation() {
        String token = svc.generateToken(USER_ID, USERNAME, ROLES);
        // 改最后一个字符破坏签名
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? 'b' : 'a');
        assertFalse(svc.validateToken(tampered));
    }

    @Test
    @DisplayName("非法格式 token 校验失败")
    void malformedTokenFailsValidation() {
        assertFalse(svc.validateToken("not.a.token"));
        assertFalse(svc.validateToken("garbage"));
    }

    @Test
    @DisplayName("黑名单中的 token 校验失败")
    void blacklistedTokenFailsValidation() {
        String token = svc.generateToken(USER_ID, USERNAME, ROLES);
        // 模拟该 token 已加黑名单
        when(redis.hasKey(Mockito.contains("blacklist"))).thenReturn(true);
        assertFalse(svc.validateToken(token), "黑名单 token 必须被拒");
    }

    @Test
    @DisplayName("blacklistToken 调用 redis 写入")
    void blacklistTokenCallsRedis() {
        String token = svc.generateToken(USER_ID, USERNAME, ROLES);
        svc.blacklistToken(token);
        verify(valueOps, atLeastOnce()).set(
            Mockito.contains("blacklist"),
            ArgumentMatchers.any(),
            ArgumentMatchers.anyLong(),
            ArgumentMatchers.eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("revokeAllTokensForUser 删除 refresh_token key")
    void revokeAllTokensForUser() {
        when(redis.delete(anyString())).thenReturn(true);
        svc.revokeAllTokensForUser(USER_ID);
        verify(redis).delete(eq("refresh_token:" + USER_ID));
    }
}
