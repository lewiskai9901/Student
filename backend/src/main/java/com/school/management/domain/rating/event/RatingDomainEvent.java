package com.school.management.domain.rating.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * Base class for rating domain events.
 * Extends BaseDomainEvent to reuse common event functionality.
 */
public abstract class RatingDomainEvent extends BaseDomainEvent {

    private final Long resultId;
    private final Long ratingConfigId;
    private final Long classId;

    protected RatingDomainEvent(Long resultId, Long ratingConfigId, Long classId) {
        super("RatingResult", resultId);
        this.resultId = resultId;
        this.ratingConfigId = ratingConfigId;
        this.classId = classId;
    }

    public Long getResultId() {
        return resultId;
    }

    public Long getRatingConfigId() {
        return ratingConfigId;
    }

    public Long getClassId() {
        return classId;
    }
}
