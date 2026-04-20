package com.school.management.infrastructure.extension;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 策略注册表 — 运行时集中执行所有 Policy bean.
 *
 * Spring 自动注入所有实现 Policy 接口的 @Component bean.
 *
 * 两个主 API:
 *   check(ctx)   — 执行所有 supports(ctx)=true 的策略, 返回全部违规 (BLOCK/WARN/INFO)
 *   enforce(ctx) — 同 check, 但一旦含 BLOCK 抛 PolicyViolationException
 *
 * 典型调用处在 ApplicationService 的变更边界:
 *   registry.enforce(new PolicyContext&lt;&gt;("place", "BEFORE_CHECKIN", cmd));
 *   // ... do business ...
 *   List&lt;Violation&gt; warns = registry.check(new PolicyContext&lt;&gt;("place", "AFTER_CHECKIN", cmd));
 *   warns.forEach(notificationService::warn);
 */
@Component
public class PolicyRegistry {

    private final List<Policy<?>> policies;

    public PolicyRegistry(List<Policy<?>> policies) {
        this.policies = List.copyOf(policies);
    }

    /** 所有已注册 Policy bean (immutable). 供管理面板内省使用. */
    public List<Policy<?>> getPolicies() {
        return policies;
    }

    /** 返回所有匹配 supports(ctx) 的策略产生的违规; 不抛异常 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Violation> check(PolicyContext<?> ctx) {
        return policies.stream()
            .filter(p -> p.supports(ctx))
            .flatMap(p -> ((Policy) p).check(ctx).stream())
            .map(v -> (Violation) v)
            .toList();
    }

    /** 同 check, 但如果任何违规 severity=BLOCK, 抛 PolicyViolationException */
    public List<Violation> enforce(PolicyContext<?> ctx) {
        List<Violation> all = check(ctx);
        boolean hasBlock = all.stream().anyMatch(v -> v.severity() == Violation.Severity.BLOCK);
        if (hasBlock) {
            throw new PolicyViolationException(all);
        }
        return all;
    }
}
