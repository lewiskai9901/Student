package com.school.management.domain.access.model;

/**
 * Types of permissions in the system.
 */
public enum PermissionType {
    /**
     * Menu permission - controls visibility of menu items.
     */
    MENU("Menu", "Controls menu visibility"),

    /**
     * Operation permission - controls ability to perform actions.
     */
    OPERATION("Operation", "Controls action execution"),

    /**
     * Data permission - controls access to specific data.
     */
    DATA("Data", "Controls data access scope"),

    /**
     * API permission - controls access to API endpoints.
     */
    API("API", "Controls API access");

    private final String displayName;
    private final String description;

    PermissionType(String displayName, String description) {
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
