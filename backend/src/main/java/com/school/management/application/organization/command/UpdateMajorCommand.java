package com.school.management.application.organization.command;

import lombok.Data;

/**
 * 更新专业命令
 */
@Data
public class UpdateMajorCommand {
    private String majorName;
    private String description;
    private Integer status;
    private Long updatedBy;
}
