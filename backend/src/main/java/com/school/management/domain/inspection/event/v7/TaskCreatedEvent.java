package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.BaseDomainEvent;

import java.time.LocalDate;

public class TaskCreatedEvent extends BaseDomainEvent implements InspV7DomainEvent {

    private final Long taskId;
    private final String taskCode;
    private final Long projectId;
    private final LocalDate taskDate;

    public TaskCreatedEvent(Long taskId, String taskCode, Long projectId, LocalDate taskDate) {
        super("InspTask", taskId);
        this.taskId = taskId;
        this.taskCode = taskCode;
        this.projectId = projectId;
        this.taskDate = taskDate;
    }

    public Long getTaskId() { return taskId; }
    public String getTaskCode() { return taskCode; }
    public Long getProjectId() { return projectId; }
    public LocalDate getTaskDate() { return taskDate; }
}
