package com.school.management.application.student.command;

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
