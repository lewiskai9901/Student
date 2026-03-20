package com.school.management.domain.organization.event;

import com.school.management.domain.organization.model.Position;
import com.school.management.domain.shared.event.BaseDomainEvent;

public class PositionCreatedEvent extends BaseDomainEvent {

    private final Long positionId;
    private final String positionCode;
    private final String positionName;
    private final Long orgUnitId;
    private final Long createdBy;

    public PositionCreatedEvent(Position position) {
        super("Position", position.getId());
        this.positionId = position.getId();
        this.positionCode = position.getPositionCode();
        this.positionName = position.getPositionName();
        this.orgUnitId = position.getOrgUnitId();
        this.createdBy = position.getCreatedBy();
    }

    public Long getPositionId() { return positionId; }
    public String getPositionCode() { return positionCode; }
    public String getPositionName() { return positionName; }
    public Long getOrgUnitId() { return orgUnitId; }
    public Long getCreatedBy() { return createdBy; }
}
