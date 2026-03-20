package com.school.management.domain.place.event;

import com.school.management.domain.shared.event.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 场所组织分配事件
 * 当场所被分配给组织单元时发布
 */
@Getter
public class PlaceOrgAssignedEvent implements DomainEvent {

    /**
     * 场所ID
     */
    private final Long placeId;

    /**
     * 场所名称
     */
    private final String placeName;

    /**
     * 新组织单元ID（null表示恢复继承）
     */
    private final Long newOrgUnitId;

    /**
     * 旧组织单元ID
     */
    private final Long oldOrgUnitId;

    /**
     * 是否为恢复继承操作
     */
    private final boolean clearingOverride;

    /**
     * 操作原因/备注
     */
    private final String reason;

    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredOn;
    private final String eventId;

    public PlaceOrgAssignedEvent(Long placeId, String placeName, Long oldOrgUnitId, Long newOrgUnitId, String reason) {
        this.placeId = placeId;
        this.placeName = placeName;
        this.oldOrgUnitId = oldOrgUnitId;
        this.newOrgUnitId = newOrgUnitId;
        this.clearingOverride = (newOrgUnitId == null);
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
        return clearingOverride ? "PlaceOrgCleared" : "PlaceOrgAssigned";
    }

    @Override
    public String getAggregateType() {
        return "Place";
    }
}
