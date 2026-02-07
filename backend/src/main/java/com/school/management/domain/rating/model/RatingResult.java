package com.school.management.domain.rating.model;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.rating.event.RatingApprovedEvent;
import com.school.management.domain.rating.event.RatingCalculatedEvent;
import com.school.management.domain.rating.event.RatingPublishedEvent;
import com.school.management.domain.rating.event.RatingRejectedEvent;
import com.school.management.domain.rating.event.RatingRevokedEvent;
import com.school.management.domain.rating.event.RatingSubmittedEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Rating result aggregate root.
 *
 * <p>Represents the rating result for a class in a specific period.
 * Follows a state machine for approval workflow.
 */
public class RatingResult extends AggregateRoot<Long> {

    private Long ratingConfigId;
    private Long checkPlanId;
    private Long classId;
    private String className;
    private RatingPeriodType periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer ranking;
    private BigDecimal finalScore;
    private boolean awarded;
    private RatingResultStatus status;
    private LocalDateTime calculatedAt;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String approvalComment;
    private Long publishedBy;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected RatingResult() {}

    /**
     * Creates a new rating result from calculation.
     *
     * @param ratingConfigId rating config ID
     * @param checkPlanId    check plan ID
     * @param classId        class ID
     * @param className      class name
     * @param periodType     period type
     * @param periodStart    period start date
     * @param periodEnd      period end date
     * @param ranking        class ranking
     * @param finalScore     final score
     * @param awarded        whether rating was awarded
     * @return new RatingResult instance
     */
    public static RatingResult create(Long ratingConfigId, Long checkPlanId,
                                       Long classId, String className,
                                       RatingPeriodType periodType,
                                       LocalDate periodStart, LocalDate periodEnd,
                                       Integer ranking, BigDecimal finalScore,
                                       boolean awarded) {
        RatingResult result = new RatingResult();
        result.ratingConfigId = Objects.requireNonNull(ratingConfigId);
        result.checkPlanId = checkPlanId;
        result.classId = Objects.requireNonNull(classId);
        result.className = className;
        result.periodType = periodType;
        result.periodStart = periodStart;
        result.periodEnd = periodEnd;
        result.ranking = ranking;
        result.finalScore = finalScore;
        result.awarded = awarded;
        result.status = RatingResultStatus.DRAFT;
        result.calculatedAt = LocalDateTime.now();
        result.createdAt = LocalDateTime.now();
        result.updatedAt = result.createdAt;

        // Note: getId() is null before persistence; event consumers must handle null resultId
        result.registerEvent(RatingCalculatedEvent.of(
            null, ratingConfigId, classId, className, ranking, finalScore, awarded));

        return result;
    }

    /**
     * Submits for approval.
     */
    public void submitForApproval() {
        if (!status.canTransitionTo(RatingResultStatus.PENDING_APPROVAL)) {
            throw new IllegalStateException(
                "Cannot submit for approval from status: " + status);
        }
        this.status = RatingResultStatus.PENDING_APPROVAL;
        this.updatedAt = LocalDateTime.now();

        registerEvent(RatingSubmittedEvent.of(getId(), ratingConfigId, classId));
    }

    /**
     * Approves the rating result.
     *
     * @param approverId approver user ID
     * @param comment    approval comment
     */
    public void approve(Long approverId, String comment) {
        if (!status.canTransitionTo(RatingResultStatus.APPROVED)) {
            throw new IllegalStateException(
                "Cannot approve from status: " + status);
        }
        this.status = RatingResultStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = LocalDateTime.now();
        this.approvalComment = comment;
        this.updatedAt = LocalDateTime.now();

        registerEvent(RatingApprovedEvent.of(getId(), ratingConfigId, classId, approverId));
    }

    /**
     * Rejects the rating result.
     *
     * @param reviewerId reviewer user ID
     * @param reason     rejection reason
     */
    public void reject(Long reviewerId, String reason) {
        if (!status.canTransitionTo(RatingResultStatus.REJECTED)) {
            throw new IllegalStateException(
                "Cannot reject from status: " + status);
        }
        this.status = RatingResultStatus.REJECTED;
        this.approvedBy = reviewerId;
        this.approvedAt = LocalDateTime.now();
        this.approvalComment = reason;
        this.updatedAt = LocalDateTime.now();

        registerEvent(RatingRejectedEvent.of(getId(), ratingConfigId, classId, reviewerId, reason));
    }

