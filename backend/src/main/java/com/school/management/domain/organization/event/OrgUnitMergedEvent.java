package com.school.management.domain.organization.event;

import com.school.management.domain.shared.event.BaseDomainEvent;
import lombok.Getter;

/**
 * Domain event raised when source orgUnit is merged into target.
 *
 * <p>P8-1: 合并是重大组织变更, 需要审计 + 通知 + 数据迁移联动.
 */
@Getter
public class OrgUnitMergedEvent extends BaseDomainEvent {

    private final Long sourceOrgUnitId;
    private final Long targetOrgUnitId;
    private final String sourceUnitCode;
    private final String targetUnitCode;
    private final String reason;
    private final Long mergedBy;

    public OrgUnitMergedEvent(Long sourceOrgUnitId, Long targetOrgUnitId,
                               String sourceUnitCode, String targetUnitCode,
                               String reason, Long mergedBy) {
        super("OrgUnit", String.valueOf(sourceOrgUnitId));
        this.sourceOrgUnitId = sourceOrgUnitId;
        this.targetOrgUnitId = targetOrgUnitId;
        this.sourceUnitCode = sourceUnitCode;
        this.targetUnitCode = targetUnitCode;
        this.reason = reason;
        this.mergedBy = mergedBy;
    }
}
