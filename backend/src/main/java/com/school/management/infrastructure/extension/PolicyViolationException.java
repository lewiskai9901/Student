package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 策略违规异常 — PolicyRegistry.enforce() 发现 BLOCK 级违规时抛出.
 *
 * 在 REST 层通过 @ControllerAdvice 可捕获并返回 400 Bad Request + 违规列表 JSON.
 */
public class PolicyViolationException extends RuntimeException {

    private final List<Violation> violations;

    public PolicyViolationException(List<Violation> violations) {
        super(buildMessage(violations));
        this.violations = violations;
    }

    public List<Violation> getViolations() {
        return violations;
    }

    private static String buildMessage(List<Violation> vs) {
        return vs.stream()
            .map(v -> "[" + v.severity() + ":" + v.code() + "] " + v.message())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Policy violation");
    }
}
