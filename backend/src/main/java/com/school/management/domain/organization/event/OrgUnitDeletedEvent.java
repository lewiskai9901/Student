package com.school.management.domain.organization.event;

import com.school.management.domain.shared.event.BaseDomainEvent;
import lombok.Getter;

/**
 * Domain event raised when an organization unit is soft-deleted.
 *
 * <p>P8-1 (2026-05-17): 之前 OrgUnitApplicationService.deleteOrgUnit
 * 没 publish 事件 → 删组织无审计.
 */
@Getter
public class OrgUnitDeletedEvent extends BaseDomainEvent {

    private final Long orgUnitId;
    private final String unitCode;
    private final String unitName;
    private final String unitType;
    private final Long parentId;
    private final Long deletedBy;

    public OrgUnitDeletedEvent(Long orgUnitId, String unitCode, String unitName,
                                String unitType, Long parentId, Long deletedBy) {
        super("OrgUnit", String.valueOf(orgUnitId));
        this.orgUnitId = orgUnitId;
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.unitType = unitType;
        this.parentId = parentId;
        this.deletedBy = deletedBy;
    }
}
