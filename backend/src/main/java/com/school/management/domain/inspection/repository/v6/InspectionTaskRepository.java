package com.school.management.domain.inspection.repository.v6;

import com.school.management.domain.inspection.model.v6.InspectionTask;
import com.school.management.domain.inspection.model.v6.TaskStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * V6检查任务仓储接口
 */
public interface InspectionTaskRepository {

    InspectionTask save(InspectionTask task);

    void saveAll(List<InspectionTask> tasks);

    Optional<InspectionTask> findById(Long id);

    Optional<InspectionTask> findByTaskCode(String taskCode);

    List<InspectionTask> findByProjectId(Long projectId);

    List<InspectionTask> findByProjectIdAndDate(Long projectId, LocalDate taskDate);

    List<InspectionTask> findByInspectorId(Long inspectorId);

    List<InspectionTask> findByStatus(TaskStatus status);

    List<InspectionTask> findAvailableTasksForDate(LocalDate date);

    List<InspectionTask> findMyTasks(Long inspectorId);

    List<InspectionTask> findPagedWithConditions(int page, int size, Long projectId, TaskStatus status,
                                                  LocalDate startDate, LocalDate endDate, Long inspectorId);

    long countWithConditions(Long projectId, TaskStatus status, LocalDate startDate,
                             LocalDate endDate, Long inspectorId);

    boolean claimTask(Long id, Long inspectorId, String inspectorName);

    void delete(Long id);

    void updateTotalTargets(Long id, Integer totalTargets);

    void incrementCompletedTargets(Long id);

    void incrementSkippedTargets(Long id);

    int countByProjectAndDate(Long projectId, LocalDate taskDate);
}
