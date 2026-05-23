package com.school.management.application.inspection.dto;

import java.time.LocalDate;

/**
 * 项目克隆命令 — 应用层 DTO.
 *
 * <p>从接口层 DTO (InspProjectController.CloneProjectRequest) 映射而来,
 * 避免应用服务直接依赖 interfaces 层 (违反 DDD 分层依赖方向).
 *
 * <p>{@code cloneInspectors} 为 null 视同 false (检查员各项目独立配置, 不复制).
 */
public class CloneProjectCommand {

    private final String projectName;
    private final Long orgUnitId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final boolean cloneInspectors;

    public CloneProjectCommand(String projectName, Long orgUnitId,
                                LocalDate startDate, LocalDate endDate,
                                Boolean cloneInspectors) {
        this.projectName = projectName;
        this.orgUnitId = orgUnitId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.cloneInspectors = Boolean.TRUE.equals(cloneInspectors);
    }

    public String getProjectName() { return projectName; }
    public Long getOrgUnitId() { return orgUnitId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isCloneInspectors() { return cloneInspectors; }
}
