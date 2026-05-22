package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 任务提交被撤回 — SUBMITTED → IN_PROGRESS (P2#15).
 *
 * <p>下游影响: 已提交结果被检查员收回继续修改, 审核侧待办需同步移除,
 * 统计口径里"已提交"计数应回退.
 */
public class TaskWithdrawnEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long taskId;
    private final String taskCode;
    private final Long inspectorId;

    public TaskWithdrawnEvent(Long taskId, String taskCode, Long inspectorId) {
        super("InspTask", taskId);
        this.taskId = taskId;
        this.taskCode = taskCode;
        this.inspectorId = inspectorId;
    }

    public Long getTaskId() { return taskId; }
    public String getTaskCode() { return taskCode; }
    public Long getInspectorId() { return inspectorId; }
}
