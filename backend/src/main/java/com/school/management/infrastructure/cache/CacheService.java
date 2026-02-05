package com.school.management.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Cache service for managing Redis cache operations.
 *
 * <p>Provides utility methods for common caching patterns:
 * <ul>
 *   <li>Get-or-load pattern</li>
 *   <li>Cache invalidation</li>
 *   <li>Pattern-based key deletion</li>
 * </ul>
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String KEY_PREFIX = "sms:";

    /**
     * Gets a value from cache, or loads it using the supplier if not present.
     *
     * @param key      cache key
     * @param ttl      time-to-live
     * @param loader   supplier to load the value if not cached
     * @param <T>      value type
     * @return the cached or loaded value
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader) {
        String fullKey = KEY_PREFIX + key;
        Object cached = redisTemplate.opsForValue().get(fullKey);

        if (cached != null) {
            log.debug("Cache hit: {}", key);
            return (T) cached;
        }

        log.debug("Cache miss: {}", key);
        T value = loader.get();

        if (value != null) {
            redisTemplate.opsForValue().set(fullKey, value, ttl);
        }

        return value;
    }

    /**
     * Puts a value into cache.
     *
     * @param key   cache key
     * @param value value to cache
     * @param ttl   time-to-live
     * @param <T>   value type
     */
    public <T> void put(String key, T value, Duration ttl) {
        if (value == null) return;
        String fullKey = KEY_PREFIX + key;
        redisTemplate.opsForValue().set(fullKey, value, ttl);
        log.debug("Cache put: {}", key);
    }

    /**
     * Gets a value from cache.
     *
     * @param key cache key
     * @param <T> value type
     * @return the cached value or null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        String fullKey = KEY_PREFIX + key;
        return (T) redisTemplate.opsForValue().get(fullKey);
    }

    /**
     * Removes a value from cache.
     *
     * @param key cache key
     */
    public void evict(String key) {
        String fullKey = KEY_PREFIX + key;
        redisTemplate.delete(fullKey);
        log.debug("Cache evict: {}", key);
    }

    /**
     * Removes all values matching a pattern.
     *
     * @param pattern key pattern (e.g., "user:*")
     */
    public void evictByPattern(String pattern) {
        String fullPattern = KEY_PREFIX + pattern;
        Set<String> keys = redisTemplate.keys(fullPattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Cache evict pattern: {}, count: {}", pattern, keys.size());
        }
    }

    /**
     * Removes multiple keys.
     *
     * @param keys collection of keys
     */
    public void evictAll(Collection<String> keys) {
        keys.forEach(this::evict);
    }

    /**
     * Checks if a key exists in cache.
     *
     * @param key cache key
     * @return true if exists
     */
    public boolean exists(String key) {
        String fullKey = KEY_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
    }

    /**
     * Sets the expiration time for a key.
     *
     * @param key cache key
     * @param ttl time-to-live
     * @return true if successful
     */
    public boolean expire(String key, Duration ttl) {
        String fullKey = KEY_PREFIX + key;
        return Boolean.TRUE.equals(
            redisTemplate.expire(fullKey, ttl.toMillis(), TimeUnit.MILLISECONDS));
    }

    /**
     * Increments a counter.
     *
     * @param key   cache key
     * @param delta increment value
     * @return new value
     */
    public Long increment(String key, long delta) {
        String fullKey = KEY_PREFIX + key;
        return redisTemplate.opsForValue().increment(fullKey, delta);
    }

    /**
     * Clears all cache entries with the application prefix.
     */
    public void clearAll() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Cache cleared: {} keys", keys.size());
        }
    }

    // ========== Convenience Methods for Common Caches ==========

    public <T> T getUserCache(Long userId, Supplier<T> loader) {
        return getOrLoad("user:" + userId, Duration.ofMinutes(30), loader);
    }

    public void evictUserCache(Long userId) {
        evict("user:" + userId);
    }

    public <T> T getPermissionCache(Long userId, Supplier<T> loader) {
        return getOrLoad("permission:" + userId, Duration.ofMinutes(30), loader);
    }

    public void evictPermissionCache(Long userId) {
        evict("permission:" + userId);
    }

    public <T> T getConfigCache(String configKey, Supplier<T> loader) {
        return getOrLoad("config:" + configKey, Duration.ofHours(2), loader);
    }

    public void evictConfigCache(String configKey) {
        evict("config:" + configKey);
    }

    public <T> T getTemplateCache(Long templateId, Supplier<T> loader) {
        return getOrLoad("template:" + templateId, Duration.ofHours(2), loader);
    }

    public void evictTemplateCache(Long templateId) {
        evict("template:" + templateId);
    }
}
