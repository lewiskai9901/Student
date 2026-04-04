package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.scoring.Indicator;
import com.school.management.domain.inspection.repository.v7.IndicatorRepository;
import com.school.management.domain.inspection.repository.v7.IndicatorScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorApplicationService {

    private final IndicatorRepository indicatorRepository;
    private final IndicatorScoreRepository scoreRepository;

    // ── CRUD ────────────────────────────────────────────────────

    public List<Indicator> getIndicatorTree(Long projectId) {
        return indicatorRepository.findByProjectId(projectId);
    }

    public Indicator getIndicator(Long id) {
        return indicatorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("指标不存在: " + id));
    }

    @Transactional
    public Indicator createLeafIndicator(Long projectId, Long parentIndicatorId, String name,
                                         Long sourceSectionId, String sourceAggregation,
                                         String evaluationPeriod,
                                         Long gradeSchemeId, String normalization,
                                         String normalizationConfig,
                                         String evaluationMethod, String gradeThresholds,
                                         Integer sortOrder) {
        if (parentIndicatorId != null) {
            Indicator parent = indicatorRepository.findById(parentIndicatorId)
                    .orElseThrow(() -> new IllegalArgumentException("父指标不存在: " + parentIndicatorId));
            if (!parent.getProjectId().equals(projectId)) {
                throw new IllegalArgumentException("父指标不属于该项目");
            }
        }

        Indicator indicator = Indicator.reconstruct(Indicator.builder()
                .projectId(projectId)
                .parentIndicatorId(parentIndicatorId)
                .name(name)
                .indicatorType("LEAF")
                .sourceSectionId(sourceSectionId)
                .sourceAggregation(sourceAggregation != null ? sourceAggregation : "AVG")
                .normalization(normalization)
                .normalizationConfig(normalizationConfig)
                .evaluationPeriod(evaluationPeriod != null ? evaluationPeriod : "PER_TASK")
                .gradeSchemeId(gradeSchemeId)
                .evaluationMethod(evaluationMethod)
                .gradeThresholds(gradeThresholds)
                .sortOrder(sortOrder != null ? sortOrder : 0));
        return indicatorRepository.save(indicator);
    }

    @Transactional
    public Indicator createCompositeIndicator(Long projectId, Long parentIndicatorId, String name,
                                              String compositeAggregation, String missingPolicy,
                                              String evaluationPeriod,
                                              Long gradeSchemeId, String normalization,
                                              String normalizationConfig,
                                              String evaluationMethod, String gradeThresholds,
                                              Integer sortOrder) {
        if (parentIndicatorId != null) {
            Indicator parent = indicatorRepository.findById(parentIndicatorId)
                    .orElseThrow(() -> new IllegalArgumentException("父指标不存在: " + parentIndicatorId));
            if (!parent.getProjectId().equals(projectId)) {
                throw new IllegalArgumentException("父指标不属于该项目");
            }
        }

        Indicator indicator = Indicator.reconstruct(Indicator.builder()
                .projectId(projectId)
                .parentIndicatorId(parentIndicatorId)
                .name(name)
                .indicatorType("COMPOSITE")
                .compositeAggregation(compositeAggregation != null ? compositeAggregation : "WEIGHTED_AVG")
                .missingPolicy(missingPolicy != null ? missingPolicy : "SKIP")
                .normalization(normalization)
                .normalizationConfig(normalizationConfig)
                .evaluationPeriod(evaluationPeriod != null ? evaluationPeriod : "WEEKLY")
                .gradeSchemeId(gradeSchemeId)
                .evaluationMethod(evaluationMethod)
                .gradeThresholds(gradeThresholds)
                .sortOrder(sortOrder != null ? sortOrder : 0));
        return indicatorRepository.save(indicator);
    }

    @Transactional
    public Indicator updateIndicator(Long id, String name, String evaluationPeriod,
                                     Long gradeSchemeId, Long sourceSectionId, String sourceAggregation,
                                     String compositeAggregation, String missingPolicy,
                                     String normalization, String normalizationConfig,
                                     String evaluationMethod, String gradeThresholds,
                                     Integer sortOrder) {
        Indicator indicator = indicatorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("指标不存在: " + id));
        indicator.update(name, evaluationPeriod, gradeSchemeId,
                sourceSectionId, sourceAggregation, compositeAggregation, missingPolicy,
                normalization, normalizationConfig,
                evaluationMethod, gradeThresholds, sortOrder);
        return indicatorRepository.save(indicator);
    }

    @Transactional
    public void deleteIndicator(Long id) {
        // Recursively delete children first
        List<Indicator> children = indicatorRepository.findByParentIndicatorId(id);
        for (Indicator child : children) {
            deleteIndicator(child.getId());
        }
        // Delete scores for this indicator
        scoreRepository.deleteByIndicatorId(id);
        indicatorRepository.deleteById(id);
    }

}
