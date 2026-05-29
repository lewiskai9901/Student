package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.execution.ScoringMode;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import com.school.management.domain.inspection.model.scoring.NormalizationMode;
import com.school.management.domain.inspection.model.scoring.NormalizeBy;
import com.school.management.domain.inspection.model.scoring.ScoringProfile;
import com.school.management.domain.inspection.repository.*;
import com.school.management.domain.inspection.service.NormalizationBasisResolver;
import com.school.management.domain.inspection.model.execution.TargetType;
import com.school.management.domain.inspection.service.ScoreCalculationDomainService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 1C 装配接通归一化 — 验证 computeScoreFields 解析出正确的 population 分母,
 * 并按 (normalizeBy + mode + 主体可解析) 同一门控构建 NormalizationConfig 传给评分引擎.
 *
 * <p>本测试聚焦"装配/接线"职责: 归一化系数的数学 (baseline/population) 由
 * {@code NormalizationCalculatorTest} 覆盖, 这里只钉死 ScoreAggregationService 是否把
 * 正确的 population 与 NormalizationConfig 喂给引擎. 用 ArgumentCaptor 捕获传入参数.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreAggregationService — Stage 1 归一化装配接通")
class ScoreAggregationServiceNormalizationTest {

    @Mock InspSubmissionRepository submissionRepository;
    @Mock SubmissionDetailRepository detailRepository;
    @Mock InspTaskRepository taskRepository;
    @Mock InspProjectRepository projectRepository;
    @Mock InspectionPlanRepository planRepository;
    @Mock ProjectScoreRepository scoreRepository;
    @Mock ScoringProfileRepository scoringProfileRepository;
    @Mock ScoreDimensionRepository dimensionRepository;
    @Mock CalculationRuleRepository ruleRepository;
    @Mock GradeBandRepository gradeBandRepository;
    @Mock TemplateSectionRepository sectionRepository;
    @Mock EscalationPolicyRepository escalationPolicyRepository;
    @Mock SubmissionObservationRepository observationRepository;
    @Mock ScoreCalculationDomainService scoreCalculationService;
    @Mock NormalizationBasisResolver normalizationBasisResolver;

    private ScoreAggregationService service() {
        return new ScoreAggregationService(
                submissionRepository, detailRepository, taskRepository, projectRepository,
                planRepository, scoreRepository, scoringProfileRepository, dimensionRepository,
                ruleRepository, gradeBandRepository, sectionRepository, escalationPolicyRepository,
                observationRepository, scoreCalculationService, normalizationBasisResolver,
                new ObjectMapper());
    }

    /** baseline=40, normalizeBy=PER_MEMBER, mode=PER_CAPITA 的 profile */
    private ScoringProfile perCapitaProfile(Long id) {
        return ScoringProfile.reconstruct(ScoringProfile.builder()
                .id(id).projectId(9L).sectionId(100L)
                .normalizeBy(NormalizeBy.PER_MEMBER)
                .normalizationMode(NormalizationMode.PER_CAPITA)
                .baselinePopulation(40));
    }

    private SubmissionDetail deductionDetail() {
        return SubmissionDetail.builder()
                .id(1L).itemCode("I1").sectionId(100L)
                .scoringMode(ScoringMode.DEDUCTION)
                .scoringConfig("{\"score\":\"10\"}")
                .build();
    }

    private ScoreCalculationDomainService.ScoreResult dummyResult() {
        return new ScoreCalculationDomainService.ScoreResult(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                null, true, Map.of(), List.of(), List.of());
    }

