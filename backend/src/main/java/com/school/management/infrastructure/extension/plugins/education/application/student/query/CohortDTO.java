package com.school.management.application.student.query;

import com.school.management.domain.student.model.CohortStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Cohort.
 */
@Data
public class CohortDTO {
    private Long id;
    private String gradeCode;
    private String gradeName;
    private Integer enrollmentYear;
    private Integer graduationYear;
    private Integer schoolingYears;
    private Long directorId;
    private String directorName;
    private Long counselorId;
    private String counselorName;
    private Integer standardClassSize;
    private CohortStatus status;
    private String statusDisplayName;
    private Integer sortOrder;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 统计字段
    private Integer classCount;
    private Integer studentCount;
    private Double avgStudentsPerClass;
}
