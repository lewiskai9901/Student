package com.school.management.entity.task;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.school.management.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 流程模板实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "workflow_templates", autoResultMap = true)
public class WorkflowTemplate extends BaseEntity {

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 模板编码(唯一)
     */
    private String templateCode;

    /**
     * 模板类型: TASK-任务审批, LEAVE-请假, OTHER-其他
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
     * 表单配置(JSON格式)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> formConfig;

    /**
     * 节点配置(审批人规则等)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> nodeConfig;

    /**
     * 是否默认模板: 0-否, 1-是
     */
    private Integer isDefault;

    /**
     * 状态: 0-禁用, 1-启用
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
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建人姓名
     */
    private String createdByName;

    /**
     * 更新人ID
     */
    private Long updatedBy;
}
