package com.school.management.domain.inspection.repository.projection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个项目的任务计数视图 — 仓储聚合查询输出格式 (domain projection).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectTaskStats {
    private Long projectId;
    private int total;
    private int done;
    private int overdue;
    private int pendingReview;
}
