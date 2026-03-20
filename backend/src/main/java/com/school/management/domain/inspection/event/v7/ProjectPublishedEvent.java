package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.BaseDomainEvent;

public class ProjectPublishedEvent extends BaseDomainEvent implements InspV7DomainEvent {

    private final Long projectId;
    private final String projectCode;
    private final Long templateId;

    public ProjectPublishedEvent(Long projectId, String projectCode, Long templateId) {
        super("InspProject", projectId);
        this.projectId = projectId;
        this.projectCode = projectCode;
        this.templateId = templateId;
    }

    public Long getProjectId() { return projectId; }
    public String getProjectCode() { return projectCode; }
    public Long getTemplateId() { return templateId; }
}
