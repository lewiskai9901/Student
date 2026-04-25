package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.execution.ProjectStatus;

import java.util.List;
import java.util.Optional;

public interface InspProjectRepository {

    InspProject save(InspProject project);

    Optional<InspProject> findById(Long id);

    Optional<InspProject> findByProjectCode(String projectCode);

    List<InspProject> findByStatus(ProjectStatus status);

    List<InspProject> findByRootSectionId(Long rootSectionId);

    List<InspProject> findAll();

    void deleteById(Long id);
}
