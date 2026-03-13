package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.execution.InspProject;
import com.school.management.domain.inspection.model.v7.execution.ProjectStatus;

import java.util.List;
import java.util.Optional;

public interface InspProjectRepository {

    InspProject save(InspProject project);

    Optional<InspProject> findById(Long id);

    Optional<InspProject> findByProjectCode(String projectCode);

    List<InspProject> findByStatus(ProjectStatus status);

    List<InspProject> findByTemplateId(Long templateId);

    List<InspProject> findByParentProjectId(Long parentProjectId);

    List<InspProject> findAll();

    void deleteById(Long id);
}
