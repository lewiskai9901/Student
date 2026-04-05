package com.school.management.application.student.command;

import lombok.Data;

/**
 * Command for assigning a leader to a cohort.
 */
@Data
public class AssignCohortLeaderCommand {
    private Long directorId;
    private String directorName;
    private Long counselorId;
    private String counselorName;
    private Long updatedBy;
}
