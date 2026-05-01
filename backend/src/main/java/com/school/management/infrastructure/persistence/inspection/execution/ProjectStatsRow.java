package com.school.management.infrastructure.persistence.inspection.execution;

import lombok.Data;

/**
 * 项目任务统计行 (按 project_id 聚合).
 * MyBatis 查询结果直接映射到这里.
 */
@Data
public class ProjectStatsRow {
    private Long projectId;
    private Integer total;
    private Integer done;
    private Integer overdue;
    private Integer pendingReview;
}
