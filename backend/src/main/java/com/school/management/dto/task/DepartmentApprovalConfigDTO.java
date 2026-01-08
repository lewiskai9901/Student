package com.school.management.dto.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 部门审批配置DTO（一个系部的所有审批级别）
 */
@Data
public class DepartmentApprovalConfigDTO {

    /**
     * 系部ID
     */
    @NotNull(message = "系部ID不能为空")
    private Long departmentId;

    /**
     * 系部名称
     */
    private String departmentName;

    /**
     * 审批级别列表
     */
    @Valid
    @NotEmpty(message = "审批级别列表不能为空")
    private List<ApprovalConfigDTO> levels;
}
