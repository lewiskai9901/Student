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
}
