package com.school.management.domain.task.model.entity;

import com.school.management.domain.shared.Entity;
import com.school.management.domain.task.model.valueobject.ApprovalStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 任务审批记录实体
 */
@Getter
@Setter
public class TaskApproval extends Entity<Long> {

    private Long taskId;
    private Long submissionId;
    private String processInstanceId;
    private String taskDefinitionKey;
    private String flowableTaskId;
    private String nodeName;
    private Integer nodeOrder;
    private Long approverId;
    private String approverName;
    private String approverRole;
    private ApprovalStatus approvalStatus;
    private String approvalComment;
    private LocalDateTime approvalTime;
    private String rejectToNode;
    private String rejectReason;
    private Long transferToId;
    private String transferToName;
    private String transferReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected TaskApproval() {}

    /**
     * 创建待审批记录
     */
    public static TaskApproval createPending(Long taskId, Long submissionId, String nodeName,
                                               Integer nodeOrder, Long approverId,
                                               String approverName, String approverRole) {
        TaskApproval approval = new TaskApproval();
        approval.taskId = taskId;
        approval.submissionId = submissionId;
        approval.nodeName = nodeName;
        approval.nodeOrder = nodeOrder;
        approval.approverId = approverId;
        approval.approverName = approverName;
        approval.approverRole = approverRole;
        approval.approvalStatus = ApprovalStatus.PENDING;
        approval.createdAt = LocalDateTime.now();
        approval.updatedAt = LocalDateTime.now();
        return approval;
    }

    /**
     * 从持久化重建
     */
    public static TaskApproval reconstruct(Long id, Long taskId, Long submissionId,
                                            String processInstanceId, String taskDefinitionKey,
                                            String flowableTaskId, String nodeName, Integer nodeOrder,
                                            Long approverId, String approverName, String approverRole,
                                            ApprovalStatus approvalStatus, String approvalComment,
                                            LocalDateTime approvalTime, String rejectToNode,
                                            String rejectReason, Long transferToId,
                                            String transferToName, String transferReason,
                                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        TaskApproval approval = new TaskApproval();
        approval.setId(id);
        approval.taskId = taskId;
        approval.submissionId = submissionId;
        approval.processInstanceId = processInstanceId;
        approval.taskDefinitionKey = taskDefinitionKey;
        approval.flowableTaskId = flowableTaskId;
        approval.nodeName = nodeName;
        approval.nodeOrder = nodeOrder;
        approval.approverId = approverId;
        approval.approverName = approverName;
        approval.approverRole = approverRole;
        approval.approvalStatus = approvalStatus;
        approval.approvalComment = approvalComment;
        approval.approvalTime = approvalTime;
        approval.rejectToNode = rejectToNode;
        approval.rejectReason = rejectReason;
        approval.transferToId = transferToId;
        approval.transferToName = transferToName;
        approval.transferReason = transferReason;
        approval.createdAt = createdAt;
        approval.updatedAt = updatedAt;
        return approval;
    }

    /**
     * 审批通过
     */
    public void approve(String comment) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approvalComment = comment;
        this.approvalTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 审批拒绝
     */
    public void reject(String reason, String rejectToNode) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.rejectReason = reason;
        this.rejectToNode = rejectToNode;
        this.approvalTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 转交
     */
    public void transfer(Long toUserId, String toUserName, String reason) {
        this.approvalStatus = ApprovalStatus.TRANSFERRED;
        this.transferToId = toUserId;
        this.transferToName = toUserName;
        this.transferReason = reason;
        this.approvalTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
