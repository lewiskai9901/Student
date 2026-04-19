package com.school.management.infrastructure.extension.plugins.core.constants;

/**
 * 通用场所触发点常量 — 任何行业部署都适用的场所级事件.
 *
 * 业务代码应引用这些 CORE 常量, 禁止引用行业特定的
 * EducationTriggerPoints.DORM_CHECKIN / DORM_CHECKOUT 等.
 *
 * 行业特定语义(如"宿舍入住")由行业插件通过 event_triggers 映射:
 *   PLACE_OCCUPIED  →  DORM_CHECKIN_EVT  (EDU 订阅)
 *   PLACE_VACATED   →  DORM_CHECKOUT_EVT (EDU 订阅)
 */
public final class CorePlaceTriggerPoints {

    /** 用户入住场所 (任何类型场所 — 宿舍/工位/教室/酒店皆适用) */
    public static final String PLACE_OCCUPIED = "PLACE_OCCUPIED";

    /** 用户退出场所 */
    public static final String PLACE_VACATED = "PLACE_VACATED";

    private CorePlaceTriggerPoints() {}
}
