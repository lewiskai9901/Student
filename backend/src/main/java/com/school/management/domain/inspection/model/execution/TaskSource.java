package com.school.management.domain.inspection.model.execution;

/**
 * 检查任务来源溯源 — value object.
 *
 * <p>记录该任务由谁/为什么/基于哪个源单据创建, 用于:
 * <ul>
 *   <li>审计追溯 (从源单 → 任务)</li>
 *   <li>触发链路展示 (申诉触发了几个核查任务)</li>
 *   <li>责任追究 (突击检查的发起人)</li>
 * </ul>
 *
 * @param sourceType  SCHEDULER (调度生成) / MANUAL (手动) / EVENT (事件触发) / IMPORT (批量导入)
 * @param actorId     触发用户 ID (调度任务为 null)
 * @param reason      发起原因/说明 (突击/举报必填)
 * @param refType     来源单据类型 (Appeal/Alert/Complaint/...)
 * @param refId       来源单据 ID
 */
public record TaskSource(
        String sourceType,
        Long actorId,
        String reason,
        String refType,
        Long refId
) {

    public static TaskSource scheduler() {
        return new TaskSource("SCHEDULER", null, null, null, null);
    }

    public static TaskSource manual(Long actorId, String reason) {
        return new TaskSource("MANUAL", actorId, reason, null, null);
    }

    public static TaskSource event(String refType, Long refId, String reason) {
        return new TaskSource("EVENT", null, reason, refType, refId);
    }

    public boolean isScheduler() { return "SCHEDULER".equals(sourceType); }
    public boolean isManual() { return "MANUAL".equals(sourceType); }
    public boolean isEvent() { return "EVENT".equals(sourceType); }
}
