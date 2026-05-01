package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.execution.InspectorRole;
import com.school.management.domain.inspection.model.execution.ProjectInspector;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProjectInspectorRepository {

    ProjectInspector save(ProjectInspector inspector);

    Optional<ProjectInspector> findById(Long id);

    List<ProjectInspector> findByProjectId(Long projectId);

    List<ProjectInspector> findByProjectIdAndRole(Long projectId, InspectorRole role);

    List<ProjectInspector> findByUserId(Long userId);

    void deleteById(Long id);

    void deleteByProjectId(Long projectId);

    /** 列表页 N+1 消除: projectId -> 检查员人数. */
    Map<Long, Integer> countByProjectIds(List<Long> projectIds);
}
