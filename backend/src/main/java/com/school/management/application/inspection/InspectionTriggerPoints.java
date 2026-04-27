package com.school.management.application.inspection;

/**
 * 检查平台触发点常量 (P1#9 — 当事人通知触发点).
 *
 * <p>业务代码通过 {@code triggerService.fire(InspectionTriggerPoints.XXX, context)} 触发,
 * 让 admin 配置的订阅规则把通知推到对应主体. 避免裸字符串.
 *
 * <p>已有的旧触发点 INSP_ITEM_RESULT / INSP_GRADE_RESULT / INSP_RECORD_COMPLETE
 * 已在 V97 schema 中 seed, 仍以裸字符串引用 (InspSubmissionApplicationService); 后续可迁移到此处.
 *
 * <p>新增 (P1#9): 整改/申诉/任务驳回 关键时刻通知触发点.
 * 对应 seed: V20260427_4__inspection_notification_trigger_points.sql
 */
public final class InspectionTriggerPoints {
    private InspectionTriggerPoints() {}

    /** 整改单创建 — 通知当事人 (subject) 有新整改要求 */
    public static final String INSP_CORRECTIVE_CREATED = "INSP_CORRECTIVE_CREATED";

    /** 整改责任人分配 — 通知 assignee */
    public static final String INSP_CORRECTIVE_ASSIGNED = "INSP_CORRECTIVE_ASSIGNED";

    /** 整改验证通过 — 通知当事人和责任人 (review #B: 与 REJECTED 拆分避免幂等去重命中) */
    public static final String INSP_CORRECTIVE_VERIFIED = "INSP_CORRECTIVE_VERIFIED";

    /** 整改验证驳回 — 通知当事人和责任人 (review #B 拆分新增) */
    public static final String INSP_CORRECTIVE_REJECTED = "INSP_CORRECTIVE_REJECTED";

    /** 申诉提交 — 通知申诉人 (确认收到) + 审核员 (有待审) */
    public static final String INSP_APPEAL_SUBMITTED = "INSP_APPEAL_SUBMITTED";

    /** 申诉审核完成 (通过/驳回) — 通知申诉人结果 */
    public static final String INSP_APPEAL_REVIEWED = "INSP_APPEAL_REVIEWED";

    /** 任务驳回 — 通知检查员重新提交 */
    public static final String INSP_TASK_REJECTED = "INSP_TASK_REJECTED";
}
