package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.execution.ProjectStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface InspProjectRepository {

    InspProject save(InspProject project);

    Optional<InspProject> findById(Long id);

    Optional<InspProject> findByProjectCode(String projectCode);

    List<InspProject> findByStatus(ProjectStatus status);

    List<InspProject> findByRootSectionId(Long rootSectionId);

    List<InspProject> findAll();

    void deleteById(Long id);

    /** P1-160: 模板使用计数 — rootSectionId -> 在用项目数 (status != ARCHIVED). */
    Map<Long, Integer> countByRootSectionIds(List<Long> rootSectionIds);
}
