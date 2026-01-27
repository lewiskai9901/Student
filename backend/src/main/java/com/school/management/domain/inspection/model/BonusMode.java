package com.school.management.domain.inspection.model;

/**
 * Bonus scoring mode for bonus items.
 */
public enum BonusMode {
    /** Fixed bonus score */
    FIXED,
    /** Progressive bonus based on consecutive weeks */
    PROGRESSIVE,
    /** Improvement-based bonus comparing previous and current scores */
    IMPROVEMENT
}
