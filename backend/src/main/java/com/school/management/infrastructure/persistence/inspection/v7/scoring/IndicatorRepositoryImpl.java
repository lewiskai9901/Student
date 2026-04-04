package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.school.management.domain.inspection.model.v7.scoring.Indicator;
import com.school.management.domain.inspection.repository.v7.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class IndicatorRepositoryImpl implements IndicatorRepository {

    private final IndicatorMapper mapper;

    @Override
    public Indicator save(Indicator indicator) {
        IndicatorPO po = toPO(indicator);
        if (indicator.getId() == null) {
            mapper.insert(po);
            indicator.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return indicator;
    }

    @Override
    public Optional<Indicator> findById(Long id) {
        IndicatorPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Indicator> findByProjectId(Long projectId) {
        return mapper.findByProjectId(projectId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Indicator> findByParentIndicatorId(Long parentId) {
        return mapper.findByParentIndicatorId(parentId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        mapper.softDeleteByProjectId(projectId);
    }

    private IndicatorPO toPO(Indicator domain) {
        IndicatorPO po = new IndicatorPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId());
        po.setProjectId(domain.getProjectId());
        po.setParentIndicatorId(domain.getParentIndicatorId());
        po.setName(domain.getName());
        po.setIndicatorType(domain.getIndicatorType());
        po.setSourceSectionId(domain.getSourceSectionId());
        po.setSourceAggregation(domain.getSourceAggregation());
        po.setCompositeAggregation(domain.getCompositeAggregation());
        po.setMissingPolicy(domain.getMissingPolicy());
        po.setNormalization(domain.getNormalization());
        po.setNormalizationConfig(domain.getNormalizationConfig());
        po.setEvaluationPeriod(domain.getEvaluationPeriod());
        po.setGradeSchemeId(domain.getGradeSchemeId());
        po.setEvaluationMethod(domain.getEvaluationMethod());
        po.setGradeThresholds(domain.getGradeThresholds());
        po.setSortOrder(domain.getSortOrder());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private Indicator toDomain(IndicatorPO po) {
        return Indicator.reconstruct(Indicator.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .projectId(po.getProjectId())
                .parentIndicatorId(po.getParentIndicatorId())
                .name(po.getName())
                .indicatorType(po.getIndicatorType())
                .sourceSectionId(po.getSourceSectionId())
                .sourceAggregation(po.getSourceAggregation())
                .compositeAggregation(po.getCompositeAggregation())
                .missingPolicy(po.getMissingPolicy())
                .normalization(po.getNormalization())
                .normalizationConfig(po.getNormalizationConfig())
                .evaluationPeriod(po.getEvaluationPeriod())
                .gradeSchemeId(po.getGradeSchemeId())
                .evaluationMethod(po.getEvaluationMethod())
                .gradeThresholds(po.getGradeThresholds())
                .sortOrder(po.getSortOrder())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
