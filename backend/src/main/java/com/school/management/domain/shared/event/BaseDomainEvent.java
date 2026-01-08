package com.school.management.domain.shared.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base abstract class for domain events.
 * Provides common functionality for events that need constructor-based initialization.
 */
public abstract class BaseDomainEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String aggregateType;
    private final String aggregateId;

    protected BaseDomainEvent(String aggregateType, String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
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
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    /**
     * Convenience method for backward compatibility.
     */
    public String getAggregateId() {
        return aggregateId;
    }
}
