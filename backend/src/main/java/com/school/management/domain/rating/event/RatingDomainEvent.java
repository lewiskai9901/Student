package com.school.management.domain.rating.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * Base class for rating domain events.
 * Extends BaseDomainEvent to reuse common event functionality.
 */
public abstract class RatingDomainEvent extends BaseDomainEvent {

    private final Long resultId;
    private final Long ratingConfigId;
    private final Long orgUnitId;

    protected RatingDomainEvent(Long resultId, Long ratingConfigId, Long orgUnitId) {
        super("RatingResult", resultId);
        this.resultId = resultId;
        this.ratingConfigId = ratingConfigId;
        this.orgUnitId = orgUnitId;
    }

    public Long getResultId() {
        return resultId;
    }

    public Long getRatingConfigId() {
        return ratingConfigId;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }
}
