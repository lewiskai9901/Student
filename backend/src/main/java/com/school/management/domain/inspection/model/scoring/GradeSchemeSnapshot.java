package com.school.management.domain.inspection.model.scoring;

/**
 * Immutable snapshot of a grade match result, suitable for embedding in other aggregates.
 */
public record GradeSchemeSnapshot(
        String displayName,
        String gradeCode,
        String gradeName,
        String color,
        String icon
) {
}
