package com.school.management.infrastructure.scheduler;

import com.school.management.infrastructure.cache.PlaceCapacityCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 容量预警定时任务
 * 每5分钟刷新一次容量预警缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CapacityAlertScheduler {

    private final PlaceCapacityCacheService capacityCacheService;

    /**
     * 刷新容量预警缓存
     * 定时任务：每5分钟执行一次
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void refreshCapacityAlertCache() {
        try {
            log.debug("开始刷新容量预警缓存...");

            // 刷新所有类型的缓存
            capacityCacheService.refreshCache(null);

            log.info("容量预警缓存刷新完成");
        } catch (Exception e) {
            log.error("刷新容量预警缓存失败", e);
        }
    }
}
