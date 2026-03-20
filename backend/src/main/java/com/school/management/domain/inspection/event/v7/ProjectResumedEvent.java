package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class ProjectResumedEvent extends BaseDomainEvent implements InspV7DomainEvent {

    private final Long projectId;
    private final String projectCode;

    public ProjectResumedEvent(Long projectId, String projectCode) {
        super("InspProject", projectId);
        this.projectId = projectId;
        this.projectCode = projectCode;
    }

    public Long getProjectId() { return projectId; }
    public String getProjectCode() { return projectCode; }
}
