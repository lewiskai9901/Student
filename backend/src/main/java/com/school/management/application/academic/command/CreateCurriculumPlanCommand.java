package com.school.management.application.academic.command;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建培养方案命令
 */
@Data
public class CreateCurriculumPlanCommand {
    private String planCode;
    private String planName;
    private Long majorId;
    private Integer gradeYear;
    private BigDecimal totalCredits;
    private BigDecimal requiredCredits;
    private BigDecimal electiveCredits;
    private BigDecimal practiceCredits;
    private String trainingObjective;
    private String graduationRequirement;
    private Integer status;
    private Long createdBy;
}
