package com.school.management.domain.inspection.service;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 观察提取上下文 — 提供 submission / project / target 信息
 */
@Getter
@Builder
public class ObservationContext {
    private final Long submissionId;
    private final Long projectId;
    private final Long taskId;
    private final String projectName;

    // 检查目标（submission 的 target）
    private final String targetType;    // STUDENT / CLASS / PLACE / DEPARTMENT
    private final Long targetId;
    private final String targetName;

    // 目标所属班级（冗余）
    private final Long orgUnitId;
    private final String className;

    private final LocalDateTime observedAt;

    // templateItemId → linkedEventTypeCode 映射
    private final Map<Long, String> itemEventTypeMap;

    /**
     * 查询检查项关联的事件类型编码
     */
    public String getLinkedEventType(Long templateItemId) {
        if (templateItemId == null || itemEventTypeMap == null) return null;
        return itemEventTypeMap.get(templateItemId);
    }
}
