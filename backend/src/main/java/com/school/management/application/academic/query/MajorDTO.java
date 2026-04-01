package com.school.management.application.academic.query;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专业 DTO
 */
@Data
public class MajorDTO {
    private Long id;
    private String majorCode;
    private String majorName;
    private Long orgUnitId;
    private String orgUnitName;
    private String description;
    private Integer status;
    private String statusName;
    private String majorCategoryCode;
    private String majorCategoryName;
    private String enrollmentTarget;
    private String educationForm;
    private Long leadTeacherId;
    private String leadTeacherName;
    private Integer approvalYear;
    private String majorStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
