package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 任务检查员退出 / 重派 事件 (review #D, 对称 P1#6 整改单 unassign).
 */
public class InspectorUnclaimedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long taskId;
    private final String taskCode;
    private final Long previousInspectorId;
    private final String reason;

    public InspectorUnclaimedEvent(Long taskId, String taskCode,
                                    Long previousInspectorId, String reason) {
        super("InspTask", taskId != null ? taskId.toString() : null);
        this.taskId = taskId;
        this.taskCode = taskCode;
        this.previousInspectorId = previousInspectorId;
        this.reason = reason;
    }

    public Long getTaskId() { return taskId; }
    public String getTaskCode() { return taskCode; }
    public Long getPreviousInspectorId() { return previousInspectorId; }
    public String getReason() { return reason; }
}
