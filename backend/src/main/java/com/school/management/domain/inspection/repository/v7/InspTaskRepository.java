package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.execution.InspTask;
import com.school.management.domain.inspection.model.v7.execution.TaskStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InspTaskRepository {

    InspTask save(InspTask task);

    Optional<InspTask> findById(Long id);

    Optional<InspTask> findByTaskCode(String taskCode);

    List<InspTask> findByProjectId(Long projectId);

    List<InspTask> findByInspectorId(Long inspectorId);

    List<InspTask> findByProjectIdAndTaskDate(Long projectId, LocalDate taskDate);

    List<InspTask> findByProjectIdAndTaskDateBetween(Long projectId, LocalDate startDate, LocalDate endDate);

    List<InspTask> findByStatus(TaskStatus status);

    List<InspTask> findAll();

    List<InspTask> findAvailableTasks();

    void deleteById(Long id);

    void deleteByProjectId(Long projectId);
}
