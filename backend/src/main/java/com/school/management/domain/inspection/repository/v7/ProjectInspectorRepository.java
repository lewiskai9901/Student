package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.execution.InspectorRole;
import com.school.management.domain.inspection.model.v7.execution.ProjectInspector;

import java.util.List;
import java.util.Optional;

public interface ProjectInspectorRepository {

    ProjectInspector save(ProjectInspector inspector);

    Optional<ProjectInspector> findById(Long id);

    List<ProjectInspector> findByProjectId(Long projectId);

    List<ProjectInspector> findByProjectIdAndRole(Long projectId, InspectorRole role);

    List<ProjectInspector> findByUserId(Long userId);

    void deleteById(Long id);

    void deleteByProjectId(Long projectId);
}
