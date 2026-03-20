package com.school.management.infrastructure.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CacheService单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("缓存服务测试")
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CacheService cacheService;

    private static final String KEY_PREFIX = "sms:";

    @BeforeEach
    void setUp() {
        cacheService = new CacheService(redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("getOrLoad测试")
    class GetOrLoadTest {

        @Test
        @DisplayName("缓存命中时返回缓存值")
        void shouldReturnCachedValueWhenHit() {
            String key = "user:1";
            String cachedValue = "cached-user";
            when(valueOperations.get(KEY_PREFIX + key)).thenReturn(cachedValue);

            String result = cacheService.getOrLoad(key, Duration.ofMinutes(30), () -> "loaded-user");

            assertEquals(cachedValue, result);
            verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
        }

        @Test
        @DisplayName("缓存未命中时加载并缓存")
        void shouldLoadAndCacheWhenMiss() {
            String key = "user:2";
            String loadedValue = "loaded-user";
            when(valueOperations.get(KEY_PREFIX + key)).thenReturn(null);

            String result = cacheService.getOrLoad(key, Duration.ofMinutes(30), () -> loadedValue);

            assertEquals(loadedValue, result);
            verify(valueOperations).set(eq(KEY_PREFIX + key), eq(loadedValue), eq(Duration.ofMinutes(30)));
        }

        @Test
        @DisplayName("加载结果为null时不缓存")
        void shouldNotCacheNullValue() {
            String key = "user:3";
            when(valueOperations.get(KEY_PREFIX + key)).thenReturn(null);

            String result = cacheService.getOrLoad(key, Duration.ofMinutes(30), () -> null);

            assertNull(result);
            verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("put测试")
    class PutTest {

        @Test
        @DisplayName("成功放入缓存")
        void shouldPutValueSuccessfully() {
            String key = "config:app";
            String value = "config-value";

            cacheService.put(key, value, Duration.ofHours(2));

            verify(valueOperations).set(eq(KEY_PREFIX + key), eq(value), eq(Duration.ofHours(2)));
        }

        @Test
        @DisplayName("null值不放入缓存")
        void shouldNotPutNullValue() {
            cacheService.put("key", null, Duration.ofMinutes(30));

            verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("get测试")
    class GetTest {

        @Test
        @DisplayName("获取缓存值")
        void shouldGetValue() {
            String key = "user:1";
            String value = "user-data";
            when(valueOperations.get(KEY_PREFIX + key)).thenReturn(value);

            String result = cacheService.get(key);

            assertEquals(value, result);
        }

        @Test
        @DisplayName("缓存不存在返回null")
        void shouldReturnNullWhenNotExists() {
            String key = "user:999";
            when(valueOperations.get(KEY_PREFIX + key)).thenReturn(null);

            String result = cacheService.get(key);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("evict测试")
    class EvictTest {

        @Test
        @DisplayName("删除缓存")
        void shouldEvictKey() {
            String key = "user:1";

            cacheService.evict(key);

            verify(redisTemplate).delete(KEY_PREFIX + key);
        }
    }

    @Nested
    @DisplayName("evictByPattern测试")
    class EvictByPatternTest {

        @SuppressWarnings("unchecked")
        private Cursor<String> mockCursor(List<String> keys) {
            Iterator<String> iterator = keys.iterator();
            Cursor<String> cursor = mock(Cursor.class);
            when(cursor.hasNext()).thenAnswer(inv -> iterator.hasNext());
            lenient().when(cursor.next()).thenAnswer(inv -> iterator.next());
            return cursor;
        }

        @Test
        @DisplayName("按模式删除缓存")
        @SuppressWarnings("unchecked")
        void shouldEvictByPattern() {
            String pattern = "user:*";
            List<String> keyList = List.of(KEY_PREFIX + "user:1", KEY_PREFIX + "user:2");
            Cursor<String> cursor = mockCursor(keyList);
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

            cacheService.evictByPattern(pattern);

            verify(redisTemplate).delete(Set.of(KEY_PREFIX + "user:1", KEY_PREFIX + "user:2"));
        }

        @Test
        @DisplayName("模式无匹配时不删除")
        @SuppressWarnings("unchecked")
        void shouldNotDeleteWhenNoMatch() {
            String pattern = "nonexistent:*";
            Cursor<String> cursor = mockCursor(List.of());
            when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

            cacheService.evictByPattern(pattern);

            verify(redisTemplate, never()).delete(anyCollection());
        }
    }

    @Nested
    @DisplayName("exists测试")
    class ExistsTest {

        @Test
        @DisplayName("存在返回true")
        void shouldReturnTrueWhenExists() {
            String key = "user:1";
            when(redisTemplate.hasKey(KEY_PREFIX + key)).thenReturn(true);

            assertTrue(cacheService.exists(key));
        }

        @Test
        @DisplayName("不存在返回false")
        void shouldReturnFalseWhenNotExists() {
            String key = "user:999";
            when(redisTemplate.hasKey(KEY_PREFIX + key)).thenReturn(false);

            assertFalse(cacheService.exists(key));
        }
    }

    @Nested
    @DisplayName("expire测试")
    class ExpireTest {

        @Test
        @DisplayName("设置过期时间")
        void shouldSetExpire() {
            String key = "session:abc";
            Duration ttl = Duration.ofMinutes(30);
            when(redisTemplate.expire(eq(KEY_PREFIX + key), eq(ttl.toMillis()), eq(TimeUnit.MILLISECONDS)))
                .thenReturn(true);

            boolean result = cacheService.expire(key, ttl);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("increment测试")
    class IncrementTest {

        @Test
        @DisplayName("递增计数器")
        void shouldIncrement() {
            String key = "counter:visits";
            when(valueOperations.increment(KEY_PREFIX + key, 1L)).thenReturn(42L);

            Long result = cacheService.increment(key, 1L);

            assertEquals(42L, result);
        }

        @Test
        @DisplayName("递增指定值")
        void shouldIncrementByDelta() {
            String key = "counter:score";
            when(valueOperations.increment(KEY_PREFIX + key, 10L)).thenReturn(100L);

            Long result = cacheService.increment(key, 10L);

            assertEquals(100L, result);
        }
    }

    @Nested
    @DisplayName("便捷方法测试")
    class ConvenienceMethodsTest {

        @Test
        @DisplayName("用户缓存获取")
        void shouldGetUserCache() {
            Long userId = 1L;
            String userData = "user-data";
            when(valueOperations.get(KEY_PREFIX + "user:" + userId)).thenReturn(userData);

            String result = cacheService.getUserCache(userId, () -> "loaded");

            assertEquals(userData, result);
        }

        @Test
        @DisplayName("用户缓存失效")
        void shouldEvictUserCache() {
            Long userId = 1L;

            cacheService.evictUserCache(userId);

            verify(redisTemplate).delete(KEY_PREFIX + "user:" + userId);
        }

        @Test
        @DisplayName("权限缓存获取")
        void shouldGetPermissionCache() {
            Long userId = 1L;
            when(valueOperations.get(KEY_PREFIX + "permission:" + userId)).thenReturn(null);

            cacheService.getPermissionCache(userId, () -> "permissions");

            verify(valueOperations).set(
                eq(KEY_PREFIX + "permission:" + userId),
                eq("permissions"),
                eq(Duration.ofMinutes(30))
            );
        }

        @Test
        @DisplayName("配置缓存获取")
        void shouldGetConfigCache() {
            String configKey = "app.settings";
            when(valueOperations.get(KEY_PREFIX + "config:" + configKey)).thenReturn(null);

            cacheService.getConfigCache(configKey, () -> "config-value");

            verify(valueOperations).set(
                eq(KEY_PREFIX + "config:" + configKey),
                eq("config-value"),
                eq(Duration.ofHours(2))
            );
        }
    }
}
