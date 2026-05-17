package com.school.management.domain.inspection.event;

/**
 * 检查平台触发点常量 (统一版本, I2 合并自两个旧 class).
 *
 * <p>业务代码通过 {@code triggerService.fire(InspectionTriggerPoints.XXX, context)} 触发,
 * 让 admin 配置的订阅规则把通知推到对应主体. 避免裸字符串.
 *
 * <p>放在 domain 层是因为 application + infrastructure 都需要引用,
 * 同时遵循 DDD 单向依赖 (domain ← application ← infrastructure).
 *
 * <p>历史:
 * <ul>
 *   <li>{@code core.constants.InspectionTriggerPoints} (3 个: ITEM/GRADE/RECORD) — 已合并删除</li>
 *   <li>{@code application.inspection.InspectionTriggerPoints} (7 个: corrective/appeal/task) — 已合并删除</li>
 * </ul>
 */
public final class InspectionTriggerPoints {
    private InspectionTriggerPoints() {}

    // ========== 提交/评分 触发点 (V97 schema 已 seed) ==========

    /** 单项检查结果(打分/判定完成) */
    public static final String INSP_ITEM_RESULT     = "INSP_ITEM_RESULT";

    /** 等级结果(按分数段评级) */
    public static final String INSP_GRADE_RESULT    = "INSP_GRADE_RESULT";

    /** 单条提交完成 */
    public static final String INSP_RECORD_COMPLETE = "INSP_RECORD_COMPLETE";

    // ========== 整改 触发点 (V20260427_4 seed) ==========

    /** 整改单创建 — 通知当事人 (subject) 有新整改要求 */
    public static final String INSP_CORRECTIVE_CREATED  = "INSP_CORRECTIVE_CREATED";

    /** 整改责任人分配 — 通知 assignee */
    public static final String INSP_CORRECTIVE_ASSIGNED = "INSP_CORRECTIVE_ASSIGNED";

    /** 整改验证通过 — 通知当事人和责任人 (review #B: 与 REJECTED 拆分避免幂等去重命中) */
    public static final String INSP_CORRECTIVE_VERIFIED = "INSP_CORRECTIVE_VERIFIED";

    /** 整改验证驳回 — 通知当事人和责任人 (review #B 拆分新增) */
    public static final String INSP_CORRECTIVE_REJECTED = "INSP_CORRECTIVE_REJECTED";

    // ========== 申诉 触发点 ==========

    /** 申诉提交 — 通知申诉人 (确认收到) + 审核员 (有待审) */
    public static final String INSP_APPEAL_SUBMITTED = "INSP_APPEAL_SUBMITTED";

    /** 申诉审核完成 (通过/驳回) — 通知申诉人结果 */
    public static final String INSP_APPEAL_REVIEWED  = "INSP_APPEAL_REVIEWED";

    // ========== 任务 触发点 ==========

    /** 任务驳回 — 通知检查员重新提交 */
    public static final String INSP_TASK_REJECTED    = "INSP_TASK_REJECTED";
}
