package com.school.management.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务DTO
 */
@Data
public class TaskDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 任务编号
     */
    private String taskCode;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 优先级: 1-紧急, 2-普通, 3-低
     */
    private Integer priority;

    /**
     * 优先级文本
     */
    private String priorityText;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 分配人ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long assignerId;

    /**
     * 分配人姓名
     */
    private String assignerName;

    /**
     * 分配类型
     */
    private Integer assignType;

    /**
     * 执行人ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long assigneeId;

    /**
     * 执行人姓名
     */
    private String assigneeName;

    /**
     * 组织单元ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orgUnitId;

    /**
     * 组织单元名称
     */
    private String orgUnitName;

    /**
     * 目标执行人列表
     */
    private List<TaskAssigneeDTO> assignees;

    /**
     * 总执行人数（批量任务）
     */
    private Integer totalAssignees;

    /**
     * 已提交人数（批量任务）
     */
    private Integer submittedAssignees;

    /**
     * 已完成人数（批量任务）
     */
    private Integer completedAssignees;

    /**
     * 我的执行记录ID（仅在"我的任务"中返回）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long myAssigneeId;

    /**
     * 我的任务状态（仅在"我的任务"中返回）
     */
    private Integer myStatus;

    /**
     * 我的接收时间（仅在"我的任务"中返回）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime myAcceptedAt;

    /**
     * 我的提交时间（仅在"我的任务"中返回）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime mySubmittedAt;

    /**
     * 截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueDate;

    /**
     * 是否超期
     */
    private Boolean overdue;

    /**
     * 接收时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acceptedAt;

    /**
     * 提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submittedAt;

    /**
     * 完成时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    /**
     * 流程模板ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long workflowTemplateId;

    /**
     * 流程模板名称
     */
    private String workflowTemplateName;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 当前审批节点
     */
    private String currentNode;

    /**
     * 当前待审批人
     */
    private List<Long> currentApprovers;

    /**
     * 附件列表
     */
    private List<Long> attachmentIds;

    /**
     * 提交记录
     */
    private TaskSubmissionDTO submission;

    /**
     * 审批记录
     */
    private List<TaskApprovalRecordDTO> approvalRecords;

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
