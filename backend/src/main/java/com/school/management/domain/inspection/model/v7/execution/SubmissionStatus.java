package com.school.management.domain.inspection.model.v7.execution;

/**
 * V7 检查提交状态枚举
 * 状态机: PENDING → LOCKED → IN_PROGRESS → COMPLETED
 *         PENDING → OPEN (持续检查模式，可反复添加记录) → COMPLETED
 * 终态: SKIPPED
 */
public enum SubmissionStatus {
    PENDING,
    LOCKED,
    IN_PROGRESS,
    OPEN,           // 持续检查模式：时间段内可反复添加记录
    COMPLETED,
    SKIPPED;
}
