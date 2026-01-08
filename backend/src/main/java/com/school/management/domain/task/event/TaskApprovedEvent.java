package com.school.management.domain.task.event;

/**
 * Event published when a task submission is approved.
 */
public class TaskApprovedEvent extends TaskDomainEvent {

    private final Long assigneeId;
    private final Long reviewerId;
    private final String reviewerName;

    private TaskApprovedEvent(Long taskId, String taskCode, Long assigneeId, 
                              Long reviewerId, String reviewerName) {
        super(taskId, taskCode);
        this.assigneeId = assigneeId;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
    }

    public static TaskApprovedEvent of(Long taskId, String taskCode, Long assigneeId, 
                                        Long reviewerId, String reviewerName) {
        return new TaskApprovedEvent(taskId, taskCode, assigneeId, reviewerId, reviewerName);
    }

    @Override
    public String getEventType() {
        return "task.approved";
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
}
