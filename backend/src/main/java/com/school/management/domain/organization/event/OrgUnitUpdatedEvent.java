package com.school.management.domain.organization.event;

import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * Domain event raised when an organization unit is updated.
 */
public class OrgUnitUpdatedEvent extends BaseDomainEvent {

    private final Long orgUnitId;
    private final String unitName;
    private final Long updatedBy;

    public OrgUnitUpdatedEvent(OrgUnit orgUnit) {
        super("OrgUnit", String.valueOf(orgUnit.getId()));
        this.orgUnitId = orgUnit.getId();
        this.unitName = orgUnit.getUnitName();
        this.updatedBy = orgUnit.getUpdatedBy();
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }
}
