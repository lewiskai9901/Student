package com.school.management.domain.task.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * Base class for task domain events.
 * Extends BaseDomainEvent to reuse common event functionality.
 */
public abstract class TaskDomainEvent extends BaseDomainEvent {

    private final Long taskId;
    private final String taskCode;

    protected TaskDomainEvent(Long taskId, String taskCode) {
        super("Task", taskId);
        this.taskId = taskId;
        this.taskCode = taskCode;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getTaskCode() {
        return taskCode;
    }
}
