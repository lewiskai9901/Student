package com.school.management.domain.asset.event;

import com.school.management.domain.shared.event.DomainEvent;

/**
 * 宿舍创建事件
 */
public class DormitoryCreatedEvent extends DomainEvent {

    private final String dormitoryNo;
    private final Long buildingId;
    private final Integer bedCapacity;

    public DormitoryCreatedEvent(String aggregateId, String dormitoryNo,
                                  Long buildingId, Integer bedCapacity) {
        super("Dormitory", aggregateId);
        this.dormitoryNo = dormitoryNo;
        this.buildingId = buildingId;
        this.bedCapacity = bedCapacity;
    }

    public String getDormitoryNo() {
        return dormitoryNo;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public Integer getBedCapacity() {
        return bedCapacity;
    }
}
