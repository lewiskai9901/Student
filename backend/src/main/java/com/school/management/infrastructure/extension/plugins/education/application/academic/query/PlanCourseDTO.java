package com.school.management.application.academic.query;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 方案课程 DTO
 */
@Data
public class PlanCourseDTO {
    private Long id;
    private Long planId;
    private Long courseId;
    private Integer semesterNumber;
    private Integer courseCategory;
    private Integer courseType;
    private BigDecimal credits;
    private Integer totalHours;
    private Integer weeklyHours;
    private Integer theoryHours;
    private Integer practiceHours;
    private Integer assessmentMethod;
    private Integer sortOrder;
    private String remark;
    /** JOIN 字段 */
    private String courseCode;
    /** JOIN 字段 */
    private String courseName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
