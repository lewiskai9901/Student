package com.school.management.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 流程模板DTO
 */
@Data
public class WorkflowTemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板编码
     */
    private String templateCode;

    /**
     * 模板类型
     */
    private String templateType;

    /**
     * 模板描述
     */
    private String description;

    /**
     * Flowable流程定义ID
     */
    private String processDefinitionId;

    /**
     * Flowable流程定义Key
     */
    private String processDefinitionKey;

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
    private Integer isDefault;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建人姓名
     */
    private String createdByName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
