package com.school.management.infrastructure.access.policy;

/**
 * 授权护栏规则。实现类标 {@code @Component} 即被 {@link PolicyEngine} 自动收集。
 *
 * <p>规则应是<b>纯函数</b>：只读 {@link AccessRequest}，不查库、无副作用 —— 目标属性已由
 * 解析器填进 request.attrs。这样规则可脱库单测。
 */
public interface PolicyRule {

    /** 规则唯一 id，用于审计/日志，如 "protect-super-admin" */
    String id();

    /** 本规则是否适用于该请求（按 action / resourceType 等判断）。不适用则引擎跳过。 */
    boolean appliesTo(AccessRequest req);

    /** 评估：返回 DENY 拒绝、PERMIT 明确放行、NOT_APPLICABLE 不表态。 */
    Effect evaluate(AccessRequest req);

    /** DENY 时给用户/审计看的原因。 */
    default String denyReason(AccessRequest req) {
        return "策略 " + id() + " 拒绝该操作";
    }
}
