package com.school.management.domain.asset.event;

import com.school.management.domain.asset.model.valueobject.BuildingType;
import com.school.management.domain.shared.event.DomainEvent;

/**
 * 楼宇创建事件
 */
public class BuildingCreatedEvent extends DomainEvent {

    private final String buildingNo;
    private final String buildingName;
    private final BuildingType buildingType;

    public BuildingCreatedEvent(String aggregateId, String buildingNo,
                                 String buildingName, BuildingType buildingType) {
        super("Building", aggregateId);
        this.buildingNo = buildingNo;
        this.buildingName = buildingName;
        this.buildingType = buildingType;
    }

    public String getBuildingNo() {
        return buildingNo;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public BuildingType getBuildingType() {
        return buildingType;
    }
}
