package com.school.management.domain.inspection.model.template;

/**
 * 模板状态枚举
 * 状态机: DRAFT → PUBLISHED → DEPRECATED → ARCHIVED
 */
public enum TemplateStatus {
    DRAFT,
    PUBLISHED,
    DEPRECATED,
    ARCHIVED;
}
