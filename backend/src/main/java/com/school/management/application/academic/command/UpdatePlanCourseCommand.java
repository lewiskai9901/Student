package com.school.management.application.academic.command;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新方案课程命令
 */
@Data
public class UpdatePlanCourseCommand {
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
