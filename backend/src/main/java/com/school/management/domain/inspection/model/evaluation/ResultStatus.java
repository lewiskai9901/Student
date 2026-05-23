package com.school.management.domain.inspection.model.evaluation;

/**
 * IndicatorResult 状态.
 *
 * <ul>
 *   <li>{@link #DRAFT} 已计算未发布, 可重写</li>
 *   <li>{@link #PUBLISHED} 已发布, 不可改; 补做触发新 revision</li>
 *   <li>{@link #SUPERSEDED} 被更新版本覆盖, 历史保留</li>
 * </ul>
 */
public enum ResultStatus {
    DRAFT,
    PUBLISHED,
    SUPERSEDED
}
