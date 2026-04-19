package com.school.management.infrastructure.extension.plugins.education.application.student.command;

import lombok.Data;

/**
 * Command for updating an existing cohort.
 */
@Data
public class UpdateCohortCommand {
    private String gradeName;
    private Integer standardClassSize;
    private Integer sortOrder;
    private String remarks;
    private Long updatedBy;
}
