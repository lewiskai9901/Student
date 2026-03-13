package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.domain.inspection.repository.v7.InspProjectRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InspProjectRepositoryImpl implements InspProjectRepository {

    private final InspProjectMapper mapper;

    public InspProjectRepositoryImpl(InspProjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InspProject save(InspProject project) {
        InspProjectPO po = toPO(project);
        if (project.getId() == null) {
            mapper.insert(po);
            project.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return project;
    }

    @Override
    public Optional<InspProject> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<InspProject> findByProjectCode(String projectCode) {
        return Optional.ofNullable(mapper.findByProjectCode(projectCode)).map(this::toDomain);
    }

    @Override
    public List<InspProject> findByStatus(ProjectStatus status) {
        return mapper.findByStatus(status.name()).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspProject> findByTemplateId(Long templateId) {
        return mapper.findByTemplateId(templateId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspProject> findByParentProjectId(Long parentProjectId) {
        return mapper.findByParentProjectId(parentProjectId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspProject> findAll() {
        return mapper.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private InspProjectPO toPO(InspProject d) {
        InspProjectPO po = new InspProjectPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setParentProjectId(d.getParentProjectId());
        po.setProjectCode(d.getProjectCode());
        po.setProjectName(d.getProjectName());
        po.setTemplateId(d.getTemplateId());
        po.setTemplateVersionId(d.getTemplateVersionId());
        po.setScoringProfileId(d.getScoringProfileId());
        po.setScopeType(d.getScopeType() != null ? d.getScopeType().name() : null);
        po.setScopeConfig(d.getScopeConfig());
        po.setTargetType(d.getTargetType() != null ? d.getTargetType().name() : null);
        po.setStartDate(d.getStartDate());
        po.setEndDate(d.getEndDate());
        po.setCycleType(d.getCycleType() != null ? d.getCycleType().name() : null);
        po.setCycleConfig(d.getCycleConfig());
        po.setTimeSlots(d.getTimeSlots());
        po.setSkipHolidays(d.getSkipHolidays());
        po.setHolidayCalendarId(d.getHolidayCalendarId());
        po.setExcludedDates(d.getExcludedDates());
        po.setAssignmentMode(d.getAssignmentMode() != null ? d.getAssignmentMode().name() : null);
        po.setReviewRequired(d.getReviewRequired());
        po.setAutoPublish(d.getAutoPublish());
        po.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedBy(d.getUpdatedBy());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private InspProject toDomain(InspProjectPO po) {
        return InspProject.reconstruct(InspProject.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .parentProjectId(po.getParentProjectId())
                .projectCode(po.getProjectCode())
                .projectName(po.getProjectName())
                .templateId(po.getTemplateId())
                .templateVersionId(po.getTemplateVersionId())
                .scoringProfileId(po.getScoringProfileId())
                .scopeType(po.getScopeType() != null ? ScopeType.valueOf(po.getScopeType()) : null)
                .scopeConfig(po.getScopeConfig())
                .targetType(po.getTargetType() != null ? TargetType.valueOf(po.getTargetType()) : null)
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .cycleType(po.getCycleType() != null ? CycleType.valueOf(po.getCycleType()) : null)
                .cycleConfig(po.getCycleConfig())
                .timeSlots(po.getTimeSlots())
                .skipHolidays(po.getSkipHolidays())
                .holidayCalendarId(po.getHolidayCalendarId())
                .excludedDates(po.getExcludedDates())
                .assignmentMode(po.getAssignmentMode() != null ? AssignmentMode.valueOf(po.getAssignmentMode()) : null)
                .reviewRequired(po.getReviewRequired())
                .autoPublish(po.getAutoPublish())
                .status(po.getStatus() != null ? ProjectStatus.valueOf(po.getStatus()) : null)
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt()));
    }
}
