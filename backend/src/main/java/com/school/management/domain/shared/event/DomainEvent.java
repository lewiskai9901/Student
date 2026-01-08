package com.school.management.domain.shared.event;

import java.time.LocalDateTime;

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
}
