package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.execution.TaskSectionAssignment;

import java.util.List;
import java.util.Optional;

public interface TaskSectionAssignmentRepository {

    TaskSectionAssignment save(TaskSectionAssignment assignment);

    Optional<TaskSectionAssignment> findById(Long id);

    List<TaskSectionAssignment> findByTaskId(Long taskId);

    void deleteByTaskId(Long taskId);
}
