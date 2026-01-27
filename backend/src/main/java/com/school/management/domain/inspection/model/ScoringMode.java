package com.school.management.domain.inspection.model;

/**
 * Scoring mode for an inspection session.
 */
public enum ScoringMode {
    /** Only deductions from a perfect base score */
    DEDUCTION_ONLY,
    /** Explicit base score assignment */
    BASE_SCORE,
    /** Both base score and deduction tracking */
    DUAL_TRACK
}
