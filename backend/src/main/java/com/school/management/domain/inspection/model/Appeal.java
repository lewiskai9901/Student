package com.school.management.domain.inspection.model;

import com.school.management.domain.inspection.event.AppealStatusChangedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Appeal Aggregate Root.
 * Represents an appeal against an inspection deduction with state machine workflow.
 */
public class Appeal extends AggregateRoot<Long> {

    private Long id;
    private Long inspectionRecordId;
    private Long deductionDetailId;
    private Long classId;
    private String appealCode;
    private String reason;
    private List<String> attachments;
    private BigDecimal originalDeduction;
    private BigDecimal requestedDeduction;
    private BigDecimal approvedDeduction;
    private AppealStatus status;
    private Long applicantId;
    private LocalDateTime appliedAt;
    private Long level1ReviewerId;
    private LocalDateTime level1ReviewedAt;
    private String level1Comment;
    private Long level2ReviewerId;
    private LocalDateTime level2ReviewedAt;
    private String level2Comment;
    private LocalDateTime effectiveAt;

    private List<AppealApproval> approvalRecords;

    // For JPA/MyBatis
    protected Appeal() {
        this.approvalRecords = new ArrayList<>();
        this.attachments = new ArrayList<>();
    }

    private Appeal(Builder builder) {
        this.id = builder.id;
        this.inspectionRecordId = builder.inspectionRecordId;
        this.deductionDetailId = builder.deductionDetailId;
        this.classId = builder.classId;
        this.appealCode = builder.appealCode;
        this.reason = builder.reason;
        this.attachments = builder.attachments != null
            ? new ArrayList<>(builder.attachments)
            : new ArrayList<>();
        this.originalDeduction = builder.originalDeduction;
        this.requestedDeduction = builder.requestedDeduction;
        this.status = AppealStatus.PENDING;
        this.applicantId = builder.applicantId;
        this.appliedAt = LocalDateTime.now();
        this.approvalRecords = new ArrayList<>();

        validate();
    }

    /**
     * Factory method to create a new appeal.
     */
    public static Appeal create(Long inspectionRecordId, Long deductionDetailId,
                                Long classId, String appealCode, String reason,
                                List<String> attachments, BigDecimal originalDeduction,
                                BigDecimal requestedDeduction, Long applicantId) {
        return builder()
            .inspectionRecordId(inspectionRecordId)
            .deductionDetailId(deductionDetailId)
            .classId(classId)
            .appealCode(appealCode)
            .reason(reason)
            .attachments(attachments)
            .originalDeduction(originalDeduction)
            .requestedDeduction(requestedDeduction)
            .applicantId(applicantId)
            .build();
    }

    /**
     * Starts Level 1 review.
     */
    public void startLevel1Review(Long reviewerId) {
        assertCanTransitionTo(AppealStatus.LEVEL1_REVIEWING);
        AppealStatus oldStatus = this.status;
        this.status = AppealStatus.LEVEL1_REVIEWING;
        this.level1ReviewerId = reviewerId;

        addApprovalRecord(reviewerId, "LEVEL1", "START_REVIEW", null);
        registerEvent(new AppealStatusChangedEvent(this, oldStatus, this.status, reviewerId));
    }

    /**
     * Level 1 approves the appeal.
     */
    public void level1Approve(Long reviewerId, String comment) {
        assertCanTransitionTo(AppealStatus.LEVEL1_APPROVED);
        AppealStatus oldStatus = this.status;
        this.status = AppealStatus.LEVEL1_APPROVED;
        this.level1ReviewerId = reviewerId;
        this.level1ReviewedAt = LocalDateTime.now();
        this.level1Comment = comment;

        addApprovalRecord(reviewerId, "LEVEL1", "APPROVE", comment);
        registerEvent(new AppealStatusChangedEvent(this, oldStatus, this.status, reviewerId));
    }

    /**
     * Level 1 rejects the appeal.
     */
    public void level1Reject(Long reviewerId, String comment) {
        assertCanTransitionTo(AppealStatus.LEVEL1_REJECTED);
        AppealStatus oldStatus = this.status;
        this.status = AppealStatus.LEVEL1_REJECTED;
        this.level1ReviewerId = reviewerId;
        this.level1ReviewedAt = LocalDateTime.now();
        this.level1Comment = comment;

        addApprovalRecord(reviewerId, "LEVEL1", "REJECT", comment);
        registerEvent(new AppealStatusChangedEvent(this, oldStatus, this.status, reviewerId));
    }

    /**
     * Starts Level 2 review.
     */
    public void startLevel2Review(Long reviewerId) {
        assertCanTransitionTo(AppealStatus.LEVEL2_REVIEWING);
        AppealStatus oldStatus = this.status;
        this.status = AppealStatus.LEVEL2_REVIEWING;
        this.level2ReviewerId = reviewerId;

        addApprovalRecord(reviewerId, "LEVEL2", "START_REVIEW", null);
        registerEvent(new AppealStatusChangedEvent(this, oldStatus, this.status, reviewerId));
    }

    /**
     * Final approval of the appeal.
     */
    public void approve(Long reviewerId, String comment, BigDecimal approvedDeduction) {
        assertCanTransitionTo(AppealStatus.APPROVED);
        AppealStatus oldStatus = this.status;
        this.status = AppealStatus.APPROVED;
        this.level2ReviewerId = reviewerId;
        this.level2ReviewedAt = LocalDateTime.now();
        this.level2Comment = comment;
        this.approvedDeduction = approvedDeduction;

        addApprovalRecord(reviewerId, "LEVEL2", "APPROVE", comment);
        registerEvent(new AppealStatusChangedEvent(this, oldStatus, this.status, reviewerId));
    }

