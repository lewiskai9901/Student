package com.school.management.infrastructure.query;

import com.school.management.infrastructure.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 查询优化服务测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("查询优化服务测试")
class QueryOptimizationServiceTest {

    @Mock
    private CacheService cacheService;

    private QueryOptimizationService queryOptimizationService;

    @BeforeEach
    void setUp() {
        queryOptimizationService = new QueryOptimizationService(cacheService);
    }

    @Nested
    @DisplayName("批量加载缓存测试")
    class BatchLoadWithCacheTest {

        @Test
        @DisplayName("全部缓存命中")
        void shouldReturnAllFromCache() {
            List<Long> ids = Arrays.asList(1L, 2L, 3L);

            when(cacheService.get("user:1")).thenReturn("User1");
            when(cacheService.get("user:2")).thenReturn("User2");
            when(cacheService.get("user:3")).thenReturn("User3");

            Map<Long, String> result = queryOptimizationService.batchLoadWithCache(
                ids,
                "user",
                Duration.ofMinutes(30),
                missingIds -> List.of(), // 不应被调用
                str -> Long.parseLong(str.replace("User", ""))
            );

            assertEquals(3, result.size());
            assertEquals("User1", result.get(1L));
            assertEquals("User2", result.get(2L));
            assertEquals("User3", result.get(3L));

            // 验证没有调用数据库加载
            verify(cacheService, never()).put(anyString(), any(), any());
        }

        @Test
        @DisplayName("部分缓存命中")
        void shouldLoadMissingFromDatabase() {
            List<Long> ids = Arrays.asList(1L, 2L, 3L);

            when(cacheService.get("user:1")).thenReturn("User1");
            when(cacheService.get("user:2")).thenReturn(null); // 缓存未命中
            when(cacheService.get("user:3")).thenReturn("User3");

            Map<Long, String> result = queryOptimizationService.batchLoadWithCache(
                ids,
                "user",
                Duration.ofMinutes(30),
                missingIds -> {
                    assertEquals(1, missingIds.size());
                    assertTrue(missingIds.contains(2L));
                    return List.of("User2");
                },
                str -> Long.parseLong(str.replace("User", ""))
            );

            assertEquals(3, result.size());

            // 验证缓存写入
            verify(cacheService).put(eq("user:2"), eq("User2"), eq(Duration.ofMinutes(30)));
        }

        @Test
        @DisplayName("全部缓存未命中")
        void shouldLoadAllFromDatabase() {
            List<Long> ids = Arrays.asList(1L, 2L);

            when(cacheService.get(anyString())).thenReturn(null);

            Map<Long, String> result = queryOptimizationService.batchLoadWithCache(
                ids,
                "user",
                Duration.ofMinutes(30),
                missingIds -> Arrays.asList("User1", "User2"),
                str -> Long.parseLong(str.replace("User", ""))
            );

            assertEquals(2, result.size());

            verify(cacheService, times(2)).put(anyString(), any(), any());
        }

        @Test
        @DisplayName("空ID列表返回空Map")
        void shouldReturnEmptyForEmptyIds() {
            Map<Long, String> result = queryOptimizationService.batchLoadWithCache(
                List.of(),
                "user",
                Duration.ofMinutes(30),
                ids -> List.of(),
                str -> 0L
            );

            assertTrue(result.isEmpty());
            verifyNoInteractions(cacheService);
        }

        @Test
        @DisplayName("null ID列表返回空Map")
        void shouldReturnEmptyForNullIds() {
            Map<Long, String> result = queryOptimizationService.batchLoadWithCache(
                null,
                "user",
                Duration.ofMinutes(30),
                ids -> List.of(),
                str -> 0L
            );

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("单个加载缓存测试")
    class LoadWithCacheTest {

        @Test
        @DisplayName("缓存命中直接返回")
        void shouldReturnFromCache() {
            when(cacheService.getOrLoad(eq("user:1"), any(), any())).thenReturn("CachedUser");

            String result = queryOptimizationService.loadWithCache(
                1L,
                "user",
                Duration.ofMinutes(30),
                () -> "LoadedUser"
            );

            assertEquals("CachedUser", result);
        }
    }

    @Nested
    @DisplayName("缓存失效测试")
    class InvalidateCacheTest {

        @Test
        @DisplayName("失效单个缓存")
        void shouldInvalidateSingleCache() {
            queryOptimizationService.invalidateCache(1L, "user");

            verify(cacheService).evict("user:1");
        }

        @Test
        @DisplayName("按前缀失效缓存")
        void shouldInvalidateByPrefix() {
            queryOptimizationService.invalidateCacheByPrefix("user");

            verify(cacheService).evictByPattern("user:*");
        }
    }

    @Nested
    @DisplayName("关联数据增强测试")
    class EnrichWithRelatedDataTest {

        record User(Long id, String name, Long deptId, String deptName) {
            User withDeptName(String deptName) {
                return new User(id, name, deptId, deptName);
            }
        }

        record Department(Long id, String name) {}

        @Test
        @DisplayName("批量增强关联数据")
        void shouldEnrichEntities() {
            List<User> users = Arrays.asList(
                new User(1L, "张三", 10L, null),
                new User(2L, "李四", 10L, null),
                new User(3L, "王五", 20L, null)
            );

            // 模拟加载部门数据
            java.util.function.Function<List<Long>, Map<Long, Department>> deptLoader = deptIds -> {
                assertEquals(2, deptIds.size()); // 去重后只有2个部门ID
                return Map.of(
                    10L, new Department(10L, "技术部"),
                    20L, new Department(20L, "产品部")
                );
            };

            List<User> enriched = new java.util.ArrayList<>();
            for (User user : users) {
                // 简单模拟增强逻辑
                enriched.add(user);
            }

            // 实际调用enrichWithRelatedData
            List<User> result = queryOptimizationService.enrichWithRelatedData(
                users,
                User::deptId,
                deptLoader,
                (user, dept) -> {
                    // 这里演示增强逻辑
                }
            );

            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("空列表直接返回")
        void shouldReturnEmptyListAsIs() {
            List<User> result = queryOptimizationService.enrichWithRelatedData(
                List.of(),
                User::deptId,
                ids -> Map.of(),
                (u, d) -> {}
            );

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("null列表直接返回")
        void shouldReturnNullListAsNull() {
            List<User> result = queryOptimizationService.enrichWithRelatedData(
                null,
                User::deptId,
                ids -> Map.of(),
                (u, d) -> {}
            );

            assertNull(result);
        }
    }
}
