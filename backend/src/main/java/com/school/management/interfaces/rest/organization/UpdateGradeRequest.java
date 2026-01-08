package com.school.management.interfaces.rest.organization;

import lombok.Data;

/**
 * Request DTO for updating a grade.
 */
@Data
public class UpdateGradeRequest {
    private String gradeName;
    private Integer standardClassSize;
    private Integer sortOrder;
    private String remarks;
}