    @Test
    @DisplayName("PER_MEMBER + 主体可解析 → population=分母, normConfig 按 profile 构建")
    void resolvesPopulationAndBuildsNormConfig() {
        ScoringProfile profile = perCapitaProfile(555L);
        when(scoringProfileRepository.findById(555L)).thenReturn(Optional.of(profile));
        // 80 个成员的组织 → resolver 返回 80
        when(normalizationBasisResolver.resolveDenominator(TargetType.ORG, 7L, NormalizeBy.PER_MEMBER))
                .thenReturn(80);
        when(scoreCalculationService.calculate(any(), any(), any(), any(), anyList(), anyInt()))
                .thenReturn(dummyResult());

        service().computeScoreFields(555L, List.of(deductionDetail()), 100L, "ORG_UNIT", 7L);

        ArgumentCaptor<Integer> popCap = ArgumentCaptor.forClass(Integer.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ScoreCalculationDomainService.ItemScoreInput>> inputsCap =
                ArgumentCaptor.forClass(List.class);
        verify(scoreCalculationService).calculate(any(), any(), any(), any(),
                inputsCap.capture(), popCap.capture());

        // population 来自 resolver
        assertThat(popCap.getValue()).isEqualTo(80);
        // normConfig 按 profile 构建: enabled, PER_CAPITA, baseline=40
        var nc = inputsCap.getValue().get(0).getNormalizationConfig();
        assertThat(nc).isNotNull();
        assertThat(nc.isEnabled()).isTrue();
        assertThat(nc.getMode()).isEqualTo(NormalizationMode.PER_CAPITA);
        assertThat(nc.getBaselinePopulation()).isEqualTo(40);
    }

    @Test
    @DisplayName("normalizeBy=NONE → population=1, 不归一 (normConfig=null), 不调 resolver")
    void noneSkipsNormalization() {
        ScoringProfile profile = ScoringProfile.reconstruct(ScoringProfile.builder()
                .id(556L).projectId(9L).sectionId(100L)
                .normalizeBy(NormalizeBy.NONE)
                .normalizationMode(NormalizationMode.NONE));
        when(scoringProfileRepository.findById(556L)).thenReturn(Optional.of(profile));
        when(scoreCalculationService.calculate(any(), any(), any(), any(), anyList(), anyInt()))
                .thenReturn(dummyResult());

        service().computeScoreFields(556L, List.of(deductionDetail()), 100L, "ORG_UNIT", 7L);

        ArgumentCaptor<Integer> popCap = ArgumentCaptor.forClass(Integer.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ScoreCalculationDomainService.ItemScoreInput>> inputsCap =
                ArgumentCaptor.forClass(List.class);
        verify(scoreCalculationService).calculate(any(), any(), any(), any(),
                inputsCap.capture(), popCap.capture());

        assertThat(popCap.getValue()).isEqualTo(1);
        assertThat(inputsCap.getValue().get(0).getNormalizationConfig()).isNull();
        verifyNoInteractions(normalizationBasisResolver);
    }

    @Test
    @DisplayName("回归: PER_CAPITA 配置但缺主体信息 → population=1 且 normConfig=null (不会用 baseline/1 放大)")
    void missingSubjectDoesNotNormalizeWithPopulationOne() {
        // 复现耦合 bug: 若 population 与 normConfig 门控不一致, 缺主体时会 population=1 + normConfig 非空,
        // 导致引擎用 baseline/1=baseline 系数放大分数. 修复后两者必须同时关闭.
        ScoringProfile profile = perCapitaProfile(557L);
        when(scoringProfileRepository.findById(557L)).thenReturn(Optional.of(profile));
        when(scoreCalculationService.calculate(any(), any(), any(), any(), anyList(), anyInt()))
                .thenReturn(dummyResult());

        // subjectType / subjectId 为 null (如 @Deprecated 3 参路径)
        service().computeScoreFields(557L, List.of(deductionDetail()), 100L, null, null);

        ArgumentCaptor<Integer> popCap = ArgumentCaptor.forClass(Integer.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ScoreCalculationDomainService.ItemScoreInput>> inputsCap =
                ArgumentCaptor.forClass(List.class);
        verify(scoreCalculationService).calculate(any(), any(), any(), any(),
                inputsCap.capture(), popCap.capture());

        assertThat(popCap.getValue()).isEqualTo(1);
        assertThat(inputsCap.getValue().get(0).getNormalizationConfig()).isNull();
        verifyNoInteractions(normalizationBasisResolver);
    }
}
