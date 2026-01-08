package com.school.management.domain.task.model;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entity representing a task assignee within a Task aggregate.
 *
 * <p>In batch assignment scenarios, a task can have multiple assignees,
 * each tracking their own progress independently.
 */
public class Assignee extends Entity<Long> {

    private Long taskId;
    private Long userId;
    private String userName;
    private Long departmentId;
    private String departmentName;
    private AssigneeStatus status;
    private LocalDateTime acceptedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private String processInstanceId;
    private Integer currentApprovalLevel;
    private List<Submission> submissions;

    protected Assignee() {
        this.submissions = new ArrayList<>();
    }

    /**
     * Creates a new assignee for a task.
     *
     * @param taskId         the task ID
     * @param userId         the assignee user ID
     * @param userName       the assignee name
     * @param departmentId   the department ID
     * @param departmentName the department name
     * @return new Assignee instance
     */
    public static Assignee create(Long taskId, Long userId, String userName,
                                   Long departmentId, String departmentName) {
        Assignee assignee = new Assignee();
        assignee.taskId = taskId;
        assignee.userId = userId;
        assignee.userName = userName;
        assignee.departmentId = departmentId;
        assignee.departmentName = departmentName;
        assignee.status = AssigneeStatus.PENDING_ACCEPT;
        assignee.currentApprovalLevel = 0;
        return assignee;
    }

    /**
     * Accepts the task assignment.
     */
    public void accept() {
        if (!status.canTransitionTo(AssigneeStatus.IN_PROGRESS)) {
            throw new IllegalStateException(
                "Cannot accept task from status: " + status);
        }
        this.status = AssigneeStatus.IN_PROGRESS;
        this.acceptedAt = LocalDateTime.now();
    }

    /**
     * Submits work for review.
     *
     * @param content        submission content
     * @param attachmentIds  attachment IDs
     * @param submitterId    submitter user ID
     * @param submitterName  submitter name
     * @return the created submission
     */
    public Submission submit(String content, List<Long> attachmentIds,
                             Long submitterId, String submitterName) {
        if (!status.canTransitionTo(AssigneeStatus.PENDING_REVIEW)) {
            throw new IllegalStateException(
                "Cannot submit task from status: " + status);
        }

        Submission submission = Submission.create(
            getId(), taskId, content, attachmentIds, submitterId, submitterName);
        this.submissions.add(submission);
        this.status = AssigneeStatus.PENDING_REVIEW;
        this.submittedAt = LocalDateTime.now();
        return submission;
    }

    /**
     * Approves the submission and completes the assignee's task.
     *
     * @param reviewerId    reviewer user ID
     * @param reviewerName  reviewer name
     * @param comment       review comment
     */
    public void approve(Long reviewerId, String reviewerName, String comment) {
        if (!status.canTransitionTo(AssigneeStatus.COMPLETED)) {
            throw new IllegalStateException(
                "Cannot approve task from status: " + status);
        }

        Submission latestSubmission = getLatestSubmission();
        if (latestSubmission != null) {
            latestSubmission.approve(reviewerId, reviewerName, comment);
        }

        this.status = AssigneeStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Rejects the submission, requiring revision.
     *
     * @param reviewerId    reviewer user ID
     * @param reviewerName  reviewer name
     * @param reason        rejection reason
     */
    public void reject(Long reviewerId, String reviewerName, String reason) {
        if (!status.canTransitionTo(AssigneeStatus.REJECTED)) {
            throw new IllegalStateException(
                "Cannot reject task from status: " + status);
        }

        Submission latestSubmission = getLatestSubmission();
        if (latestSubmission != null) {
            latestSubmission.reject(reviewerId, reviewerName, reason);
        }

        this.status = AssigneeStatus.REJECTED;
    }

    /**
     * Resubmits after rejection.
     *
     * @param content        new submission content
     * @param attachmentIds  new attachment IDs
     * @param submitterId    submitter user ID
     * @param submitterName  submitter name
     * @return the new submission
     */
    public Submission resubmit(String content, List<Long> attachmentIds,
                               Long submitterId, String submitterName) {
        if (status != AssigneeStatus.REJECTED) {
            throw new IllegalStateException(
                "Can only resubmit from REJECTED status, current: " + status);
        }

        // First transition back to IN_PROGRESS
        this.status = AssigneeStatus.IN_PROGRESS;

        // Then submit again
        return submit(content, attachmentIds, submitterId, submitterName);
    }

    /**
     * Gets the latest submission.
     *
     * @return the latest submission or null
     */
    public Submission getLatestSubmission() {
        if (submissions.isEmpty()) {
            return null;
        }
        return submissions.get(submissions.size() - 1);
    }

    /**
     * Checks if this assignee has completed their task.
     *
     * @return true if completed
     */
    public boolean isCompleted() {
        return status == AssigneeStatus.COMPLETED;
    }

    // Getters

    public Long getTaskId() {
        return taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public AssigneeStatus getStatus() {
        return status;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public Integer getCurrentApprovalLevel() {
        return currentApprovalLevel;
    }

    public List<Submission> getSubmissions() {
        return Collections.unmodifiableList(submissions);
    }

    // Builder pattern for reconstruction from persistence

    public static class Builder {
        private final Assignee assignee = new Assignee();

        public Builder id(Long id) {
            assignee.setId(id);
            return this;
        }

        public Builder taskId(Long taskId) {
            assignee.taskId = taskId;
            return this;
        }

        public Builder userId(Long userId) {
            assignee.userId = userId;
            return this;
        }

        public Builder userName(String userName) {
            assignee.userName = userName;
            return this;
        }

        public Builder departmentId(Long departmentId) {
            assignee.departmentId = departmentId;
            return this;
        }

        public Builder departmentName(String departmentName) {
            assignee.departmentName = departmentName;
            return this;
        }

        public Builder status(AssigneeStatus status) {
            assignee.status = status;
            return this;
        }

        public Builder acceptedAt(LocalDateTime acceptedAt) {
            assignee.acceptedAt = acceptedAt;
            return this;
        }

        public Builder submittedAt(LocalDateTime submittedAt) {
            assignee.submittedAt = submittedAt;
            return this;
        }

        public Builder completedAt(LocalDateTime completedAt) {
            assignee.completedAt = completedAt;
            return this;
        }

        public Builder processInstanceId(String processInstanceId) {
            assignee.processInstanceId = processInstanceId;
            return this;
        }

        public Builder currentApprovalLevel(Integer level) {
            assignee.currentApprovalLevel = level;
            return this;
        }

        public Builder submissions(List<Submission> submissions) {
            assignee.submissions = new ArrayList<>(submissions);
            return this;
        }

        public Assignee build() {
            return assignee;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
