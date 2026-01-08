package com.school.management.dto.task;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批配置DTO（单个审批级别）
 */
@Data
public class ApprovalConfigDTO {

    /**
     * 审批级别
     */
    @NotNull(message = "审批级别不能为空")
    private Integer level;

    /**
     * 审批人ID
     */
    @NotNull(message = "审批人ID不能为空")
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批人角色
     */
    private String approverRole;
}
