package com.school.management.domain.rating.model;

/**
 * Rating period type enumeration.
 */
public enum RatingPeriodType {

    /**
     * Daily rating.
     */
    DAILY("日评级"),

    /**
     * Weekly rating.
     */
    WEEKLY("周评级"),

    /**
     * Monthly rating.
     */
    MONTHLY("月评级");

    private final String displayName;

    RatingPeriodType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RatingPeriodType fromString(String value) {
        if (value == null) return DAILY;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DAILY;
        }
    }
}
