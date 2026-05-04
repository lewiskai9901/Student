package com.school.management.domain.inspection.model.execution;

/**
 * 逾期判定策略.
 *
 * <p>不同检查类型对"逾期"语义不同, 用此 policy 在领域层路由:
 * <ul>
 *   <li>{@link #STRICT}: 硬逾期 — deadline 过即 overdue, 影响 KPI (默认 SCHEDULED)</li>
 *   <li>{@link #RELAXED}: 软逾期 — 仅显示提醒, 不影响 KPI (TRIGGERED 默认)</li>
 *   <li>{@link #NONE}: 永不逾期 (AD_HOC/SELF_CHECK 默认)</li>
 * </ul>
 */
public enum DeadlinePolicy {
    STRICT,
    RELAXED,
    NONE;

    /** 根据 TaskType 推导默认 policy */
    public static DeadlinePolicy defaultFor(TaskType type) {
        return switch (type) {
            case SCHEDULED, CROSS_AUDIT -> STRICT;
            case TRIGGERED, COMPLAINT -> RELAXED;
            case AD_HOC, SELF_CHECK -> NONE;
        };
    }
}
