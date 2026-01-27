package com.school.management.domain.inspection.model;

/**
 * Inspection level indicating the scope of the inspection session.
 */
public enum InspectionLevel {
    /** Class-level inspection (default) */
    CLASS,
    /** Department-level inspection */
    DEPARTMENT,
    /** Special inspection (ad-hoc) */
    SPECIAL
}
