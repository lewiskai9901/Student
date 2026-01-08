package com.school.management.domain.task.event;

import com.school.management.domain.shared.event.DomainEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for task domain events.
 */
public abstract class TaskDomainEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final Long taskId;
    private final String taskCode;

    protected TaskDomainEvent(Long taskId, String taskCode) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.taskId = taskId;
        this.taskCode = taskCode;
    }

    public String getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public String aggregateId() {
        return taskId != null ? taskId.toString() : null;
    }

    public String getAggregateType() {
        return "Task";
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getTaskCode() {
        return taskCode;
    }
}
