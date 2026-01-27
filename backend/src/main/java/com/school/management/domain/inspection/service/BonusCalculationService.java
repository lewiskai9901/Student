package com.school.management.domain.inspection.service;

import com.school.management.domain.inspection.model.BonusItem;

import java.math.BigDecimal;

/**
 * Domain service for calculating bonus scores based on different bonus modes.
 */
public interface BonusCalculationService {

    /**
     * Calculates a fixed bonus score.
     */
    BigDecimal calculateFixedBonus(BonusItem item);

    /**
     * Calculates a progressive bonus based on consecutive weeks of achievement.
     */
    BigDecimal calculateProgressiveBonus(BonusItem item, int consecutiveWeeks);

    /**
     * Calculates an improvement bonus based on score change.
     */
    BigDecimal calculateImprovementBonus(BonusItem item, BigDecimal previousScore, BigDecimal currentScore);
}
