package com.school.management.domain.inspection.model;

/**
 * Status of an inspection template.
 */
public enum TemplateStatus {

    /**
     * Template is being created/edited
     */
    DRAFT("Draft"),

    /**
     * Template is published and in use
     */
    PUBLISHED("Published"),

    /**
     * Template is archived and no longer active
     */
    ARCHIVED("Archived");

    private final String displayName;

    TemplateStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
