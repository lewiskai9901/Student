package com.school.management.domain.asset.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 楼宇更新事件
 */
public class BuildingUpdatedEvent extends BaseDomainEvent {

    private final String buildingNo;
    private final String buildingName;

    public BuildingUpdatedEvent(String aggregateId, String buildingNo, String buildingName) {
        super("Building", aggregateId);
        this.buildingNo = buildingNo;
        this.buildingName = buildingName;
    }

    public String getBuildingNo() {
        return buildingNo;
    }

    public String getBuildingName() {
        return buildingName;
    }
}
