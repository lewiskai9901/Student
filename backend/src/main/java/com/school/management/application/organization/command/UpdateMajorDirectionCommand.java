package com.school.management.application.organization.command;

import lombok.Data;

/**
 * 更新专业方向命令
 */
@Data
public class UpdateMajorDirectionCommand {
    private String directionName;
    private String level;
    private Integer years;
    private Integer isSegmented;
    private String phase1Level;
    private Integer phase1Years;
    private String phase2Level;
    private Integer phase2Years;
    private String remarks;
    private Long updatedBy;
}
