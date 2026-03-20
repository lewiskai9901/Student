package com.school.management.domain.place.event;

import com.school.management.domain.shared.event.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 场所容量更新事件
 * 当场所占用人数变化时发布（用于容量预警）
 */
@Getter
public class PlaceCapacityUpdatedEvent implements DomainEvent {

    /**
     * 场所ID
     */
    private final Long placeId;

    /**
     * 场所名称
     */
    private final String placeName;

    /**
     * 场所类型代码
     */
    private final String typeCode;

    /**
     * 新占用人数
     */
    private final Integer newOccupancy;

    /**
     * 旧占用人数
     */
    private final Integer oldOccupancy;

    /**
     * 容量上限
     */
    private final Integer capacity;

    /**
     * 新占用率（%）
     */
    private final Double newOccupancyRate;

    /**
     * 是否达到预警阈值（>= 80%）
     */
    private final boolean alertTriggered;

    /**
     * 操作类型：CHECK_IN（入住）/ CHECK_OUT（退住）/ MANUAL（手动调整）
     */
    private final String operationType;

    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredOn;
    private final String eventId;

    public PlaceCapacityUpdatedEvent(Long placeId, String placeName, String typeCode,
                                      Integer oldOccupancy, Integer newOccupancy, Integer capacity,
                                      String operationType) {
        this.placeId = placeId;
        this.placeName = placeName;
        this.typeCode = typeCode;
        this.oldOccupancy = oldOccupancy;
        this.newOccupancy = newOccupancy;
        this.capacity = capacity;
        this.operationType = operationType;
        this.occurredOn = LocalDateTime.now();
        this.eventId = UUID.randomUUID().toString();

        // 计算新占用率
        if (capacity != null && capacity > 0) {
            this.newOccupancyRate = (newOccupancy * 100.0 / capacity);
            this.alertTriggered = (newOccupancyRate >= 80.0);
        } else {
            this.newOccupancyRate = 0.0;
            this.alertTriggered = false;
        }
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public String aggregateId() {
        return placeId != null ? placeId.toString() : null;
    }

    @Override
    public String getEventType() {
        return "PlaceCapacityUpdated";
    }

    @Override
    public String getAggregateType() {
        return "Place";
    }

    /**
     * 是否为容量增加
     */
    public boolean isCapacityIncreased() {
        return newOccupancy > oldOccupancy;
    }

    /**
     * 容量变化量
     */
    public int getCapacityDelta() {
        return newOccupancy - oldOccupancy;
    }
}
