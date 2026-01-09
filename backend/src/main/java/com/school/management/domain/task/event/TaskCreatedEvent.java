package com.school.management.domain.task.event;

import java.util.List;

/**
 * Event published when a task is created.
 */
public class TaskCreatedEvent extends TaskDomainEvent {

    private final String title;
    private final Long assignerId;
    private final String assignerName;
    private final List<Long> targetIds;

    public TaskCreatedEvent(Long taskId, String taskCode, String title,
                            Long assignerId, String assignerName, List<Long> targetIds) {
        super(taskId, taskCode);
        this.title = title;
        this.assignerId = assignerId;
        this.assignerName = assignerName;
        this.targetIds = targetIds;
    }

    public static TaskCreatedEvent of(Long taskId, String taskCode, String title,
                                       Long assignerId, String assignerName, List<Long> targetIds) {
        return new TaskCreatedEvent(taskId, taskCode, title, assignerId, assignerName, targetIds);
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

    public List<Long> getTargetIds() {
        return targetIds;
    }
}
