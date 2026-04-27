package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

import java.time.LocalDate;

/**
 * 任务驳回事件 (P1#5).
 *
 * <p>当审核人驳回任务时发出, 携带驳回次数和自动延期后的有效日期,
 * 以便通知系统提醒检查员重新提交并感知延期.
 */
public class TaskRejectedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long taskId;
    private final String taskCode;
    private final Long inspectorId;
    private final int rejectionCount;
    private final LocalDate extendedTo;
    private final String comment;

    public TaskRejectedEvent(Long taskId, String taskCode, Long inspectorId,
                             int rejectionCount, LocalDate extendedTo, String comment) {
        super("InspTask", taskId != null ? taskId.toString() : null);
        this.taskId = taskId;
        this.taskCode = taskCode;
        this.inspectorId = inspectorId;
        this.rejectionCount = rejectionCount;
        this.extendedTo = extendedTo;
        this.comment = comment;
    }

    public Long getTaskId() { return taskId; }
    public String getTaskCode() { return taskCode; }
    public Long getInspectorId() { return inspectorId; }
    public int getRejectionCount() { return rejectionCount; }
    public LocalDate getExtendedTo() { return extendedTo; }
    public String getComment() { return comment; }
}
