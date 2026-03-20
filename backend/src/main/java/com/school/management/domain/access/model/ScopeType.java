package com.school.management.domain.access.model;

/**
 * Scope type constants for scoped role assignments.
 */
public final class ScopeType {

    /** Global scope - no restriction */
    public static final String ALL = "ALL";

    /** Scoped to a specific org unit and its children */
    public static final String ORG_UNIT = "ORG_UNIT";

    private ScopeType() {}

    public static boolean isValid(String scopeType) {
        return ALL.equals(scopeType) || ORG_UNIT.equals(scopeType);
    }
}
