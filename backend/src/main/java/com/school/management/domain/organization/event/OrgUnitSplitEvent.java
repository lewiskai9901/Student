package com.school.management.domain.organization.event;

import com.school.management.domain.shared.event.BaseDomainEvent;
import lombok.Getter;

import java.util.List;

/**
 * Domain event raised when an orgUnit is split into N new ones.
 *
 * <p>P8-1: 拆分跟合并对称, 需要审计.
 */
@Getter
public class OrgUnitSplitEvent extends BaseDomainEvent {

    private final Long sourceOrgUnitId;
    private final String sourceUnitCode;
    private final List<Long> newOrgUnitIds;
    private final String reason;
    private final Long splitBy;

    public OrgUnitSplitEvent(Long sourceOrgUnitId, String sourceUnitCode,
                              List<Long> newOrgUnitIds, String reason, Long splitBy) {
        super("OrgUnit", String.valueOf(sourceOrgUnitId));
        this.sourceOrgUnitId = sourceOrgUnitId;
        this.sourceUnitCode = sourceUnitCode;
        this.newOrgUnitIds = newOrgUnitIds;
        this.reason = reason;
        this.splitBy = splitBy;
    }
}
