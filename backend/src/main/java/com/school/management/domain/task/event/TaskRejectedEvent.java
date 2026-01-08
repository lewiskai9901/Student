package com.school.management.domain.task.event;

/**
 * Event published when a task submission is rejected.
 */
public class TaskRejectedEvent extends TaskDomainEvent {

    private final Long assigneeId;
    private final Long reviewerId;
    private final String reviewerName;
    private final String reason;

    private TaskRejectedEvent(Long taskId, String taskCode, Long assigneeId, 
                              Long reviewerId, String reviewerName, String reason) {
        super(taskId, taskCode);
        this.assigneeId = assigneeId;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.reason = reason;
    }

    public static TaskRejectedEvent of(Long taskId, String taskCode, Long assigneeId, 
                                        Long reviewerId, String reviewerName, String reason) {
        return new TaskRejectedEvent(taskId, taskCode, assigneeId, reviewerId, reviewerName, reason);
    }

    @Override
    public String getEventType() {
        return "task.rejected";
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public String getReason() {
        return reason;
    }
}
