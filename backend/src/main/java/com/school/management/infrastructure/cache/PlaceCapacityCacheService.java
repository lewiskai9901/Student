package com.school.management.infrastructure.cache;

import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.service.PlaceCapacityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 场所容量预警缓存服务
 * 使用Redis缓存高占用率场所列表，减少数据库查询
 * 对标: AWS CloudWatch Metrics Cache
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceCapacityCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PlaceCapacityService capacityService;

    /**
     * 缓存Key前缀
     */
    private static final String CACHE_KEY_PREFIX = "place:capacity:alert:";

    /**
     * 缓存过期时间（分钟）
     */
    private static final int CACHE_EXPIRATION_MINUTES = 5;

    /**
     * 获取高占用率场所（带缓存）
     *
     * @param typeCode 场所类型代码（可选）
     * @return 高占用率场所ID列表
     */
    @SuppressWarnings("unchecked")
    public List<Long> getHighOccupancyPlaceIds(String typeCode) {
        String cacheKey = buildCacheKey(typeCode);

        // 尝试从缓存获取
        List<Long> cachedIds = (List<Long>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedIds != null) {
            log.debug("从缓存获取高占用率场所列表: typeCode={}, count={}", typeCode, cachedIds.size());
            return cachedIds;
        }

        // 缓存未命中，从数据库查询
        List<UniversalPlace> places = capacityService.findHighOccupancyPlaces(typeCode);
        List<Long> placeIds = places.stream()
                .map(UniversalPlace::getId)
                .collect(Collectors.toList());

        // 写入缓存
        redisTemplate.opsForValue().set(cacheKey, placeIds, CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        log.info("已缓存高占用率场所列表: typeCode={}, count={}", typeCode, placeIds.size());

        return placeIds;
    }

    /**
     * 获取高占用率场所数量（带缓存）
     *
     * @param typeCode 场所类型代码（可选）
     * @return 高占用率场所数量
     */
    public int getHighOccupancyCount(String typeCode) {
        return getHighOccupancyPlaceIds(typeCode).size();
    }

    /**
     * 清除缓存
     *
     * @param typeCode 场所类型代码（null表示清除所有）
     */
    public void clearCache(String typeCode) {
        if (typeCode == null) {
            // 清除所有类型的缓存
            redisTemplate.keys(CACHE_KEY_PREFIX + "*")
                    .forEach(redisTemplate::delete);
            log.info("已清除所有容量预警缓存");
        } else {
            // 清除指定类型的缓存
            String cacheKey = buildCacheKey(typeCode);
            redisTemplate.delete(cacheKey);
            log.info("已清除容量预警缓存: typeCode={}", typeCode);
        }
    }

    /**
     * 刷新缓存（重新计算并写入）
     *
     * @param typeCode 场所类型代码（可选）
     */
    public void refreshCache(String typeCode) {
        // 清除旧缓存
        clearCache(typeCode);

        // 重新查询并缓存
        getHighOccupancyPlaceIds(typeCode);

        log.info("已刷新容量预警缓存: typeCode={}", typeCode);
    }

    /**
     * 构建缓存Key
     */
    private String buildCacheKey(String typeCode) {
        if (typeCode == null || typeCode.isBlank()) {
            return CACHE_KEY_PREFIX + "all";
        }
        return CACHE_KEY_PREFIX + typeCode;
    }
}
