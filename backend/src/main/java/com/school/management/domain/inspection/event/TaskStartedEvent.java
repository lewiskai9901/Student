package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class TaskStartedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long taskId;
    private final String taskCode;

    public TaskStartedEvent(Long taskId, String taskCode) {
        super("InspTask", taskId);
        this.taskId = taskId;
        this.taskCode = taskCode;
    }

    public Long getTaskId() { return taskId; }
    public String getTaskCode() { return taskCode; }
}