    /**
     * Publishes the rating result.
     *
     * @param publisherId publisher user ID
     */
    public void publish(Long publisherId) {
        if (!status.canTransitionTo(RatingResultStatus.PUBLISHED)) {
            throw new IllegalStateException(
                "Cannot publish from status: " + status);
        }
        this.status = RatingResultStatus.PUBLISHED;
        this.publishedBy = publisherId;
        this.publishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        registerEvent(RatingPublishedEvent.of(getId(), ratingConfigId, classId, className, awarded));
    }

    /**
     * Revokes a published result.
     *
     * @param revokedBy user who revoked
     */
    public void revoke(Long revokedBy) {
        if (!status.canTransitionTo(RatingResultStatus.REVOKED)) {
            throw new IllegalStateException(
                "Cannot revoke from status: " + status);
        }
        this.status = RatingResultStatus.REVOKED;
        this.updatedAt = LocalDateTime.now();

        registerEvent(RatingRevokedEvent.of(getId(), ratingConfigId, classId, revokedBy));
    }

    /**
     * Resets to draft state (after rejection or revocation).
     */
    public void resetToDraft() {
        if (!status.canTransitionTo(RatingResultStatus.DRAFT)) {
            throw new IllegalStateException(
                "Cannot reset to draft from status: " + status);
        }
        this.status = RatingResultStatus.DRAFT;
        this.approvedBy = null;
        this.approvedAt = null;
        this.approvalComment = null;
        this.publishedBy = null;
        this.publishedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters

    public Long getRatingConfigId() { return ratingConfigId; }
    public Long getCheckPlanId() { return checkPlanId; }
    public Long getClassId() { return classId; }
    public String getClassName() { return className; }
    public RatingPeriodType getPeriodType() { return periodType; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public Integer getRanking() { return ranking; }
    public BigDecimal getFinalScore() { return finalScore; }
    public boolean isAwarded() { return awarded; }
    public RatingResultStatus getStatus() { return status; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public Long getApprovedBy() { return approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public String getApprovalComment() { return approvalComment; }
    public Long getPublishedBy() { return publishedBy; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Builder

    public static class Builder {
        private final RatingResult r = new RatingResult();

        public Builder id(Long v) { r.setId(v); return this; }
        public Builder ratingConfigId(Long v) { r.ratingConfigId = v; return this; }
        public Builder checkPlanId(Long v) { r.checkPlanId = v; return this; }
        public Builder classId(Long v) { r.classId = v; return this; }
        public Builder className(String v) { r.className = v; return this; }
        public Builder periodType(RatingPeriodType v) { r.periodType = v; return this; }
        public Builder periodStart(LocalDate v) { r.periodStart = v; return this; }
        public Builder periodEnd(LocalDate v) { r.periodEnd = v; return this; }
        public Builder ranking(Integer v) { r.ranking = v; return this; }
        public Builder finalScore(BigDecimal v) { r.finalScore = v; return this; }
        public Builder awarded(boolean v) { r.awarded = v; return this; }
        public Builder status(RatingResultStatus v) { r.status = v; return this; }
        public Builder calculatedAt(LocalDateTime v) { r.calculatedAt = v; return this; }
        public Builder approvedBy(Long v) { r.approvedBy = v; return this; }
        public Builder approvedAt(LocalDateTime v) { r.approvedAt = v; return this; }
        public Builder approvalComment(String v) { r.approvalComment = v; return this; }
        public Builder publishedBy(Long v) { r.publishedBy = v; return this; }
        public Builder publishedAt(LocalDateTime v) { r.publishedAt = v; return this; }
        public Builder createdAt(LocalDateTime v) { r.createdAt = v; return this; }
        public Builder updatedAt(LocalDateTime v) { r.updatedAt = v; return this; }
        public Builder version(Long v) { r.setVersion(v); return this; }
        public RatingResult build() { return r; }
    }

    public static Builder builder() { return new Builder(); }
}
