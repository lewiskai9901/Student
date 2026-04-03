package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.task.TeachingTask;

import java.util.List;
import java.util.Optional;

public interface TeachingTaskRepository {
    TeachingTask save(TeachingTask task);
    Optional<TeachingTask> findById(Long id);
    List<TeachingTask> findByFilter(Long semesterId, Integer taskStatus, int offset, int limit);
    long countByFilter(Long semesterId, Integer taskStatus);
    void deleteById(Long id);
}
