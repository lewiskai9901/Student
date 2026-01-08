package com.school.management.domain.task.event;

/**
 * Event published when a task is created.
 */
public class TaskCreatedEvent extends TaskDomainEvent {

    private final String title;
    private final Long assignerId;
    private final String assignerName;

    private TaskCreatedEvent(Long taskId, String taskCode, String title,
                             Long assignerId, String assignerName) {
        super(taskId, taskCode);
        this.title = title;
        this.assignerId = assignerId;
        this.assignerName = assignerName;
    }

    public static TaskCreatedEvent of(Long taskId, String taskCode, String title,
                                       Long assignerId, String assignerName) {
        return new TaskCreatedEvent(taskId, taskCode, title, assignerId, assignerName);
    }

    @Override
    public String getEventType() {
        return "task.created";
    }

    public String getTitle() {
        return title;
    }

    public Long getAssignerId() {
        return assignerId;
    }

    public String getAssignerName() {
        return assignerName;
    }
}
