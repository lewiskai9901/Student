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

    private InspProjectPO toPO(InspProject d) {
        InspProjectPO po = new InspProjectPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setProjectCode(d.getProjectCode());
        po.setProjectName(d.getProjectName());
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
                .status(po.getStatus() != null ? ProjectStatus.valueOf(po.getStatus()) : null)
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt()));
    }
}
