package com.school.management.domain.task.event;

/**
 * Event published when a task is assigned to a user.
 */
public class TaskAssignedEvent extends TaskDomainEvent {

    private final Long assigneeId;
    private final String assigneeName;

    private TaskAssignedEvent(Long taskId, String taskCode, Long assigneeId, String assigneeName) {
        super(taskId, taskCode);
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
    }

    public static TaskAssignedEvent of(Long taskId, String taskCode, Long assigneeId, String assigneeName) {
        return new TaskAssignedEvent(taskId, taskCode, assigneeId, assigneeName);
    }

    @Override
    public String getEventType() {
        return "task.assigned";
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }
}
