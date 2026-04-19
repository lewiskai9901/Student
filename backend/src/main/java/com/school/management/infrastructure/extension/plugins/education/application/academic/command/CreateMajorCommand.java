package com.school.management.application.academic.command;

import lombok.Data;

/**
 * 创建专业命令
 */
@Data
public class CreateMajorCommand {
    private String majorCode;
    private String majorName;
    private Long orgUnitId;
    private String description;
    private String majorCategoryCode;
    private String enrollmentTarget;
    private String educationForm;
    private Long leadTeacherId;
    private String leadTeacherName;
    private Integer approvalYear;
    private String majorStatus;
    private Long createdBy;
}