    /**
     * Final rejection of the appeal.
     */
    public void reject(Long reviewerId, String comment) {
        assertCanTransitionTo(AppealStatus.REJECTED);
        AppealStatus oldStatus = this.status;
        this.status = AppealStatus.REJECTED;
        this.level2ReviewerId = reviewerId;
        this.level2ReviewedAt = LocalDateTime.now();
        this.level2Comment = comment;

        addApprovalRecord(reviewerId, "LEVEL2", "REJECT", comment);
        registerEvent(new AppealStatusChangedEvent(this, oldStatus, this.status, reviewerId));
    }

    /**
     * Withdraws the appeal.
     */
    public void withdraw(Long applicantId) {
        if (this.status != AppealStatus.PENDING) {
            throw new IllegalStateException("Can only withdraw pending appeals");
        }
        if (!this.applicantId.equals(applicantId)) {
            throw new IllegalArgumentException("Only the applicant can withdraw the appeal");
        }

        AppealStatus oldStatus = this.status;
        this.status = AppealStatus.WITHDRAWN;

        addApprovalRecord(applicantId, "APPLICANT", "WITHDRAW", null);
        registerEvent(new AppealStatusChangedEvent(this, oldStatus, this.status, applicantId));
    }

    /**
     * Makes the appeal effective (score adjustment applied).
     */
    public void makeEffective() {
        assertCanTransitionTo(AppealStatus.EFFECTIVE);
        AppealStatus oldStatus = this.status;
        this.status = AppealStatus.EFFECTIVE;
        this.effectiveAt = LocalDateTime.now();

        addApprovalRecord(null, "SYSTEM", "MAKE_EFFECTIVE", "申诉生效，分数已调整");
        registerEvent(new AppealStatusChangedEvent(this, oldStatus, this.status, null));
    }

    /**
     * Calculates the score difference if approved.
     */
    public BigDecimal calculateScoreDifference() {
        BigDecimal adjustedDeduction = approvedDeduction != null ? approvedDeduction : requestedDeduction;
        return originalDeduction.subtract(adjustedDeduction);
    }

    private void assertCanTransitionTo(AppealStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", status, target));
        }
    }

    private void addApprovalRecord(Long reviewerId, String level, String action, String comment) {
        AppealApproval record = AppealApproval.builder()
            .appealId(this.id)
            .reviewerId(reviewerId)
            .reviewLevel(level)
            .action(action)
            .comment(comment)
            .reviewedAt(LocalDateTime.now())
            .build();
        this.approvalRecords.add(record);
    }

    private void validate() {
        if (inspectionRecordId == null) {
            throw new IllegalArgumentException("Inspection record ID is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Appeal reason cannot be empty");
        }
        if (originalDeduction == null) {
            throw new IllegalArgumentException("Original deduction is required");
        }
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInspectionRecordId() {
        return inspectionRecordId;
    }

    public Long getDeductionDetailId() {
        return deductionDetailId;
    }

    public Long getClassId() {
        return classId;
    }

    public String getAppealCode() {
        return appealCode;
    }

    public String getReason() {
        return reason;
    }

    public List<String> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }

    public BigDecimal getOriginalDeduction() {
        return originalDeduction;
    }

    public BigDecimal getRequestedDeduction() {
        return requestedDeduction;
    }

    public BigDecimal getApprovedDeduction() {
        return approvedDeduction;
    }

    public AppealStatus getStatus() {
        return status;
    }

    public Long getApplicantId() {
        return applicantId;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public Long getLevel1ReviewerId() {
        return level1ReviewerId;
    }

    public LocalDateTime getLevel1ReviewedAt() {
        return level1ReviewedAt;
    }

    public String getLevel1Comment() {
        return level1Comment;
    }

    public Long getLevel2ReviewerId() {
        return level2ReviewerId;
    }

    public LocalDateTime getLevel2ReviewedAt() {
        return level2ReviewedAt;
    }

    public String getLevel2Comment() {
        return level2Comment;
    }

    public LocalDateTime getEffectiveAt() {
        return effectiveAt;
    }

    public List<AppealApproval> getApprovalRecords() {
        return Collections.unmodifiableList(approvalRecords);
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long inspectionRecordId;
        private Long deductionDetailId;
        private Long classId;
        private String appealCode;
        private String reason;
        private List<String> attachments;
        private BigDecimal originalDeduction;
        private BigDecimal requestedDeduction;
        private Long applicantId;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder inspectionRecordId(Long inspectionRecordId) {
            this.inspectionRecordId = inspectionRecordId;
            return this;
        }

        public Builder deductionDetailId(Long deductionDetailId) {
            this.deductionDetailId = deductionDetailId;
            return this;
        }

        public Builder classId(Long classId) {
            this.classId = classId;
            return this;
        }

        public Builder appealCode(String appealCode) {
            this.appealCode = appealCode;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder attachments(List<String> attachments) {
            this.attachments = attachments;
            return this;
        }

        public Builder originalDeduction(BigDecimal originalDeduction) {
            this.originalDeduction = originalDeduction;
            return this;
        }

        public Builder requestedDeduction(BigDecimal requestedDeduction) {
            this.requestedDeduction = requestedDeduction;
            return this;
        }

        public Builder applicantId(Long applicantId) {
            this.applicantId = applicantId;
            return this;
        }

        public Appeal build() {
            return new Appeal(this);
        }
    }
}
