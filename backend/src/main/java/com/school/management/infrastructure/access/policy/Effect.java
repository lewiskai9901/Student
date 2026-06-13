package com.school.management.infrastructure.access.policy;

/**
 * 策略规则评估结果。
 *
 * <p>引擎用 deny-overrides 合并：任一规则 {@link #DENY} 即整体拒绝；
 * 否则放行（默认 PERMIT）。{@link #NOT_APPLICABLE} 表示该规则不适用本请求、不表态。
 */
public enum Effect {
    PERMIT,
    DENY,
    NOT_APPLICABLE
}
