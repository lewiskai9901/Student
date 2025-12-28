package com.school.management.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 待审批任务DTO
 * 用于getMyPendingApprovals接口返回
 */
@Data
public class TaskApprovalDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 审批记录ID
     */
    private Long recordId;

    /**
     * 任务ID
     */
    private Long taskId;

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
     * 提交记录ID
     */
    private Long submissionId;

    /**
     * 提交人ID
     */
    private Long submitterId;

    /**
     * 提交人姓名
     */
    private String submitterName;

    /**
     * 完成情况说明
     */
    private String content;

    /**
     * 提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submittedAt;

    /**
     * 当前审批节点名称
     */
    private String nodeName;

    /**
     * 当前审批顺序
     */
    private Integer nodeOrder;

    /**
     * 审批人角色
     */
    private String approverRole;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 部门名称
     */
    private String departmentName;

    /**
     * 截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deadline;
}
