package com.school.management.domain.inspection.model.execution;

/**
 * 检查项目状态枚举
 * 状态机: DRAFT → PUBLISHED → PAUSED → COMPLETED → ARCHIVED
 */
public enum ProjectStatus {
    DRAFT,
    PUBLISHED,
    PAUSED,
    COMPLETED,
    ARCHIVED;
}
