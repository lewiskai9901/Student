package com.school.management.domain.inspection.model.scoring;

/**
 * Indicator 归属周期时, 取 submission 哪个日期字段.
 *
 * <ul>
 *   <li>{@link #taskDate} 任务计划日 (默认; 反映"应检日"语义)</li>
 *   <li>{@link #completedAt} 实际完成时刻 (反映"实检日"语义)</li>
 * </ul>
 */
public enum SubmissionDateField {
    taskDate,
    completedAt
}
