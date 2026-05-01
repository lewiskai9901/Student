package com.school.management.application.inspection.dto;

import com.school.management.domain.inspection.model.execution.InspProject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 项目列表聚合视图 — 项目本身 + 任务统计 + 检查员人数.
 *
 * 用于 /inspection/projects/with-stats 端点, 替代前端 N+1 调用.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectStatsSummary {

    private InspProject project;

    private int taskTotal;
    private int taskDone;
    private int taskOverdue;
    private int taskPendingReview;

    private int inspectorCount;
}
