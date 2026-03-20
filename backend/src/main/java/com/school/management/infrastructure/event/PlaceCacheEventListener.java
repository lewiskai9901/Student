package com.school.management.infrastructure.event;

import com.school.management.domain.place.event.PlaceCapacityUpdatedEvent;
import com.school.management.infrastructure.cache.PlaceCapacityCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 场所缓存事件监听器
 * 监听容量变更事件，清除相关缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceCacheEventListener {

    private final PlaceCapacityCacheService capacityCacheService;

    /**
     * 监听场所容量更新事件
     * 当容量变化时，清除相关类型的缓存
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePlaceCapacityUpdated(PlaceCapacityUpdatedEvent event) {
        try {
            // 清除该类型的容量预警缓存
            capacityCacheService.clearCache(event.getTypeCode());

            log.debug("已清除容量预警缓存: placeId={}, typeCode={}",
                    event.getPlaceId(), event.getTypeCode());

            // 如果触发预警，记录日志
            if (event.isAlertTriggered()) {
                log.warn("场所容量预警触发: placeId={}, placeName={}, occupancyRate={}%",
                        event.getPlaceId(), event.getPlaceName(), event.getNewOccupancyRate());
            }
        } catch (Exception e) {
            log.error("清除容量预警缓存失败: placeId={}", event.getPlaceId(), e);
        }
    }
}
