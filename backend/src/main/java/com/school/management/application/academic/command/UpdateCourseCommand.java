package com.school.management.application.academic.command;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新课程命令
 */
@Data
public class UpdateCourseCommand {
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
    private Integer examType;
    private Long orgUnitId;
    private String description;
    private Integer status;
    private Long updatedBy;
}
