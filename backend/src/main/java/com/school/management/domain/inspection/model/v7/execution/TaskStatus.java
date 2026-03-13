package com.school.management.domain.inspection.model.v7.execution;

/**
 * V7 检查任务状态枚举
 * 状态机: PENDING → CLAIMED → IN_PROGRESS → SUBMITTED → UNDER_REVIEW → REVIEWED → PUBLISHED
 * 终态: CANCELLED, EXPIRED
 */
public enum TaskStatus {
    PENDING,
    CLAIMED,
    IN_PROGRESS,
    SUBMITTED,
    UNDER_REVIEW,
    REVIEWED,
    PUBLISHED,
    CANCELLED,
    EXPIRED;
}
