package com.school.management.domain.rating.event;

/**
 * Event published when a rating result is revoked.
 */
public class RatingRevokedEvent extends RatingDomainEvent {

    private final Long revokedBy;

    private RatingRevokedEvent(Long resultId, Long ratingConfigId, Long classId, Long revokedBy) {
        super(resultId, ratingConfigId, classId);
        this.revokedBy = revokedBy;
    }

    public static RatingRevokedEvent of(Long resultId, Long ratingConfigId, Long classId, Long revokedBy) {
        return new RatingRevokedEvent(resultId, ratingConfigId, classId, revokedBy);
    }

    @Override
    public String getEventType() { return "rating.revoked"; }

    public Long getRevokedBy() { return revokedBy; }
}
