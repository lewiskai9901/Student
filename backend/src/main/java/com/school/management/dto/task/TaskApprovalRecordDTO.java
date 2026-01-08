package com.school.management.dto.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务审批记录DTO
 */
@Data
public class TaskApprovalRecordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 提交记录ID
     */
    private Long submissionId;

    /**
     * 审批节点名称
     */
    private String nodeName;

    /**
     * 审批顺序
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
     * 审批状态文本
     */
    private String approvalStatusText;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 审批时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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

    /**
     * Flowable任务ID
     */
    private String flowableTaskId;
}
