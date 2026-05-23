package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.model.scoring.LatePolicy;
import com.school.management.domain.inspection.model.scoring.MissingPolicy;
import com.school.management.domain.inspection.model.scoring.RankDirection;
import com.school.management.domain.inspection.model.scoring.SubmissionDateField;
import com.school.management.domain.inspection.model.scoring.TriggerMode;
import com.school.management.domain.inspection.repository.IndicatorRepository;
import com.school.management.domain.inspection.repository.IndicatorScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        return createLeafIndicator(projectId, parentIndicatorId, name,
                singletonList(sourceSectionId), sourceAggregation,
                null, null, null, null,
                null, null, null,
                evaluationPeriod, gradeSchemeId, normalization, normalizationConfig,
                evaluationMethod, gradeThresholds, sortOrder);
    }

    /** 重载 — 接收 sourceSectionIds 多分区 + 评级引擎完美架构新字段. */
    @Transactional
    public Indicator createLeafIndicator(Long projectId, Long parentIndicatorId, String name,
                                         List<Long> sourceSectionIds, String sourceAggregation,
                                         TriggerMode triggerMode, Integer countThreshold,
                                         Map<Long, BigDecimal> weightsBySection,
                                         RankDirection rankDirection,
                                         MissingPolicy missingPolicy, LatePolicy latePolicy,
                                         SubmissionDateField submissionDateField,
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
        if (sourceSectionIds == null || sourceSectionIds.isEmpty()) {
            throw new IllegalArgumentException("sourceSectionIds 至少 1 个");
        }
        Indicator.validateEvaluationConfig(sourceSectionIds, triggerMode, countThreshold, weightsBySection);

        Indicator indicator = Indicator.reconstruct(Indicator.builder()
                .projectId(projectId)
                .parentIndicatorId(parentIndicatorId)
                .name(name)
                .indicatorType("LEAF")
                .sourceSectionId(sourceSectionIds.get(0))
                .sourceSectionIds(new ArrayList<>(sourceSectionIds))
                .sourceAggregation(sourceAggregation != null ? sourceAggregation : "AVG")
                .triggerMode(triggerMode != null ? triggerMode : TriggerMode.TIME_WINDOW)
                .countThreshold(countThreshold)
                .weightsBySection(weightsBySection)
                .rankDirection(rankDirection)
                .missingPolicy(missingPolicy != null ? missingPolicy : MissingPolicy.IGNORE)
                .latePolicy(latePolicy != null ? latePolicy : LatePolicy.REVISE_ORIGINAL)
                .submissionDateField(submissionDateField != null ? submissionDateField : SubmissionDateField.taskDate)
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
                .missingPolicy(MissingPolicy.fromString(missingPolicy))
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

    /** 评级引擎完美架构: 更新评级配置 (多分区组合 / 触发模式 / 政策). */
    @Transactional
    public Indicator updateEvaluationConfig(Long id,
                                            List<Long> sourceSectionIds,
                                            TriggerMode triggerMode,
                                            Integer countThreshold,
                                            Map<Long, BigDecimal> weightsBySection,
                                            RankDirection rankDirection,
                                            MissingPolicy missingPolicy,
                                            LatePolicy latePolicy,
                                            SubmissionDateField submissionDateField,
                                            Long gradeSchemeId,
                                            String evaluationPeriod) {
        Indicator indicator = indicatorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("指标不存在: " + id));
        indicator.updateEvaluationConfig(sourceSectionIds, triggerMode, countThreshold,
                weightsBySection, rankDirection, missingPolicy, latePolicy,
                submissionDateField, gradeSchemeId, evaluationPeriod);
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

    private static List<Long> singletonList(Long id) {
        List<Long> l = new ArrayList<>();
        if (id != null) l.add(id);
        return l;
    }
}
