package com.school.management.domain.rating.event;

import java.math.BigDecimal;

/**
 * Event published when a rating is calculated.
 */
public class RatingCalculatedEvent extends RatingDomainEvent {

    private final String className;
    private final Integer ranking;
    private final BigDecimal finalScore;
    private final boolean awarded;

    private RatingCalculatedEvent(Long resultId, Long ratingConfigId, Long classId,
                                   String className, Integer ranking,
                                   BigDecimal finalScore, boolean awarded) {
        super(resultId, ratingConfigId, classId);
        this.className = className;
        this.ranking = ranking;
        this.finalScore = finalScore;
        this.awarded = awarded;
    }

    public static RatingCalculatedEvent of(Long resultId, Long ratingConfigId, Long classId,
                                            String className, Integer ranking,
                                            BigDecimal finalScore, boolean awarded) {
        return new RatingCalculatedEvent(resultId, ratingConfigId, classId,
            className, ranking, finalScore, awarded);
    }

    @Override
    public String getEventType() { return "rating.calculated"; }

    public String getClassName() { return className; }
    public Integer getRanking() { return ranking; }
    public BigDecimal getFinalScore() { return finalScore; }
    public boolean isAwarded() { return awarded; }
}
