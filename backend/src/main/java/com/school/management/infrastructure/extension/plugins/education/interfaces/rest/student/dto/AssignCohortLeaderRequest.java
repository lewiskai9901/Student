package com.school.management.infrastructure.extension.plugins.education.interfaces.rest.student.dto;

import lombok.Data;

/**
 * Request DTO for assigning leaders to a cohort.
 */
@Data
public class AssignCohortLeaderRequest {
    private Long directorId;
    private String directorName;
    private Long counselorId;
    private String counselorName;
}
