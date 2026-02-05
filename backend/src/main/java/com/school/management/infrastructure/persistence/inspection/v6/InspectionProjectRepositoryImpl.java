package com.school.management.infrastructure.persistence.inspection.v6;

import com.school.management.domain.inspection.model.v6.*;
import com.school.management.domain.inspection.repository.v6.InspectionProjectRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * V6检查项目仓储实现
 */
@Repository
public class InspectionProjectRepositoryImpl implements InspectionProjectRepository {

    private final InspectionProjectMapper mapper;

    public InspectionProjectRepositoryImpl(InspectionProjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InspectionProject save(InspectionProject project) {
        InspectionProjectPO po = toPO(project);
        if (project.getId() == null) {
            mapper.insert(po);
            project.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return project;
    }

    @Override
    public Optional<InspectionProject> findById(Long id) {
        InspectionProjectPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<InspectionProject> findByProjectCode(String projectCode) {
        InspectionProjectPO po = mapper.findByProjectCode(projectCode);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<InspectionProject> findByTemplateId(Long templateId) {
        return mapper.findByTemplateId(templateId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionProject> findByStatus(ProjectStatus status) {
        return mapper.findByStatus(status.name()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionProject> findByCreatedBy(Long createdBy) {
        return mapper.findByCreatedBy(createdBy).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionProject> findActiveProjectsForDate(LocalDate date) {
        return mapper.findActiveProjectsForDate(date).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<InspectionProject> findPagedWithConditions(int page, int size, ProjectStatus status, String keyword) {
        int offset = (page - 1) * size;
        String statusStr = status != null ? status.name() : null;
        return mapper.findPagedWithConditions(offset, size, statusStr, keyword).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countWithConditions(ProjectStatus status, String keyword) {
        String statusStr = status != null ? status.name() : null;
        return mapper.countWithConditions(statusStr, keyword);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void updateTotalTasks(Long id, Integer totalTasks) {
        mapper.updateTotalTasks(id, totalTasks);
    }

    @Override
    public void incrementCompletedTasks(Long id) {
        mapper.incrementCompletedTasks(id);
    }

    private InspectionProjectPO toPO(InspectionProject domain) {
        InspectionProjectPO po = new InspectionProjectPO();
        po.setId(domain.getId());
        po.setProjectCode(domain.getProjectCode());
        po.setProjectName(domain.getProjectName());
        po.setDescription(domain.getDescription());
        po.setTemplateId(domain.getTemplateId());
        po.setTemplateSnapshot(domain.getTemplateSnapshot());
        po.setScopeType(domain.getScopeType() != null ? domain.getScopeType().name() : null);
        po.setScopeConfig(domain.getScopeConfig());
        po.setStartDate(domain.getStartDate());
        po.setEndDate(domain.getEndDate());
        po.setCycleType(domain.getCycleType() != null ? domain.getCycleType().name() : null);
        po.setCycleConfig(domain.getCycleConfig());
        po.setTimeSlots(domain.getTimeSlots());
        po.setSkipHolidays(domain.isSkipHolidays());
        po.setExcludedDates(domain.getExcludedDates());
        po.setSharedSpaceStrategy(domain.getSharedSpaceStrategy() != null ? domain.getSharedSpaceStrategy().name() : null);
        po.setScoreDistributionMode(domain.getScoreDistributionMode());
        po.setInspectorAssignmentMode(domain.getInspectorAssignmentMode() != null ? domain.getInspectorAssignmentMode().name() : null);
        po.setDefaultInspectors(domain.getDefaultInspectors());
        po.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        po.setPublishedAt(domain.getPublishedAt());
        po.setPausedAt(domain.getPausedAt());
        po.setCompletedAt(domain.getCompletedAt());
        po.setTotalTasks(domain.getTotalTasks());
        po.setCompletedTasks(domain.getCompletedTasks());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private InspectionProject toDomain(InspectionProjectPO po) {
        return InspectionProject.builder()
                .id(po.getId())
                .projectCode(po.getProjectCode())
                .projectName(po.getProjectName())
                .description(po.getDescription())
                .templateId(po.getTemplateId())
                .templateSnapshot(po.getTemplateSnapshot())
                .scopeType(ScopeType.fromCode(po.getScopeType()))
                .scopeConfig(po.getScopeConfig())
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .cycleType(CycleType.fromCode(po.getCycleType()))
                .cycleConfig(po.getCycleConfig())
                .timeSlots(po.getTimeSlots())
                .skipHolidays(po.getSkipHolidays() != null && po.getSkipHolidays())
                .excludedDates(po.getExcludedDates())
                .sharedSpaceStrategy(SharedSpaceStrategy.fromCode(po.getSharedSpaceStrategy()))
                .scoreDistributionMode(po.getScoreDistributionMode())
                .inspectorAssignmentMode(InspectorAssignmentMode.fromCode(po.getInspectorAssignmentMode()))
                .defaultInspectors(po.getDefaultInspectors())
                .status(ProjectStatus.fromCode(po.getStatus()))
                .publishedAt(po.getPublishedAt())
                .pausedAt(po.getPausedAt())
                .completedAt(po.getCompletedAt())
                .totalTasks(po.getTotalTasks())
                .completedTasks(po.getCompletedTasks())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
