package com.school.management.domain.organization.event;

import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.model.OrgUnitType;
import com.school.management.domain.shared.event.DomainEvent;

/**
 * Domain event raised when an organization unit is created.
 */
public class OrgUnitCreatedEvent extends DomainEvent {

    private final Long orgUnitId;
    private final String unitCode;
    private final String unitName;
    private final OrgUnitType unitType;
    private final Long parentId;
    private final Long createdBy;

    public OrgUnitCreatedEvent(OrgUnit orgUnit) {
        super("OrgUnit", String.valueOf(orgUnit.getId()));
        this.orgUnitId = orgUnit.getId();
        this.unitCode = orgUnit.getUnitCode();
        this.unitName = orgUnit.getUnitName();
        this.unitType = orgUnit.getUnitType();
        this.parentId = orgUnit.getParentId();
        this.createdBy = orgUnit.getCreatedBy();
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public String getUnitCode() {
        return unitCode;
    }

    public String getUnitName() {
        return unitName;
    }

    public OrgUnitType getUnitType() {
        return unitType;
    }

    public Long getParentId() {
        return parentId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }
}
