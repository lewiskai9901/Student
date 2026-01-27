package com.school.management.dto.task;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务创建请求DTO
 */
@Data
public class TaskCreateRequest {

    /**
     * 任务标题
     */
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 200, message = "任务标题不能超过200字符")
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 优先级: 1-紧急, 2-普通, 3-低
     */
    private Integer priority = 2;

    /**
     * 分配类型: 1-指定个人, 2-批量分配
     */
    @NotNull(message = "分配类型不能为空")
    private Integer assignType;

    /**
     * 执行人ID(单人任务时必填)
     */
    private Long assigneeId;

    /**
     * 目标ID列表(批量分配时必填)
     */
    private List<Long> targetIds;

    /**
     * 组织单元ID(批量分配时可选)
     */
    private Long orgUnitId;

    /**
     * 截止时间
     */
    private LocalDateTime dueDate;

    /**
     * 流程模板ID
     */
    @NotNull(message = "请选择审批流程")
    private Long workflowTemplateId;

    /**
     * 审批配置（按系部）
     */
    @Valid
    @NotEmpty(message = "审批配置不能为空")
    private List<OrgUnitApprovalConfigDTO> approvalConfigs;

    /**
     * 任务附件ID列表
     */
    private List<Long> attachmentIds;
}
