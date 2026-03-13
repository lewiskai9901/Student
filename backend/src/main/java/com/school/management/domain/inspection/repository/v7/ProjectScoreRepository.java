package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.execution.ProjectScore;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProjectScoreRepository {

    ProjectScore save(ProjectScore score);

    Optional<ProjectScore> findById(Long id);

    Optional<ProjectScore> findByProjectIdAndCycleDate(Long projectId, LocalDate cycleDate);

    List<ProjectScore> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);
}
