package com.school.management.application.organization.command;

import lombok.Data;

/**
 * Command for assigning a leader to a grade.
 */
@Data
public class AssignGradeLeaderCommand {
    private Long directorId;
    private String directorName;
    private Long counselorId;
    private String counselorName;
    private Long updatedBy;
}
