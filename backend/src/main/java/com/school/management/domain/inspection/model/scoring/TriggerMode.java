package com.school.management.domain.inspection.model.scoring;

/**
 * Indicator 触发模式 — 评级何时被计算.
 *
 * <ul>
 *   <li>{@link #TIME_WINDOW} 按 evaluationPeriod (DAILY/WEEKLY/MONTHLY) 边界扫</li>
 *   <li>{@link #COUNT} 按累计 submission 次数, 达 countThreshold 触发</li>
 *   <li>{@link #MANUAL} 仅 REST 端点显式触发</li>
 * </ul>
 */
public enum TriggerMode {
    TIME_WINDOW,
    COUNT,
    MANUAL
}
