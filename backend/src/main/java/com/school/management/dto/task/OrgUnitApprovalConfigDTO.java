package com.school.management.dto.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 组织单元审批配置DTO（一个组织单元的所有审批级别）
 */
@Data
public class OrgUnitApprovalConfigDTO {

    /**
     * 组织单元ID
     */
    @NotNull(message = "组织单元ID不能为空")
    private Long orgUnitId;

    /**
     * 组织单元名称
     */
    private String orgUnitName;

    /**
     * 审批级别列表
     */
    @Valid
    @NotEmpty(message = "审批级别列表不能为空")
    private List<ApprovalConfigDTO> levels;
}
