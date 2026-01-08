package com.school.management.infrastructure.query;

import com.school.management.infrastructure.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Query optimization service providing common patterns for efficient data retrieval.
 *
 * <p>Features:
 * <ul>
 *   <li>Batch loading to avoid N+1 queries</li>
 *   <li>Cached lookups for frequently accessed data</li>
 *   <li>Lazy loading with caching</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryOptimizationService {

    private final CacheService cacheService;

    /**
     * Batch loads entities by IDs with caching.
     *
     * @param ids       list of IDs to load
     * @param cacheKey  cache key prefix
     * @param ttl       time-to-live for cache entries
     * @param loader    function to load missing entities by IDs
     * @param idExtractor function to extract ID from entity
     * @param <ID>      ID type
     * @param <T>       entity type
     * @return map of ID to entity
     */
    public <ID, T> Map<ID, T> batchLoadWithCache(
            List<ID> ids,
            String cacheKey,
            Duration ttl,
            Function<List<ID>, List<T>> loader,
            Function<T, ID> idExtractor) {

        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        Map<ID, T> result = new ConcurrentHashMap<>();
        List<ID> missingIds = new java.util.ArrayList<>();

        // Check cache first
        for (ID id : ids) {
            String key = cacheKey + ":" + id;
            T cached = cacheService.get(key);
            if (cached != null) {
                result.put(id, cached);
            } else {
                missingIds.add(id);
            }
        }

        // Load missing from database
        if (!missingIds.isEmpty()) {
            log.debug("Batch loading {} missing items for {}", missingIds.size(), cacheKey);
            List<T> loaded = loader.apply(missingIds);

            for (T item : loaded) {
                ID id = idExtractor.apply(item);
                result.put(id, item);
                // Cache the loaded item
                cacheService.put(cacheKey + ":" + id, item, ttl);
            }
        }

        return result;
    }

    /**
     * Loads a single entity with caching.
     *
     * @param id       entity ID
     * @param cacheKey cache key prefix
     * @param ttl      time-to-live
     * @param loader   supplier to load the entity
     * @param <ID>     ID type
     * @param <T>      entity type
     * @return the entity or null
     */
    public <ID, T> T loadWithCache(ID id, String cacheKey, Duration ttl, Supplier<T> loader) {
        String key = cacheKey + ":" + id;
        return cacheService.getOrLoad(key, ttl, loader);
    }

    /**
     * Invalidates cache for a specific entity.
     *
     * @param id       entity ID
     * @param cacheKey cache key prefix
     * @param <ID>     ID type
     */
    public <ID> void invalidateCache(ID id, String cacheKey) {
        cacheService.evict(cacheKey + ":" + id);
    }

    /**
     * Invalidates all cache entries for a cache key prefix.
     *
     * @param cacheKey cache key prefix
     */
    public void invalidateCacheByPrefix(String cacheKey) {
        cacheService.evictByPattern(cacheKey + ":*");
    }

    /**
     * Enriches a list of entities with related data using batch loading.
     *
     * @param entities      list of entities to enrich
     * @param foreignKeyExtractor function to extract foreign key from entity
     * @param relatedLoader function to load related entities by foreign keys
     * @param enricher      function to enrich entity with related data
     * @param <T>           entity type
     * @param <FK>          foreign key type
     * @param <R>           related entity type
     * @return enriched entities
     */
    public <T, FK, R> List<T> enrichWithRelatedData(
            List<T> entities,
            Function<T, FK> foreignKeyExtractor,
            Function<List<FK>, Map<FK, R>> relatedLoader,
            java.util.function.BiConsumer<T, R> enricher) {

        if (entities == null || entities.isEmpty()) {
            return entities;
        }

        // Extract unique foreign keys
        List<FK> foreignKeys = entities.stream()
                .map(foreignKeyExtractor)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (foreignKeys.isEmpty()) {
            return entities;
        }

        // Batch load related data
        Map<FK, R> relatedData = relatedLoader.apply(foreignKeys);

        // Enrich entities
        for (T entity : entities) {
            FK fk = foreignKeyExtractor.apply(entity);
            if (fk != null) {
                R related = relatedData.get(fk);
                if (related != null) {
                    enricher.accept(entity, related);
                }
            }
        }

        return entities;
    }
}
