package com.school.management.domain.rating.event;

/**
 * Event published when a rating result is approved.
 */
public class RatingApprovedEvent extends RatingDomainEvent {

    private final Long approverId;

    private RatingApprovedEvent(Long resultId, Long ratingConfigId, Long classId, Long approverId) {
        super(resultId, ratingConfigId, classId);
        this.approverId = approverId;
    }

    public static RatingApprovedEvent of(Long resultId, Long ratingConfigId, Long classId, Long approverId) {
        return new RatingApprovedEvent(resultId, ratingConfigId, classId, approverId);
    }

    @Override
    public String getEventType() { return "rating.approved"; }

    public Long getApproverId() { return approverId; }
}
