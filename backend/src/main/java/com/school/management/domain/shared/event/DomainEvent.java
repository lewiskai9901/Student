package com.school.management.domain.shared.event;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Domain event interface.
 * Domain events represent something that happened in the domain.
 */
public interface DomainEvent {

    /**
     * Returns when this event occurred.
     */
    LocalDateTime occurredOn();

    /**
     * Returns the ID of the aggregate that raised this event.
     */
    String aggregateId();

    /**
     * Returns the type name of this event.
     */
    default String eventType() {
        return this.getClass().getSimpleName();
    }

    /**
     * Returns the type name of this event (alias for eventType).
     */
    default String getEventType() {
        return eventType();
    }

    /**
     * Returns the aggregate ID (alias for aggregateId).
     */
    default String getAggregateId() {
        return aggregateId();
    }

    /**
     * Returns the aggregate type.
     */
    default String getAggregateType() {
        return "Unknown";
    }

    /**
     * Returns the unique event ID.
     * Implementations MUST override this to cache the ID in a field.
     * The default generates a new UUID each call — only safe as a fallback.
     * All events should extend BaseDomainEvent which caches the ID properly.
     */
    String getEventId();

    /**
     * Returns when this event occurred as Instant.
     */
    default Instant getOccurredAt() {
        LocalDateTime ldt = occurredOn();
        return ldt != null ? ldt.atZone(ZoneId.systemDefault()).toInstant() : Instant.now();
    }
}
