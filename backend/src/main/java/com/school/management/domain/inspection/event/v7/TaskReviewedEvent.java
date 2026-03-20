package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class TaskReviewedEvent extends BaseDomainEvent implements InspV7DomainEvent {

    private final Long taskId;
    private final String taskCode;
    private final Long reviewerId;

    public TaskReviewedEvent(Long taskId, String taskCode, Long reviewerId) {
        super("InspTask", taskId);
        this.taskId = taskId;
        this.taskCode = taskCode;
        this.reviewerId = reviewerId;
    }

    public Long getTaskId() { return taskId; }
    public String getTaskCode() { return taskCode; }
    public Long getReviewerId() { return reviewerId; }
}
