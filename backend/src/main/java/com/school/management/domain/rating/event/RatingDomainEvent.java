package com.school.management.domain.rating.event;

import com.school.management.domain.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for rating domain events.
 */
public abstract class RatingDomainEvent implements DomainEvent {

    private final String eventId;
    private final Instant occurredOn;
    private final Long resultId;
    private final Long ratingConfigId;
    private final Long classId;

    protected RatingDomainEvent(Long resultId, Long ratingConfigId, Long classId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = Instant.now();
        this.resultId = resultId;
        this.ratingConfigId = ratingConfigId;
        this.classId = classId;
    }

    @Override
    public String getEventId() { return eventId; }

    @Override
    public Instant getOccurredOn() { return occurredOn; }

    @Override
    public String getAggregateId() { return resultId != null ? resultId.toString() : null; }

    @Override
    public String getAggregateType() { return "RatingResult"; }

    public Long getResultId() { return resultId; }
    public Long getRatingConfigId() { return ratingConfigId; }
    public Long getClassId() { return classId; }
}
