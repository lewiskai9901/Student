package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class TaskPublishedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long taskId;
    private final String taskCode;
    private final Long projectId;

    public TaskPublishedEvent(Long taskId, String taskCode, Long projectId) {
        super("InspTask", taskId);
        this.taskId = taskId;
        this.taskCode = taskCode;
        this.projectId = projectId;
    }

    public Long getTaskId() { return taskId; }
    public String getTaskCode() { return taskCode; }
    public Long getProjectId() { return projectId; }
}
