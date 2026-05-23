package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 项目创建事件 — 由 InspProject.create() 注册.
 *
 * <p>AutoEnrollCreatorAsLeadHandler 监听该事件 (AFTER_COMMIT) 自动把 createdBy
 * 写入 project_inspector, role=LEAD, 让创建者立即成为"项目负责人".
 */
public class ProjectCreatedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long projectId;
    private final String projectCode;
    private final String projectName;
    private final Long createdBy;
    private final Long orgUnitId;

    public ProjectCreatedEvent(Long projectId, String projectCode, String projectName,
                               Long createdBy, Long orgUnitId) {
        super("InspProject", projectId);
        this.projectId = projectId;
        this.projectCode = projectCode;
        this.projectName = projectName;
        this.createdBy = createdBy;
        this.orgUnitId = orgUnitId;
    }

    public Long getProjectId() { return projectId; }
    public String getProjectCode() { return projectCode; }
    public String getProjectName() { return projectName; }
    public Long getCreatedBy() { return createdBy; }
    public Long getOrgUnitId() { return orgUnitId; }
}
