package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.execution.TaskSectionAssignment;

import java.util.List;
import java.util.Optional;

public interface TaskSectionAssignmentRepository {

    TaskSectionAssignment save(TaskSectionAssignment assignment);

    Optional<TaskSectionAssignment> findById(Long id);

    List<TaskSectionAssignment> findByTaskId(Long taskId);

    void deleteByTaskId(Long taskId);
}
