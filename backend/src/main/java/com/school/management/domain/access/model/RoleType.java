package com.school.management.domain.access.model;

/**
 * Types of roles in the system.
 */
public enum RoleType {
    /**
     * Super administrator with full system access.
     */
    SUPER_ADMIN("Super Admin", 1),

    /**
     * System administrator for system management.
     */
    SYSTEM_ADMIN("System Admin", 10),

    /**
     * Department administrator managing a department.
     */
    DEPT_ADMIN("Department Admin", 20),

    /**
     * Grade director managing a grade level.
     */
    GRADE_DIRECTOR("Grade Director", 30),

    /**
     * Class teacher managing a class.
     */
    CLASS_TEACHER("Class Teacher", 40),

    /**
     * Inspector performing inspections.
     */
    INSPECTOR("Inspector", 50),

    /**
     * Regular user with basic access.
     */
    USER("User", 100),

    /**
     * Custom role defined by administrators.
     */
    CUSTOM("Custom", 90);

    private final String displayName;
    private final int defaultLevel;

    RoleType(String displayName, int defaultLevel) {
        this.displayName = displayName;
        this.defaultLevel = defaultLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultLevel() {
        return defaultLevel;
    }
}
