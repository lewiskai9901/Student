package com.school.management.domain.task.model.entity;

import com.school.management.domain.shared.Entity;
import com.school.management.domain.task.model.valueobject.ApprovalStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务提交实体
 */
@Getter
@Setter
public class TaskSubmission extends Entity<Long> {

    private Long taskId;
    private Long taskAssigneeId;
    private Long submitterId;
    private String submitterName;
    private String content;
    private List<Long> attachmentIds;
    private LocalDateTime submittedAt;
    private ApprovalStatus reviewStatus;
    private Long finalReviewerId;
    private String finalReviewerName;
    private String finalReviewComment;
    private LocalDateTime finalReviewedAt;
    private Integer rejectCount;
    private String rejectToNode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 审批记录
    private List<TaskApproval> approvalRecords = new ArrayList<>();

    protected TaskSubmission() {}

    /**
     * 创建提交
     */
    public static TaskSubmission create(Long taskId, Long submitterId, String submitterName,
                                          String content, List<Long> attachmentIds) {
        TaskSubmission submission = new TaskSubmission();
        submission.taskId = taskId;
        submission.submitterId = submitterId;
        submission.submitterName = submitterName;
        submission.content = content;
        submission.attachmentIds = attachmentIds;
        submission.submittedAt = LocalDateTime.now();
        submission.reviewStatus = ApprovalStatus.PENDING;
        submission.rejectCount = 0;
        submission.createdAt = LocalDateTime.now();
        submission.updatedAt = LocalDateTime.now();
        return submission;
    }

    /**
     * 从持久化重建
     */
    public static TaskSubmission reconstruct(Long id, Long taskId, Long taskAssigneeId,
                                               Long submitterId, String submitterName,
                                               String content, List<Long> attachmentIds,
                                               LocalDateTime submittedAt, ApprovalStatus reviewStatus,
                                               Long finalReviewerId, String finalReviewerName,
                                               String finalReviewComment, LocalDateTime finalReviewedAt,
                                               Integer rejectCount, String rejectToNode,
                                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        TaskSubmission submission = new TaskSubmission();
        submission.setId(id);
        submission.taskId = taskId;
        submission.taskAssigneeId = taskAssigneeId;
        submission.submitterId = submitterId;
        submission.submitterName = submitterName;
        submission.content = content;
        submission.attachmentIds = attachmentIds;
        submission.submittedAt = submittedAt;
        submission.reviewStatus = reviewStatus;
        submission.finalReviewerId = finalReviewerId;
        submission.finalReviewerName = finalReviewerName;
        submission.finalReviewComment = finalReviewComment;
        submission.finalReviewedAt = finalReviewedAt;
        submission.rejectCount = rejectCount;
        submission.rejectToNode = rejectToNode;
        submission.createdAt = createdAt;
        submission.updatedAt = updatedAt;
        return submission;
    }

    /**
     * 审批通过
     */
    public void approve(Long reviewerId, String reviewerName, String comment) {
        this.reviewStatus = ApprovalStatus.APPROVED;
        this.finalReviewerId = reviewerId;
        this.finalReviewerName = reviewerName;
        this.finalReviewComment = comment;
        this.finalReviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 审批拒绝
     */
    public void reject(Long reviewerId, String reviewerName, String reason, String rejectToNode) {
        this.reviewStatus = ApprovalStatus.REJECTED;
        this.finalReviewerId = reviewerId;
        this.finalReviewerName = reviewerName;
        this.finalReviewComment = reason;
        this.finalReviewedAt = LocalDateTime.now();
        this.rejectCount = (this.rejectCount == null ? 0 : this.rejectCount) + 1;
        this.rejectToNode = rejectToNode;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 添加审批记录
     */
    public void addApprovalRecord(TaskApproval approval) {
        this.approvalRecords.add(approval);
    }

    /**
     * 是否待审批
     */
    public boolean isPending() {
        return reviewStatus != null && reviewStatus.isPending();
    }

    /**
     * 是否已通过
     */
    public boolean isApproved() {
        return reviewStatus != null && reviewStatus.isApproved();
    }

    /**
     * 是否被打回
     */
    public boolean isRejected() {
        return reviewStatus != null && reviewStatus.isRejected();
    }
}
