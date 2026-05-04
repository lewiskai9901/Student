package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.repository.projection.ProjectTaskStats;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.TaskStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InspTaskRepository {

    InspTask save(InspTask task);

    Optional<InspTask> findById(Long id);

    Optional<InspTask> findByTaskCode(String taskCode);

    List<InspTask> findByProjectId(Long projectId);

    List<InspTask> findByInspectorId(Long inspectorId);

    /**
     * 我的任务: 作为检查员 OR 作为审核员 (审核相关阶段)
     */
    List<InspTask> findByInspectorOrReviewerId(Long userId);

    List<InspTask> findByProjectIdAndTaskDate(Long projectId, LocalDate taskDate);

    List<InspTask> findByProjectIdAndTaskDateBetween(Long projectId, LocalDate startDate, LocalDate endDate);

    List<InspTask> findByStatus(TaskStatus status);

    List<InspTask> findAll();

    List<InspTask> findAvailableTasks();

    void deleteById(Long id);

    void deleteByProjectId(Long projectId);

    /** 项目列表批量任务统计 (列表页 N+1 消除). */
    List<ProjectTaskStats> findStatsByProjectIds(List<Long> projectIds);
}
