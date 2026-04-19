package com.school.management.application.academic.command;

import lombok.Data;

/**
 * 更新专业命令
 */
@Data
public class UpdateMajorCommand {
    private String majorName;
    private String description;
    private Integer status;
    private String majorCategoryCode;
    private String enrollmentTarget;
    private String educationForm;
    private Long leadTeacherId;
    private String leadTeacherName;
    private Integer approvalYear;
    private String majorStatus;
    private Long updatedBy;
}
