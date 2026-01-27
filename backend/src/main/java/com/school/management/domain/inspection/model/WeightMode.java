package com.school.management.domain.inspection.model;

/**
 * Weight calculation mode for inspection scoring.
 */
public enum WeightMode {

    /** Standard weighted average: final = sum(categoryScore * weight) / totalWeight */
    STANDARD_WEIGHTED_AVERAGE("Standard Weighted Average"),

    /** Category weight: each category contributes independently via score * (weight / 100) */
    CATEGORY_WEIGHT("Category Weight");

    private final String displayName;

    WeightMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
