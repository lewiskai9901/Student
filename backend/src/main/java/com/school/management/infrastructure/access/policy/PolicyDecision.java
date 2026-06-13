package com.school.management.infrastructure.access.policy;

/**
 * 策略引擎决策结果。
 *
 * @param allowed 是否放行
 * @param ruleId  命中的规则 id（拒绝时为拒绝规则；放行时为 null）
 * @param reason  拒绝原因（放行时为 null）
 */
public record PolicyDecision(boolean allowed, String ruleId, String reason) {

    public static PolicyDecision permit() {
        return new PolicyDecision(true, null, null);
    }

    public static PolicyDecision deny(String ruleId, String reason) {
        return new PolicyDecision(false, ruleId, reason);
    }
}
