package com.school.management.domain.inspection.model.scoring;

/**
 * 逾期补做(late submission)归属策略.
 *
 * <ul>
 *   <li>{@link #REVISE_ORIGINAL} 触发已发布 IndicatorResult 重算 + 生成 SUPERSEDED 修订版</li>
 *   <li>{@link #CARRY_FORWARD} 把 submission 归属改为当前周期</li>
 *   <li>{@link #EXCLUDE} 该 submission 不参与任何 Indicator 评估</li>
 * </ul>
 */
public enum LatePolicy {
    REVISE_ORIGINAL,
    CARRY_FORWARD,
    EXCLUDE
}
