package com.school.management.application.academic.command;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 添加方案课程命令
 */
@Data
public class CreatePlanCourseCommand {
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
}
