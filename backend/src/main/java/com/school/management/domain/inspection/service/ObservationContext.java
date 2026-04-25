package com.school.management.domain.inspection.service;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 观察提取上下文 — 提供 submission / project / target 信息.
 *
 * <p>通用核心: 不耦合行业概念. 检查目标的具体类型(班级/年级/部门/病房...)
 * 由 entity_type_configs 决定, 此处只用通用 USER/ORG_UNIT/PLACE/ASSET 主体语义.
 */
@Getter
@Builder
public class ObservationContext {
    private final Long submissionId;
    private final Long projectId;
    private final Long taskId;
    private final String projectName;

    // 检查目标 (submission 的 target)
    private final String targetType;        // USER / ORG / PLACE / ASSET (TargetType.name())
    private final Long targetId;
    private final String targetName;

    // 主体所属组织 (冗余, 加速查询)
    private final Long orgUnitId;
    private final String orgUnitName;

    private final LocalDateTime observedAt;

    // templateItemId → linkedEventTypeCode 映射
    private final Map<Long, String> itemEventTypeMap;

    /** 查询检查项关联的事件类型编码 */
    public String getLinkedEventType(Long templateItemId) {
        if (templateItemId == null || itemEventTypeMap == null) return null;
        return itemEventTypeMap.get(templateItemId);
    }
}
