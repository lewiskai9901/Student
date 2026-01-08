package com.school.management.domain.task.event;

import com.school.management.domain.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for task domain events.
 */
public abstract class TaskDomainEvent implements DomainEvent {

    private final String eventId;
    private final Instant occurredOn;
    private final Long taskId;
    private final String taskCode;

    protected TaskDomainEvent(Long taskId, String taskCode) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = Instant.now();
        this.taskId = taskId;
        this.taskCode = taskCode;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public Instant getOccurredOn() {
        return occurredOn;
    }

    @Override
    public String getAggregateId() {
        return taskId != null ? taskId.toString() : null;
    }

    @Override
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
