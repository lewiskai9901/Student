package com.school.management.domain.inspection.model;

/**
 * Input mode for inspection data entry.
 * Determines the primary navigation dimension during inspection.
 */
public enum InputMode {
    /** Navigate by physical space (dormitory/classroom) first */
    SPACE_FIRST,
    /** Navigate by person (student) first */
    PERSON_FIRST,
    /** Navigate by organization (class/department) first */
    ORG_FIRST
}
