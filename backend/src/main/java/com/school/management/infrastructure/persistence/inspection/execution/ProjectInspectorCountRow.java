package com.school.management.infrastructure.persistence.inspection.execution;

import lombok.Data;

/**
 * 项目检查员人数 (按 project_id 聚合).
 */
@Data
public class ProjectInspectorCountRow {
    private Long projectId;
    private Integer inspectorCount;
}
