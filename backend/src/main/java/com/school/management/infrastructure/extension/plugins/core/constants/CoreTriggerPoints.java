package com.school.management.infrastructure.extension.plugins.core.constants;

/**
 * 通用核心触发点常量 — 任何行业都会用到.
 *
 * 业务代码通过 {@code triggerService.fire(CoreTriggerPoints.XXX, ...)} 引用,
 * 避免裸字符串.
 */
public final class CoreTriggerPoints {
    private CoreTriggerPoints() {}

    /** 组织单元创建 */
    public static final String ORG_UNIT_CREATED = "ORG_UNIT_CREATED";

    /**
     * Policy SPI 产生的 WARN/INFO 违规. 由 PolicyRegistry → PolicyWarningEvent →
     * PolicyWarningToNotificationListener 触发 (M4.3).
     * Seeded in V20260425_2__policy_warning_trigger_point.sql.
     */
    public static final String POLICY_WARNING = "POLICY_WARNING";
}
