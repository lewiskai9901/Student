package com.school.management.domain.inspection.model.appeal;

import com.school.management.domain.inspection.event.AppealApprovedEvent;
import com.school.management.domain.inspection.event.AppealRejectedEvent;
import com.school.management.domain.inspection.event.AppealSubmittedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检查申诉聚合根 (P1#8).
 *
 * <p>当事人或检查员对一条 {@link com.school.management.domain.inspection.model.execution.SubmissionDetail}
 * 的扣分判定不服时, 提交申诉; 经审核通过后回填实际调整, 驳回则维持原判.
 *
 * <p>状态机: PENDING → APPROVED / REJECTED / WITHDRAWN
 */
public class InspAppeal extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private Long orgUnitId;
    private String appealCode;
    private Long submissionDetailId;
    private Long submissionId;
    private Long taskId;
    private Long projectId;
    private String subjectType;
    private Long subjectId;
    private Long submitterUserId;
    private String submitterName;
    private String reason;
    private String attachments;
    private BigDecimal expectedAdjustment;
    private BigDecimal finalAdjustment;
    private AppealStatus status;
    private Long reviewerId;
    private String reviewerName;
    private String reviewerComment;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected InspAppeal() {
    }

    private InspAppeal(Builder b) {
        this.id = b.id;
        this.tenantId = b.tenantId;
        this.orgUnitId = b.orgUnitId;
        this.appealCode = b.appealCode;
        this.submissionDetailId = b.submissionDetailId;
        this.submissionId = b.submissionId;
        this.taskId = b.taskId;
        this.projectId = b.projectId;
        this.subjectType = b.subjectType;
        this.subjectId = b.subjectId;
        this.submitterUserId = b.submitterUserId;
        this.submitterName = b.submitterName;
        this.reason = b.reason;
        this.attachments = b.attachments;
        this.expectedAdjustment = b.expectedAdjustment;
        this.finalAdjustment = b.finalAdjustment;
        this.status = b.status != null ? b.status : AppealStatus.PENDING;
        this.reviewerId = b.reviewerId;
        this.reviewerName = b.reviewerName;
        this.reviewerComment = b.reviewerComment;
        this.reviewedAt = b.reviewedAt;
        this.createdAt = b.createdAt != null ? b.createdAt : LocalDateTime.now();
        this.updatedAt = b.updatedAt;
    }

    /** 提交一个新的申诉 */
    public static InspAppeal submit(String appealCode, Long submissionDetailId,
                                     Long submissionId, Long taskId, Long projectId,
                                     String subjectType, Long subjectId,
                                     Long submitterUserId, String submitterName,
                                     String reason, String attachments,
                                     BigDecimal expectedAdjustment) {
        if (appealCode == null || appealCode.isBlank()) throw new IllegalArgumentException("申诉编号不能为空");
        if (submissionDetailId == null) throw new IllegalArgumentException("申诉对应的扣分项不能为空");
        if (submitterUserId == null) throw new IllegalArgumentException("申诉提交人不能为空");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("申诉理由不能为空");

        InspAppeal appeal = builder()
                .appealCode(appealCode)
                .submissionDetailId(submissionDetailId)
                .submissionId(submissionId)
                .taskId(taskId)
                .projectId(projectId)
                .subjectType(subjectType)
                .subjectId(subjectId)
                .submitterUserId(submitterUserId)
                .submitterName(submitterName)
                .reason(reason)
                .attachments(attachments)
                .expectedAdjustment(expectedAdjustment)
                .status(AppealStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        appeal.registerEvent(new AppealSubmittedEvent(
                null, appealCode, submissionDetailId, submitterUserId));
        return appeal;
    }

    public static InspAppeal reconstruct(Builder builder) {
        return new InspAppeal(builder);
    }

    /** 审核通过 — PENDING → APPROVED */
    public void approve(Long reviewerId, String reviewerName, String comment, BigDecimal finalAdjustment) {
        if (this.status != AppealStatus.PENDING) {
            throw new IllegalStateException("只有待审核状态的申诉才能审核通过");
        }
        this.status = AppealStatus.APPROVED;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.reviewerComment = comment;
        this.finalAdjustment = finalAdjustment;
        this.reviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        registerEvent(new AppealApprovedEvent(this.id, this.appealCode,
                this.submissionDetailId, reviewerId, finalAdjustment));
    }

    /** 审核驳回 — PENDING → REJECTED */
    public void reject(Long reviewerId, String reviewerName, String comment) {
        if (this.status != AppealStatus.PENDING) {
            throw new IllegalStateException("只有待审核状态的申诉才能驳回");
        }
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("驳回必须填写理由");
        }
        this.status = AppealStatus.REJECTED;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.reviewerComment = comment;
        this.reviewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        registerEvent(new AppealRejectedEvent(this.id, this.appealCode, reviewerId, comment));
    }

    /** 提交人撤回 — PENDING → WITHDRAWN */
    public void withdraw(Long byUserId) {
        if (this.status != AppealStatus.PENDING) {
            throw new IllegalStateException("只有待审核状态的申诉才能撤回");
        }
        if (byUserId == null || !byUserId.equals(this.submitterUserId)) {
            throw new IllegalStateException("仅申诉提交人可以撤回该申诉");
        }
        this.status = AppealStatus.WITHDRAWN;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getOrgUnitId() { return orgUnitId; }
    public String getAppealCode() { return appealCode; }
    public Long getSubmissionDetailId() { return submissionDetailId; }
    public Long getSubmissionId() { return submissionId; }
    public Long getTaskId() { return taskId; }
    public Long getProjectId() { return projectId; }
    public String getSubjectType() { return subjectType; }
    public Long getSubjectId() { return subjectId; }
    public Long getSubmitterUserId() { return submitterUserId; }
    public String getSubmitterName() { return submitterName; }
    public String getReason() { return reason; }
    public String getAttachments() { return attachments; }
    public BigDecimal getExpectedAdjustment() { return expectedAdjustment; }
    public BigDecimal getFinalAdjustment() { return finalAdjustment; }
    public AppealStatus getStatus() { return status; }
    public Long getReviewerId() { return reviewerId; }
    public String getReviewerName() { return reviewerName; }
    public String getReviewerComment() { return reviewerComment; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long orgUnitId;
        private String appealCode;
        private Long submissionDetailId;
        private Long submissionId;
        private Long taskId;
        private Long projectId;
        private String subjectType;
        private Long subjectId;
        private Long submitterUserId;
        private String submitterName;
        private String reason;
        private String attachments;
        private BigDecimal expectedAdjustment;
        private BigDecimal finalAdjustment;
        private AppealStatus status;
        private Long reviewerId;
        private String reviewerName;
        private String reviewerComment;
        private LocalDateTime reviewedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long v) { this.id = v; return this; }
        public Builder tenantId(Long v) { this.tenantId = v; return this; }
        public Builder orgUnitId(Long v) { this.orgUnitId = v; return this; }
        public Builder appealCode(String v) { this.appealCode = v; return this; }
        public Builder submissionDetailId(Long v) { this.submissionDetailId = v; return this; }
        public Builder submissionId(Long v) { this.submissionId = v; return this; }
        public Builder taskId(Long v) { this.taskId = v; return this; }
        public Builder projectId(Long v) { this.projectId = v; return this; }
        public Builder subjectType(String v) { this.subjectType = v; return this; }
        public Builder subjectId(Long v) { this.subjectId = v; return this; }
        public Builder submitterUserId(Long v) { this.submitterUserId = v; return this; }
        public Builder submitterName(String v) { this.submitterName = v; return this; }
        public Builder reason(String v) { this.reason = v; return this; }
        public Builder attachments(String v) { this.attachments = v; return this; }
        public Builder expectedAdjustment(BigDecimal v) { this.expectedAdjustment = v; return this; }
        public Builder finalAdjustment(BigDecimal v) { this.finalAdjustment = v; return this; }
        public Builder status(AppealStatus v) { this.status = v; return this; }
        public Builder reviewerId(Long v) { this.reviewerId = v; return this; }
        public Builder reviewerName(String v) { this.reviewerName = v; return this; }
        public Builder reviewerComment(String v) { this.reviewerComment = v; return this; }
        public Builder reviewedAt(LocalDateTime v) { this.reviewedAt = v; return this; }
        public Builder createdAt(LocalDateTime v) { this.createdAt = v; return this; }
        public Builder updatedAt(LocalDateTime v) { this.updatedAt = v; return this; }

        public InspAppeal build() { return new InspAppeal(this); }
    }
}
