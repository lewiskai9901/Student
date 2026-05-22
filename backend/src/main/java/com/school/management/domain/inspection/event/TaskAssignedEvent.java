package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 任务被指派检查员 — PENDING 状态下管理员分配 (P2#15).
 *
 * <p>区别于 {@link TaskClaimedEvent} (检查员主动领取): assign 是管理员推派,
 * 下游可据此给被指派人发"你有新任务"通知.
 */
public class TaskAssignedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long taskId;
    private final String taskCode;
    private final Long inspectorId;
    private final String inspectorName;

    public TaskAssignedEvent(Long taskId, String taskCode, Long inspectorId, String inspectorName) {
        super("InspTask", taskId);
        this.taskId = taskId;
        this.taskCode = taskCode;
        this.inspectorId = inspectorId;
        this.inspectorName = inspectorName;
    }

    public Long getTaskId() { return taskId; }
    public String getTaskCode() { return taskCode; }
    public Long getInspectorId() { return inspectorId; }
    public String getInspectorName() { return inspectorName; }
}
