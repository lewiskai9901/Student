package com.school.management.entity.task;

import com.baomidou.mybatisplus.annotation.TableName;
import com.school.management.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 任务审批记录实体(多级审批的每一步记录)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_approval_records")
public class TaskApprovalRecord extends BaseEntity {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 提交记录ID
     */
    private Long submissionId;

    /**
     * Flowable流程实例ID
     */
    private String processInstanceId;

    /**
     * Flowable任务定义Key
     */
    private String taskDefinitionKey;

    /**
     * Flowable任务ID
     */
    private String flowableTaskId;

    /**
     * 审批节点名称
     */
    private String nodeName;

    /**
     * 审批顺序(第几级)
     */
    private Integer nodeOrder;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批人角色
     */
    private String approverRole;

    /**
     * 审批状态: 0-待审批, 1-通过, 2-打回, 3-转交
     */
    private Integer approvalStatus;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;

    /**
     * 打回到的节点
     */
    private String rejectToNode;

    /**
     * 打回原因
     */
    private String rejectReason;

    /**
     * 转交给谁
     */
    private Long transferToId;

    /**
     * 转交人姓名
     */
    private String transferToName;

    /**
     * 转交原因
     */
    private String transferReason;
}
