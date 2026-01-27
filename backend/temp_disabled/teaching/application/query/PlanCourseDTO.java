package com.school.management.application.teaching.query;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 培养方案课程DTO
 */
@Data
@Builder
public class PlanCourseDTO {

    private Long id;

    private Long planId;

    private Long courseId;

    private String courseCode;

    private String courseName;

    /**
     * 开课学期
     */
    private Integer semester;

    /**
     * 周学时
     */
    private Integer weeklyHours;

    /**
     * 课程学分
     */
    private BigDecimal credits;

    /**
     * 课程总学时
     */
    private Integer totalHours;

    /**
     * 课程类型
     */
    private Integer courseType;

    private String courseTypeName;

    /**
     * 考核方式
     */
    private String examType;

    /**
     * 是否必修
     */
    private Boolean isRequired;

    /**
     * 课程性质
     */
    private Integer courseNature;

    private String courseNatureName;
}
