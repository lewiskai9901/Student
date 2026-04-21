package com.school.management.infrastructure.extension;

import com.school.management.infrastructure.extension.event.PolicyWarningEvent;
import org.springframework.context.ApplicationEventPublisher;
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
 *
 * M4.3: 非 BLOCK 违规会自动发布 {@link PolicyWarningEvent},
 *       由 PolicyWarningToNotificationListener 接入消息派发 (POLICY_WARNING 触发点).
 *       BLOCK 违规走异常路径, 不发事件 (enforce 会抛异常, 链路中断).
 */
@Component
public class PolicyRegistry {

    private final List<Policy<?>> policies;
    private final ApplicationEventPublisher eventPublisher;

    public PolicyRegistry(List<Policy<?>> policies, ApplicationEventPublisher eventPublisher) {
        this.policies = List.copyOf(policies);
        this.eventPublisher = eventPublisher;
    }

    /** 所有已注册 Policy bean (immutable). 供管理面板内省使用. */
    public List<Policy<?>> getPolicies() {
        return policies;
    }

    /** 返回所有匹配 supports(ctx) 的策略产生的违规; 不抛异常 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Violation> check(PolicyContext<?> ctx) {
        List<Violation> all = policies.stream()
            .filter(p -> p.supports(ctx))
            .flatMap(p -> ((Policy) p).check(ctx).stream())
            .map(v -> (Violation) v)
            .toList();
        publishNonBlockWarnings(ctx, all);
        return all;
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

    /**
     * M4.3: 过滤非 BLOCK 违规并发布 PolicyWarningEvent.
     * BLOCK 不发: enforce 会抛 PolicyViolationException 中断主流程, 消息无法写入;
     *            如果业务需要通知 BLOCK, 应在 catch 块主动调 triggerService.fire.
     * 发布异常不吞不抛 — 调 ApplicationEventPublisher 本身应该不会失败,
     * 失败也只影响通知而非策略检查本身, 因此此处不做额外防御.
     */
    private void publishNonBlockWarnings(PolicyContext<?> ctx, List<Violation> all) {
        if (all.isEmpty()) return;
        List<Violation> nonBlock = all.stream()
            .filter(v -> v.severity() != Violation.Severity.BLOCK)
            .toList();
        if (nonBlock.isEmpty()) return;
        eventPublisher.publishEvent(new PolicyWarningEvent(this, ctx, nonBlock));
    }
}
