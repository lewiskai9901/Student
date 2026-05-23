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

    /**
     * 精确查 (project, user, role) — LEAD 自动绑定幂等性 + 角色矩阵 toggle.
     */
    Optional<ProjectInspector> findOneByProjectUserRole(Long projectId, Long userId, InspectorRole role);

    /**
     * 统计某项目某角色的活跃人数 — LEAD 不变量校验用 (≥1).
     */
    int countActiveByProjectIdAndRole(Long projectId, InspectorRole role);
}
