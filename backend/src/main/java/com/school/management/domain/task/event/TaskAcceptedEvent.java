package com.school.management.domain.task.event;

/**
 * Event published when a task is accepted by an assignee.
 */
public class TaskAcceptedEvent extends TaskDomainEvent {

    private final Long acceptedBy;
    private final String acceptedByName;

    private TaskAcceptedEvent(Long taskId, String taskCode, Long acceptedBy, String acceptedByName) {
        super(taskId, taskCode);
        this.acceptedBy = acceptedBy;
        this.acceptedByName = acceptedByName;
    }

    public static TaskAcceptedEvent of(Long taskId, String taskCode, Long acceptedBy, String acceptedByName) {
        return new TaskAcceptedEvent(taskId, taskCode, acceptedBy, acceptedByName);
    }

    @Override
    public String getEventType() {
        return "task.accepted";
    }

    public Long getAcceptedBy() {
        return acceptedBy;
    }

    public String getAcceptedByName() {
        return acceptedByName;
    }

    /**
     * Alias for getAcceptedBy for compatibility.
     */
    public Long getAssigneeId() {
        return acceptedBy;
    }
}
