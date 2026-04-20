package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 策略 SPI — 插件实现此接口, 通过 PolicyContribution 注入到 core.
 *
 * 实现类应:
 *   1. supports(ctx) 判断是否处理此 (entityType, phase) 组合
 *   2. check(ctx)    返回违规列表, 空 = 通过
 *
 * core 在变更边界 (checkIn / addMember / delete) 调用 PolicyRegistry.check/enforce()
 * 集中执行所有 supports=true 的策略, 按 Severity 分级处理.
 *
 * 示例:
 * <pre>
 *   &#64;Component
 *   class MinOccupantsPolicy implements Policy&lt;CheckInCommand&gt; {
 *       public String code() { return "MIN_OCCUPANTS"; }
 *       public boolean supports(PolicyContext&lt;?&gt; c) {
 *           return "place".equals(c.entityType()) &amp;&amp; "AFTER_CHECKIN".equals(c.phase());
 *       }
 *       public List&lt;Violation&gt; check(PolicyContext&lt;CheckInCommand&gt; c) { ... }
 *   }
 * </pre>
 */
public interface Policy<T> {

    /** 策略代号, 全局唯一 */
    String code();

    /** 人类可读名称 */
    default String name() { return code(); }

    /** 判断此策略是否适用于给定上下文 */
    boolean supports(PolicyContext<?> ctx);

    /** 执行检查, 返回违规, 空 = 通过 */
    List<Violation> check(PolicyContext<T> ctx);
}
