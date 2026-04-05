package com.school.management.interfaces.rest.student.dto;

import lombok.Data;

/**
 * Request DTO for updating a cohort.
 */
@Data
public class UpdateCohortRequest {
    private String gradeName;
    private Integer standardClassSize;
    private Integer sortOrder;
    private String remarks;
}
