package com.school.management.domain.inspection.repository.v6;

import com.school.management.domain.inspection.model.v6.InspectionProject;
import com.school.management.domain.inspection.model.v6.ProjectStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * V6检查项目仓储接口
 */
public interface InspectionProjectRepository {

    InspectionProject save(InspectionProject project);

    Optional<InspectionProject> findById(Long id);

    Optional<InspectionProject> findByProjectCode(String projectCode);

    List<InspectionProject> findByTemplateId(Long templateId);

    List<InspectionProject> findByStatus(ProjectStatus status);

    List<InspectionProject> findByCreatedBy(Long createdBy);

    List<InspectionProject> findActiveProjectsForDate(LocalDate date);

    List<InspectionProject> findPagedWithConditions(int page, int size, ProjectStatus status, String keyword);

    long countWithConditions(ProjectStatus status, String keyword);

    void delete(Long id);

    void updateTotalTasks(Long id, Integer totalTasks);

    void incrementCompletedTasks(Long id);
}
