package com.school.management.domain.task.model;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entity representing a task submission.
 *
 * <p>When an assignee submits their work, a submission record is created
 * with details and attachments for review.
 */
public class Submission extends Entity<Long> {

    /**
     * Review status for submissions.
     */
    public enum ReviewStatus {
        PENDING("待审核"),
        IN_REVIEW("审核中"),
        APPROVED("已通过"),
        REJECTED("已打回");

        private final String displayName;

        ReviewStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private Long assigneeId;
    private Long taskId;
    private String content;
    private List<Long> attachmentIds;
    private Long submitterId;
    private String submitterName;
    private LocalDateTime submittedAt;
    private ReviewStatus reviewStatus;
    private Long reviewerId;
    private String reviewerName;
    private String reviewComment;
    private LocalDateTime reviewedAt;
    private Integer rejectCount;

    protected Submission() {
        this.attachmentIds = new ArrayList<>();
        this.rejectCount = 0;
    }

    /**
     * Creates a new submission.
     *
     * @param assigneeId    the assignee ID
     * @param taskId        the task ID
     * @param content       submission content
     * @param attachmentIds attachment IDs
     * @param submitterId   submitter user ID
     * @param submitterName submitter name
     * @return new Submission instance
     */
    public static Submission create(Long assigneeId, Long taskId, String content,
                                     List<Long> attachmentIds, Long submitterId,
                                     String submitterName) {
        Submission submission = new Submission();
        submission.assigneeId = assigneeId;
        submission.taskId = taskId;
        submission.content = content;
        submission.attachmentIds = attachmentIds != null ? new ArrayList<>(attachmentIds) : new ArrayList<>();
        submission.submitterId = submitterId;
        submission.submitterName = submitterName;
        submission.submittedAt = LocalDateTime.now();
        submission.reviewStatus = ReviewStatus.PENDING;
        return submission;
    }

    /**
     * Approves this submission.
     *
     * @param reviewerId   reviewer user ID
     * @param reviewerName reviewer name
     * @param comment      review comment
     */
    public void approve(Long reviewerId, String reviewerName, String comment) {
        if (reviewStatus != ReviewStatus.PENDING && reviewStatus != ReviewStatus.IN_REVIEW) {
            throw new IllegalStateException(
                "Cannot approve submission in status: " + reviewStatus);
        }
        this.reviewStatus = ReviewStatus.APPROVED;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.reviewComment = comment;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * Rejects this submission.
     *
     * @param reviewerId   reviewer user ID
     * @param reviewerName reviewer name
     * @param reason       rejection reason
     */
    public void reject(Long reviewerId, String reviewerName, String reason) {
        if (reviewStatus != ReviewStatus.PENDING && reviewStatus != ReviewStatus.IN_REVIEW) {
            throw new IllegalStateException(
                "Cannot reject submission in status: " + reviewStatus);
        }
        this.reviewStatus = ReviewStatus.REJECTED;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.reviewComment = reason;
        this.reviewedAt = LocalDateTime.now();
        this.rejectCount++;
    }

    /**
     * Starts review process.
     *
     * @param reviewerId   reviewer user ID
     * @param reviewerName reviewer name
     */
    public void startReview(Long reviewerId, String reviewerName) {
        if (reviewStatus != ReviewStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot start review from status: " + reviewStatus);
        }
        this.reviewStatus = ReviewStatus.IN_REVIEW;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
    }

    // Getters

    public Long getAssigneeId() {
        return assigneeId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getContent() {
        return content;
    }

    public List<Long> getAttachmentIds() {
        return Collections.unmodifiableList(attachmentIds);
    }

    public Long getSubmitterId() {
        return submitterId;
    }

    public String getSubmitterName() {
        return submitterName;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public Integer getRejectCount() {
        return rejectCount;
    }

    // Builder pattern for reconstruction from persistence

    public static class Builder {
        private final Submission submission = new Submission();

        public Builder id(Long id) {
            submission.setId(id);
            return this;
        }

        public Builder assigneeId(Long assigneeId) {
            submission.assigneeId = assigneeId;
            return this;
        }

        public Builder taskId(Long taskId) {
            submission.taskId = taskId;
            return this;
        }

        public Builder content(String content) {
            submission.content = content;
            return this;
        }

        public Builder attachmentIds(List<Long> attachmentIds) {
            submission.attachmentIds = attachmentIds != null ? new ArrayList<>(attachmentIds) : new ArrayList<>();
            return this;
        }

        public Builder submitterId(Long submitterId) {
            submission.submitterId = submitterId;
            return this;
        }

        public Builder submitterName(String submitterName) {
            submission.submitterName = submitterName;
            return this;
        }

        public Builder submittedAt(LocalDateTime submittedAt) {
            submission.submittedAt = submittedAt;
            return this;
        }

        public Builder reviewStatus(ReviewStatus reviewStatus) {
            submission.reviewStatus = reviewStatus;
            return this;
        }

        public Builder reviewerId(Long reviewerId) {
            submission.reviewerId = reviewerId;
            return this;
        }

        public Builder reviewerName(String reviewerName) {
            submission.reviewerName = reviewerName;
            return this;
        }

        public Builder reviewComment(String reviewComment) {
            submission.reviewComment = reviewComment;
            return this;
        }

        public Builder reviewedAt(LocalDateTime reviewedAt) {
            submission.reviewedAt = reviewedAt;
            return this;
        }

        public Builder rejectCount(Integer rejectCount) {
            submission.rejectCount = rejectCount;
            return this;
        }

        public Submission build() {
            return submission;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
