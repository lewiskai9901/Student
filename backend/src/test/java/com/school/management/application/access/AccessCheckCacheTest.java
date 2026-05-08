package com.school.management.application.access;

import com.school.management.infrastructure.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AccessCheckCacheTest {

    private CacheService cacheService;
    private AccessCheckCache cache;

    @BeforeEach
    void setUp() {
        cacheService = mock(CacheService.class);
        cache = new AccessCheckCache(cacheService);
        ReflectionTestUtils.setField(cache, "enabled", true);
        ReflectionTestUtils.setField(cache, "ttlSeconds", 60);
    }

    @Test
    void checkCached_callsLoaderOnce_whenCacheMiss() {
        when(cacheService.getOrLoad(anyString(), any(Duration.class), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<Boolean> sup = (Supplier<Boolean>) inv.getArgument(2);
            return sup.get();
        });
        AtomicInteger calls = new AtomicInteger();
        boolean result = cache.checkCached("user", 1L, "admin", "org_unit", 100L,
            () -> { calls.incrementAndGet(); return true; });
        assertThat(result).isTrue();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void disabled_bypassesCache() {
        ReflectionTestUtils.setField(cache, "enabled", false);
        AtomicInteger calls = new AtomicInteger();
        cache.checkCached("user", 1L, "admin", "org_unit", 100L,
            () -> { calls.incrementAndGet(); return true; });
        verifyNoInteractions(cacheService);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void invalidateBySubject_evictsByPattern() {
        cache.invalidateBySubject("user", 7L);
        verify(cacheService).evictByPattern("access:check:user:7:*");
    }

    @Test
    void invalidateByResource_evictsByPattern() {
        cache.invalidateByResource("org_unit", 100L);
        verify(cacheService).evictByPattern("access:check:*:*:*:org_unit:100");
    }

    @Test
    void invalidate_evictsExactKey() {
        cache.invalidate("user", 1L, "admin", "org_unit", 100L);
        verify(cacheService).evict("access:check:user:1:admin:org_unit:100");
    }

    @Test
    void disabled_invalidationIsNoop() {
        ReflectionTestUtils.setField(cache, "enabled", false);
        cache.invalidate("user", 1L, "admin", "org_unit", 100L);
        cache.invalidateBySubject("user", 1L);
        cache.invalidateByResource("org_unit", 100L);
        verifyNoInteractions(cacheService);
    }
}
