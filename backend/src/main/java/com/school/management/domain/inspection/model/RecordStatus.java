package com.school.management.domain.inspection.model;

/**
 * Status of an inspection record.
 */
public enum RecordStatus {

    /**
     * Record is being created/edited
     */
    DRAFT("Draft"),

    /**
     * Record submitted for review
     */
    SUBMITTED("Submitted"),

    /**
     * Record approved after review
     */
    APPROVED("Approved"),

    /**
     * Record published, scores are effective
     */
    PUBLISHED("Published"),

    /**
     * Record voided/cancelled
     */
    VOIDED("Voided");

    private final String displayName;

    RecordStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if the record is in a final state.
     */
    public boolean isFinal() {
        return this == PUBLISHED || this == VOIDED;
    }

    /**
     * Checks if the record can be edited.
     */
    public boolean isEditable() {
        return this == DRAFT;
    }
}
