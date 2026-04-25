package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class ProjectCompletedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long projectId;
    private final String projectCode;

    public ProjectCompletedEvent(Long projectId, String projectCode) {
        super("InspProject", projectId);
        this.projectId = projectId;
        this.projectCode = projectCode;
    }

    public Long getProjectId() { return projectId; }
    public String getProjectCode() { return projectCode; }
}
