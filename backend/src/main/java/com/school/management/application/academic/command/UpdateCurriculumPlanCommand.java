package com.school.management.application.academic.command;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新培养方案命令
 */
@Data
public class UpdateCurriculumPlanCommand {
    private String planName;
    private Long majorId;
    private Long majorDirectionId;
    private Integer gradeYear;
    private BigDecimal totalCredits;
    private BigDecimal requiredCredits;
    private BigDecimal electiveCredits;
    private BigDecimal practiceCredits;
    private String trainingObjective;
    private String graduationRequirement;
    private Long updatedBy;
}
