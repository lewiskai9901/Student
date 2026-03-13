package com.school.management.domain.inspection.model.v7.template;

/**
 * V7 模板状态枚举
 * 状态机: DRAFT → PUBLISHED → DEPRECATED → ARCHIVED
 */
public enum TemplateStatus {
    DRAFT,
    PUBLISHED,
    DEPRECATED,
    ARCHIVED;
}
