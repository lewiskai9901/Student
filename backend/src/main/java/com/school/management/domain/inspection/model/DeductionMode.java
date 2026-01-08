package com.school.management.domain.inspection.model;

/**
 * Deduction calculation mode for inspection items.
 */
public enum DeductionMode {

    /**
     * Fixed deduction - a fixed amount is deducted if the item is triggered
     */
    FIXED_DEDUCT("Fixed Deduction"),

    /**
     * Per-person deduction - deduction is multiplied by the number of people involved
     */
    PER_PERSON_DEDUCT("Per-Person Deduction"),

    /**
     * Score range - deduction is selected from a predefined range
     */
    SCORE_RANGE("Score Range");

    private final String displayName;

    DeductionMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
