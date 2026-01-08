package com.school.management.domain.rating.event;

/**
 * Event published when a rating result is submitted for approval.
 */
public class RatingSubmittedEvent extends RatingDomainEvent {

    private RatingSubmittedEvent(Long resultId, Long ratingConfigId, Long classId) {
        super(resultId, ratingConfigId, classId);
    }

    public static RatingSubmittedEvent of(Long resultId, Long ratingConfigId, Long classId) {
        return new RatingSubmittedEvent(resultId, ratingConfigId, classId);
    }

    @Override
    public String getEventType() { return "rating.submitted"; }
}
