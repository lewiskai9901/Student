package com.school.management.infrastructure.persistence.inspection.execution;

import com.school.management.domain.inspection.model.execution.*;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public List<InspProject> findByRootSectionId(Long rootSectionId) {
        // DB column is still template_id
        return mapper.findByTemplateId(rootSectionId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<InspProject> findAll() {
        return mapper.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public Map<Long, Integer> countByRootSectionIds(List<Long> rootSectionIds) {
        if (rootSectionIds == null || rootSectionIds.isEmpty()) return Collections.emptyMap();
        Map<Long, Integer> result = new HashMap<>();
        for (RootSectionUsageRow row : mapper.countByRootSectionIds(rootSectionIds)) {
            result.put(row.getRootSectionId(), row.getCnt() == null ? 0 : row.getCnt());
        }
        return result;
    }

    private InspProjectPO toPO(InspProject d) {
        InspProjectPO po = new InspProjectPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setProjectCode(d.getProjectCode());
        po.setProjectName(d.getProjectName());
        po.setOrgUnitId(d.getOrgUnitId());
        po.setTemplateId(d.getRootSectionId());    // domain rootSectionId → PO templateId
        po.setTemplateVersionId(d.getTemplateVersionId());
        po.setScoringProfileId(d.getScoringProfileId());
        po.setScopeType(d.getScopeType() != null ? d.getScopeType().name() : null);
        po.setScopeConfig(d.getScopeConfig());
        po.setStartDate(d.getStartDate());
        po.setEndDate(d.getEndDate());
        po.setAssignmentMode(d.getAssignmentMode() != null ? d.getAssignmentMode().name() : null);
        po.setReviewRequired(d.getReviewRequired());
        po.setAutoPublish(d.getAutoPublish());
        po.setEvaluationMode(d.getEvaluationMode());
        po.setMultiRaterMode(d.getMultiRaterMode());
        po.setRaterWeightBy(d.getRaterWeightBy());
        po.setConsensusThreshold(d.getConsensusThreshold());
        po.setTrendEnabled(d.getTrendEnabled());
        po.setTrendLookbackDays(d.getTrendLookbackDays());
        po.setDecayEnabled(d.getDecayEnabled());
        po.setDecayMode(d.getDecayMode());
        po.setCalibrationEnabled(d.getCalibrationEnabled());
        po.setCalibrationMethod(d.getCalibrationMethod());
        po.setSplitStrategy(d.getSplitStrategy());
        po.setScoringConfigSnapshot(d.getScoringConfigSnapshot());
        po.setMaxRejectCount(d.getMaxRejectCount());
        po.setMaxEscalationLevel(d.getMaxEscalationLevel());
        po.setAppealWindowDays(d.getAppealWindowDays());
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
                .projectCode(po.getProjectCode())
                .projectName(po.getProjectName())
                .orgUnitId(po.getOrgUnitId())
                .rootSectionId(po.getTemplateId())    // PO templateId → domain rootSectionId
                .templateVersionId(po.getTemplateVersionId())
                .scoringProfileId(po.getScoringProfileId())
                .scopeType(po.getScopeType() != null ? ScopeType.valueOf(po.getScopeType()) : null)
                .scopeConfig(po.getScopeConfig())
                .startDate(po.getStartDate())
                .endDate(po.getEndDate())
                .assignmentMode(po.getAssignmentMode() != null ? AssignmentMode.valueOf(po.getAssignmentMode()) : null)
                .reviewRequired(po.getReviewRequired())
                .autoPublish(po.getAutoPublish())
                .evaluationMode(po.getEvaluationMode())
                .multiRaterMode(po.getMultiRaterMode())
                .raterWeightBy(po.getRaterWeightBy())
                .consensusThreshold(po.getConsensusThreshold())
                .trendEnabled(po.getTrendEnabled())
                .trendLookbackDays(po.getTrendLookbackDays())
                .decayEnabled(po.getDecayEnabled())
                .decayMode(po.getDecayMode())
                .calibrationEnabled(po.getCalibrationEnabled())
                .calibrationMethod(po.getCalibrationMethod())
                .splitStrategy(po.getSplitStrategy())
                .scoringConfigSnapshot(po.getScoringConfigSnapshot())
                .maxRejectCount(po.getMaxRejectCount())
                .maxEscalationLevel(po.getMaxEscalationLevel())
                .appealWindowDays(po.getAppealWindowDays())
                .status(po.getStatus() != null ? ProjectStatus.valueOf(po.getStatus()) : null)
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt()));
    }
}
