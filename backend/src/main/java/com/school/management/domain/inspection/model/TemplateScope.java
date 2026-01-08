package com.school.management.domain.inspection.model;

/**
 * Defines the scope of an inspection template.
 */
public enum TemplateScope {

    /**
     * Template applies to the entire school
     */
    GLOBAL("Global"),

    /**
     * Template applies to a specific department
     */
    DEPARTMENT("Department"),

    /**
     * Template applies to a specific grade
     */
    GRADE("Grade");

    private final String displayName;

    TemplateScope(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
