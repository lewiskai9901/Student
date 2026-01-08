package com.school.management.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 流程模板创建/更新请求DTO
 */
@Data
public class WorkflowTemplateRequest {

    /**
     * ID(更新时必填)
     */
    private Long id;

    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称不能超过100字符")
    private String templateName;

    /**
     * 模板编码
     */
    @NotBlank(message = "模板编码不能为空")
    @Size(max = 50, message = "模板编码不能超过50字符")
    private String templateCode;

    /**
     * 模板类型: TASK-任务审批, LEAVE-请假, OTHER-其他
     */
    private String templateType = "TASK";

    /**
     * 模板描述
     */
    private String description;

    /**
     * BPMN流程定义XML
     */
    private String bpmnXml;

    /**
     * 表单配置
     */
    private Map<String, Object> formConfig;

    /**
     * 节点配置
     */
    private Map<String, Object> nodeConfig;

    /**
     * 是否默认模板
     */
    private Integer isDefault = 0;

    /**
     * 状态
     */
    private Integer status = 1;

    /**
     * 排序
     */
    private Integer sortOrder = 0;
}
