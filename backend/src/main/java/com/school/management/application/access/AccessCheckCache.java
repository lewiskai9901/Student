package com.school.management.application.access;

import com.school.management.infrastructure.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/**
 * AccessRelationService.check() 结果缓存层.
 *
 * <p>5 元组 (subject, relation, resource) → boolean 缓存,
 * TTL 默认 60 秒. grant/revoke 主动 invalidate 按 subject + resource pattern 清.
 *
 * <p>缓存可通过 application.yml 关闭:
 * <pre>
 * access.check.cache.enabled: false  # 默认 true
 * access.check.cache.ttl-seconds: 60 # 默认 60
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessCheckCache {

    /** 完整 cache key 形如 "access:check:{subjectType}:{subjectId}:{relation}:{resourceType}:{resourceId}" */
    private static final String KEY_PREFIX = "access:check:";

    private final CacheService cacheService;

    @Value("${access.check.cache.enabled:true}")
    private boolean enabled;

    @Value("${access.check.cache.ttl-seconds:60}")
    private int ttlSeconds;

    /**
     * Cache the boolean check result. If disabled, calls loader directly without caching.
     */
    public boolean checkCached(String subjectType, Long subjectId, String relation,
                               String resourceType, Long resourceId, BooleanSupplier loader) {
        if (!enabled) return loader.getAsBoolean();
        String key = key(subjectType, subjectId, relation, resourceType, resourceId);
        Boolean cached = cacheService.getOrLoad(key, Duration.ofSeconds(ttlSeconds),
            () -> loader.getAsBoolean());
        return cached != null && cached;
    }

    /** 失效某 subject 对所有 resource 的 check 缓存 */
    public void invalidateBySubject(String subjectType, Long subjectId) {
        if (!enabled) return;
        cacheService.evictByPattern(KEY_PREFIX + subjectType + ":" + subjectId + ":*");
    }

    /** 失效所有 subject 对某 resource 的 check 缓存 (按 resource invalidate 跨多 key) */
    public void invalidateByResource(String resourceType, Long resourceId) {
        if (!enabled) return;
        // pattern 末尾匹配 *:resourceType:resourceId
        cacheService.evictByPattern(KEY_PREFIX + "*:*:*:" + resourceType + ":" + resourceId);
    }

    /** 失效特定 5 元组 */
    public void invalidate(String subjectType, Long subjectId, String relation,
                           String resourceType, Long resourceId) {
        if (!enabled) return;
        cacheService.evict(key(subjectType, subjectId, relation, resourceType, resourceId));
    }

    private String key(String subjectType, Long subjectId, String relation,
                       String resourceType, Long resourceId) {
        return KEY_PREFIX + subjectType + ":" + subjectId + ":" + relation + ":"
             + resourceType + ":" + resourceId;
    }
}
