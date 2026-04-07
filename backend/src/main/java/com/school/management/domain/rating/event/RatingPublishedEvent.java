package com.school.management.domain.rating.event;

/**
 * Event published when a rating result is published.
 */
public class RatingPublishedEvent extends RatingDomainEvent {

    private final String className;
    private final boolean awarded;

    private RatingPublishedEvent(Long resultId, Long ratingConfigId, Long orgUnitId,
                                  String className, boolean awarded) {
        super(resultId, ratingConfigId, orgUnitId);
        this.className = className;
        this.awarded = awarded;
    }

    public static RatingPublishedEvent of(Long resultId, Long ratingConfigId, Long orgUnitId,
                                           String className, boolean awarded) {
        return new RatingPublishedEvent(resultId, ratingConfigId, orgUnitId, className, awarded);
    }

    @Override
    public String getEventType() { return "rating.published"; }

    public String getClassName() { return className; }
    public boolean isAwarded() { return awarded; }
}
