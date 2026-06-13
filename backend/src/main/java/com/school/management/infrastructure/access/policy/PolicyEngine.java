package com.school.management.infrastructure.access.policy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 授权护栏引擎（授权第 3 道闸）。
 *
 * <p>定位：在 RBAC(能不能调) + 数据范围(能碰哪些行) 放行<b>之后</b>，对"这次动作 × 这个目标"
 * 做最终放行/拦截。<b>默认放行 + deny-overrides</b> —— 加任何规则永远是收紧，绝不会意外放宽。
 *
 * <p>规则由 Spring 注入（所有 {@link PolicyRule} bean）。需要插件扩展时新增 @Component 规则即可。
 */
@Slf4j
@Service
public class PolicyEngine {

    private final List<PolicyRule> rules;

    public PolicyEngine(List<PolicyRule> rules) {
        this.rules = rules == null ? List.of() : rules;
    }

    /** 评估，返回决策（不抛异常）。 */
    public PolicyDecision decide(AccessRequest req) {
        for (PolicyRule rule : rules) {
            if (!rule.appliesTo(req)) continue;
            if (rule.evaluate(req) == Effect.DENY) {
                String reason = rule.denyReason(req);
                log.warn("[policy] DENY action={} resource={}:{} by user={} rule={} reason={}",
                        req.action(), req.resourceType(), req.resourceId(),
                        req.subjectUserId(), rule.id(), reason);
                return PolicyDecision.deny(rule.id(), reason);
            }
        }
        return PolicyDecision.permit();
    }

    /** 评估，拒绝则抛 {@link AccessDeniedException}（→ 全局处理器 403）。 */
    public void requirePermit(AccessRequest req) {
        PolicyDecision d = decide(req);
        if (!d.allowed()) {
            throw new AccessDeniedException(d.reason());
        }
    }
}
