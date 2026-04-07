package com.school.management.domain.rating.event;

/**
 * Event published when a rating result is rejected.
 */
public class RatingRejectedEvent extends RatingDomainEvent {

    private final Long reviewerId;
    private final String reason;

    private RatingRejectedEvent(Long resultId, Long ratingConfigId, Long orgUnitId,
                                 Long reviewerId, String reason) {
        super(resultId, ratingConfigId, orgUnitId);
        this.reviewerId = reviewerId;
        this.reason = reason;
    }

    public static RatingRejectedEvent of(Long resultId, Long ratingConfigId, Long orgUnitId,
                                          Long reviewerId, String reason) {
        return new RatingRejectedEvent(resultId, ratingConfigId, orgUnitId, reviewerId, reason);
    }

    @Override
    public String getEventType() { return "rating.rejected"; }

    public Long getReviewerId() { return reviewerId; }
    public String getReason() { return reason; }
}
