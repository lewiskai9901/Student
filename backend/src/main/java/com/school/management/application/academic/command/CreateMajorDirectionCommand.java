package com.school.management.application.academic.command;

import lombok.Data;

import java.util.List;

/**
 * 创建专业方向命令
 */
@Data
public class CreateMajorDirectionCommand {
    private Long majorId;
    private String directionCode;
    private String directionName;
    private String level;
    private Integer years;
    private Integer isSegmented;
    private String phase1Level;
    private Integer phase1Years;
    private String phase2Level;
    private Integer phase2Years;
    private String remarks;
    private String enrollmentTarget;
    private String educationForm;
    private List<String> certificateNames;
    private String trainingStandard;
    private String cooperationEnterprise;
    private Integer maxEnrollment;
    private Integer sortOrder;
    private Long createdBy;
}
