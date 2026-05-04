package com.school.management.domain.inspection.model.execution;

/**
 * 检查任务类型 — 解耦"计划任务"硬模型, 支持随机抽查/突击/举报核查/自查/互查.
 *
 * <p>每种类型有独立的字段语义和生命周期规则:
 * <ul>
 *   <li>{@link #SCHEDULED}: 来自 plan 自动调度, deadline 必填, 逾期生效, 计入计划完成率 KPI</li>
 *   <li>{@link #AD_HOC}: 检查员临时发起, deadline 可空, 永不逾期, 反映"主动发现"</li>
 *   <li>{@link #TRIGGERED}: 申诉/告警/事件触发, 必须关联 sourceRef, 软逾期跟随源单 SLA</li>
 *   <li>{@link #SELF_CHECK}: 受检主体自评 (反向打分), inspector = 受检方本人</li>
 *   <li>{@link #COMPLAINT}: 投诉核查, 完成后回写投诉处理结果</li>
 *   <li>{@link #CROSS_AUDIT}: 检查员之间互查 (审计独立性, 不允许查自己人)</li>
 * </ul>
 */
public enum TaskType {
    SCHEDULED("计划任务", true),
    AD_HOC("临时抽查", false),
    TRIGGERED("事件触发", false),
    SELF_CHECK("自查", false),
    COMPLAINT("投诉核查", false),
    CROSS_AUDIT("互查", true);

    private final String label;
    /** 是否要求 deadline 必填 (用于逾期判定) */
    private final boolean deadlineRequired;

    TaskType(String label, boolean deadlineRequired) {
        this.label = label;
        this.deadlineRequired = deadlineRequired;
    }

    public String getLabel() { return label; }
    public boolean isDeadlineRequired() { return deadlineRequired; }

    /**
     * 是否计入计划完成率 KPI.
     * 只 SCHEDULED 计入 — 其他类型反映主动性/合规性, 单独统计.
     */
    public boolean countsTowardScheduledKpi() {
        return this == SCHEDULED;
    }

    /** 是否可以无 task_date 直接发起 */
    public boolean allowsNullTaskDate() {
        return this == AD_HOC || this == SELF_CHECK;
    }
}
