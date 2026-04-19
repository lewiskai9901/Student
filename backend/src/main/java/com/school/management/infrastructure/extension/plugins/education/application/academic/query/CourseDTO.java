package com.school.management.application.academic.query;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程 DTO
 */
@Data
public class CourseDTO {
    private Long id;
    private String courseCode;
    private String courseName;
    private String courseNameEn;
    private Integer courseCategory;
    private Integer courseType;
    private Integer courseNature;
    private BigDecimal credits;
    private Integer totalHours;
    private Integer theoryHours;
    private Integer practiceHours;
    private Integer weeklyHours;
    private Integer assessmentMethod;
    private Long orgUnitId;
    private String description;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
