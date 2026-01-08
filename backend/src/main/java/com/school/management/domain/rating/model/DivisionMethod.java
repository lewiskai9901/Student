package com.school.management.domain.rating.model;

/**
 * Division method for rating.
 *
 * <p>Defines how classes are divided into rating categories.
 */
public enum DivisionMethod {

    /**
     * Top N classes by score.
     */
    TOP_N("前N名"),

    /**
     * Top N percent of classes.
     */
    TOP_PERCENT("前N%"),

    /**
     * Bottom N classes by score.
     */
    BOTTOM_N("后N名"),

    /**
     * Bottom N percent of classes.
     */
    BOTTOM_PERCENT("后N%");

    private final String displayName;

    DivisionMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DivisionMethod fromString(String value) {
        if (value == null) return TOP_N;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TOP_N;
        }
    }

    /**
     * Checks if this method uses percentage.
     *
     * @return true if percentage-based
     */
    public boolean isPercentBased() {
        return this == TOP_PERCENT || this == BOTTOM_PERCENT;
    }

    /**
     * Checks if this method selects from top.
     *
     * @return true if top-based
     */
    public boolean isTopBased() {
        return this == TOP_N || this == TOP_PERCENT;
    }
}
