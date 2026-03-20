package com.school.management.domain.place.event;

import com.school.management.domain.shared.event.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 场所负责人分配事件
 * 当场所负责人变更时发布
 */
@Getter
public class PlaceResponsibleAssignedEvent implements DomainEvent {

    /**
     * 场所ID
     */
    private final Long placeId;

    /**
     * 场所名称
     */
    private final String placeName;

    /**
     * 新负责人ID
     */
    private final Long newResponsibleUserId;

    /**
     * 旧负责人ID
     */
    private final Long oldResponsibleUserId;

    /**
     * 操作原因/备注
     */
    private final String reason;

    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredOn;
    private final String eventId;

    public PlaceResponsibleAssignedEvent(Long placeId, String placeName, Long oldResponsibleUserId, Long newResponsibleUserId, String reason) {
        this.placeId = placeId;
        this.placeName = placeName;
        this.oldResponsibleUserId = oldResponsibleUserId;
        this.newResponsibleUserId = newResponsibleUserId;
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
        return "PlaceResponsibleAssigned";
    }

    @Override
    public String getAggregateType() {
        return "Place";
    }
}
