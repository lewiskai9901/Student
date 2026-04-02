package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.school.management.domain.inspection.model.v7.execution.InspectionPlan;
import com.school.management.domain.inspection.repository.v7.InspectionPlanRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class InspectionPlanRepositoryImpl implements InspectionPlanRepository {

    private final InspectionPlanMapper mapper;

    public InspectionPlanRepositoryImpl(InspectionPlanMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public InspectionPlan save(InspectionPlan plan) {
        InspectionPlanPO po = toPO(plan);
        if (plan.getId() == null) {
            mapper.insert(po);
            plan.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return plan;
    }

    @Override
    public Optional<InspectionPlan> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<InspectionPlan> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspectionPlan> findEnabledByProjectId(Long projectId) {
        return mapper.findEnabledByProjectId(projectId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        mapper.deleteByProjectId(projectId);
    }

    private InspectionPlanPO toPO(InspectionPlan d) {
        InspectionPlanPO po = new InspectionPlanPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setProjectId(d.getProjectId());
        po.setPlanName(d.getPlanName());
        po.setRootSectionId(d.getRootSectionId());
        po.setSectionIds(d.getSectionIds());
        po.setScheduleMode(d.getScheduleMode());
        po.setCycleType(d.getCycleType());
        po.setFrequency(d.getFrequency());
        po.setScheduleDays(d.getScheduleDays());
        po.setTimeSlots(d.getTimeSlots());
        po.setSkipHolidays(d.getSkipHolidays());
        po.setInspectorIds(d.getInspectorIds());
        po.setIsEnabled(d.getIsEnabled());
        po.setSortOrder(d.getSortOrder());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private InspectionPlan toDomain(InspectionPlanPO po) {
        return InspectionPlan.reconstruct(InspectionPlan.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .projectId(po.getProjectId())
                .planName(po.getPlanName())
                .rootSectionId(po.getRootSectionId())
                .sectionIds(po.getSectionIds())
                .scheduleMode(po.getScheduleMode())
                .cycleType(po.getCycleType())
                .frequency(po.getFrequency())
                .scheduleDays(po.getScheduleDays())
                .timeSlots(po.getTimeSlots())
                .skipHolidays(po.getSkipHolidays())
                .inspectorIds(po.getInspectorIds())
                .isEnabled(po.getIsEnabled())
                .sortOrder(po.getSortOrder())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
