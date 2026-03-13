package com.school.management.domain.inspection.model.v7.execution;

/**
 * V7 检查提交状态枚举
 * 状态机: PENDING → LOCKED → IN_PROGRESS → COMPLETED
 * 终态: SKIPPED
 */
public enum SubmissionStatus {
    PENDING,
    LOCKED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED;
}
