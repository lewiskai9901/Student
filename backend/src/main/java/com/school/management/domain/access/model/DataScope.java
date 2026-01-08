package com.school.management.domain.access.model;

/**
 * Data scope levels for data permission control.
 */
public enum DataScope {
    /**
     * Access to all data in the system.
     */
    ALL("All Data", "Access to all data"),

    /**
     * Access to data within the user's department and sub-departments.
     */
    DEPARTMENT_AND_BELOW("Department & Below", "Access to department and subordinate data"),

    /**
     * Access to data within the user's department only.
     */
    DEPARTMENT("Department Only", "Access to own department data"),

    /**
     * Access to data within custom-defined scope.
     */
    CUSTOM("Custom", "Access to custom-defined data scope"),

    /**
     * Access to only self-created data.
     */
    SELF("Self Only", "Access to self-created data only");

    private final String displayName;
    private final String description;

    DataScope(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
