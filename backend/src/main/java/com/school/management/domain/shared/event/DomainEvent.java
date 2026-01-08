package com.school.management.domain.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events.
 * Domain events represent something that happened in the domain.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final Instant occurredAt;
    private final String aggregateType;
    private final String aggregateId;

    protected DomainEvent(String aggregateType, String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
    }

    /**
     * Returns the unique identifier of this event.
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Returns when this event occurred.
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * Returns the type of aggregate that raised this event.
     */
    public String getAggregateType() {
        return aggregateType;
    }

    /**
     * Returns the ID of the aggregate that raised this event.
     */
    public String getAggregateId() {
        return aggregateId;
    }

    /**
     * Returns the type name of this event.
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
