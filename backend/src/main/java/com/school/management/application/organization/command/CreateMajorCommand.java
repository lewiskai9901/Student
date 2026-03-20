package com.school.management.application.organization.command;

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
    private Long createdBy;
}
