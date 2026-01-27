package com.school.management.domain.task.event;

/**
 * Event published when a task is cancelled.
 */
public class TaskCancelledEvent extends TaskDomainEvent {

    private final String reason;
    private final Long cancelledBy;

    private TaskCancelledEvent(Long taskId, String taskCode, String reason, Long cancelledBy) {
        super(taskId, taskCode);
        this.reason = reason;
        this.cancelledBy = cancelledBy;
    }

    public static TaskCancelledEvent of(Long taskId, String taskCode, String reason, Long cancelledBy) {
        return new TaskCancelledEvent(taskId, taskCode, reason, cancelledBy);
    }

    @Override
    public String getEventType() {
        return "task.cancelled";
    }

    public String getReason() {
        return reason;
    }

    public Long getCancelledBy() {
        return cancelledBy;
    }
}
