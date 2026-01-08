package com.school.management.domain.organization.model;

/**
 * Organization unit type enumeration.
 * Defines the hierarchy levels in the organization structure.
 */
public enum OrgUnitType {

    /**
     * School level - the root of organization hierarchy
     */
    SCHOOL("School", 1),

    /**
     * College/Faculty level
     */
    COLLEGE("College", 2),

    /**
     * Department level
     */
    DEPARTMENT("Department", 3),

    /**
     * Teaching group level
     */
    TEACHING_GROUP("Teaching Group", 4);

    private final String displayName;
    private final int level;

    OrgUnitType(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Checks if this type can be a child of the given parent type.
     */
    public boolean canBeChildOf(OrgUnitType parentType) {
        if (parentType == null) {
            return this == SCHOOL;
        }
        return this.level == parentType.level + 1;
    }
}
