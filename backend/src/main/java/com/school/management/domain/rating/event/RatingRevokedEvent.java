package com.school.management.domain.rating.event;

/**
 * Event published when a rating result is revoked.
 */
public class RatingRevokedEvent extends RatingDomainEvent {

    private final Long revokedBy;

    private RatingRevokedEvent(Long resultId, Long ratingConfigId, Long orgUnitId, Long revokedBy) {
        super(resultId, ratingConfigId, orgUnitId);
        this.revokedBy = revokedBy;
    }

    public static RatingRevokedEvent of(Long resultId, Long ratingConfigId, Long orgUnitId, Long revokedBy) {
        return new RatingRevokedEvent(resultId, ratingConfigId, orgUnitId, revokedBy);
    }

    @Override
    public String getEventType() { return "rating.revoked"; }

    public Long getRevokedBy() { return revokedBy; }
}
