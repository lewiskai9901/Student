package com.school.management.domain.inspection.model;

/**
 * Source of how a deduction was created.
 */
public enum InputSource {
    /** Auto-generated from a checklist FAIL result */
    CHECKLIST_FAIL,
    /** Manually entered free-form deduction */
    FREE_DEDUCTION,
    /** Resolved from a physical space (dormitory/classroom) inspection */
    SPACE_RESOLVED,
    /** Resolved from a person-based inspection */
    PERSON_RESOLVED
}
