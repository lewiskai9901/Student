package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class TaskCancelledEvent extends BaseDomainEvent implements InspV7DomainEvent {

    private final Long taskId;
    private final String taskCode;

    public TaskCancelledEvent(Long taskId, String taskCode) {
        super("InspTask", taskId);
        this.taskId = taskId;
        this.taskCode = taskCode;
    }

    public Long getTaskId() { return taskId; }
    public String getTaskCode() { return taskCode; }
}
