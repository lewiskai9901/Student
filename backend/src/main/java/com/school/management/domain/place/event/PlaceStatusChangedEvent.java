package com.school.management.domain.place.event;

import com.school.management.domain.place.model.valueobject.PlaceStatus;
import com.school.management.domain.shared.event.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 场所状态变更事件
 * 当场所状态（正常/禁用/维护）变更时发布
 */
@Getter
public class PlaceStatusChangedEvent implements DomainEvent {

    /**
     * 场所ID
     */
    private final Long placeId;

    /**
     * 场所名称
     */
    private final String placeName;

    /**
     * 新状态
     */
    private final PlaceStatus newStatus;

    /**
     * 旧状态
     */
    private final PlaceStatus oldStatus;

    /**
     * 操作原因/备注
     */
    private final String reason;

    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredOn;

    private final String eventId;

    public PlaceStatusChangedEvent(Long placeId, String placeName, PlaceStatus oldStatus, PlaceStatus newStatus, String reason) {
        this.placeId = placeId;
        this.placeName = placeName;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.occurredOn = LocalDateTime.now();
        this.eventId = UUID.randomUUID().toString();
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
        return "PlaceStatusChanged";
    }

    @Override
    public String getAggregateType() {
        return "Place";
    }
}
