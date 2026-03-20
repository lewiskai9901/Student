package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class TaskSubmittedEvent extends BaseDomainEvent implements InspV7DomainEvent {

    private final Long taskId;
    private final String taskCode;
    private final Long inspectorId;
    private final Long projectId;

    public TaskSubmittedEvent(Long taskId, String taskCode, Long inspectorId, Long projectId) {
        super("InspTask", taskId);
        this.taskId = taskId;
        this.taskCode = taskCode;
        this.inspectorId = inspectorId;
        this.projectId = projectId;
    }

    public Long getTaskId() { return taskId; }
    public String getTaskCode() { return taskCode; }
    public Long getInspectorId() { return inspectorId; }
    public Long getProjectId() { return projectId; }
}
