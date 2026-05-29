package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.ProjectScore;
import com.school.management.domain.inspection.model.execution.ScoringMode;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import com.school.management.domain.inspection.model.execution.SubmissionStatus;
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
import java.time.LocalDate;
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
    @Mock OrgUnitScoreRollupService orgUnitScoreRollupService;

    private ScoreAggregationService service() {
        return new ScoreAggregationService(
                submissionRepository, detailRepository, taskRepository, projectRepository,
                planRepository, scoreRepository, scoringProfileRepository, dimensionRepository,
                ruleRepository, gradeBandRepository, sectionRepository, escalationPolicyRepository,
                observationRepository, scoreCalculationService, normalizationBasisResolver,
                orgUnitScoreRollupService, new ObjectMapper());
    }

    /** baseline=40, normalizeBy=PER_MEMBER, mode=PER_CAPITA 的 profile */
    private ScoringProfile perCapitaProfile(Long id) {
        return ScoringProfile.reconstruct(ScoringProfile.builder()
                .id(id).projectId(9L).sectionId(100L)
                .normalizeBy(NormalizeBy.PER_MEMBER)
                .normalizationMode(NormalizationMode.PER_CAPITA)
                .baselinePopulation(40)
                .normFloor(new BigDecimal("0.5"))
                .normCap(new BigDecimal("2.0")));
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
        // floor/cap 不能传反: normFloor→floorAt(下限 0.5), normCap→cappedAt(上限 2.0)
        assertThat(nc.getFloorAt()).isEqualByComparingTo("0.5");
        assertThat(nc.getCappedAt()).isEqualByComparingTo("2.0");
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

    // ========== Phase 3.4: 项目分重算后级联触发组织树 roll-up ==========

    private InspProject project() {
        return InspProject.reconstruct(InspProject.builder()
                .id(9L).projectCode("P-9").projectName("项目9"));
    }

    @Test
    @DisplayName("Phase 3.4: count>0 (有 COMPLETED 提交) → 重算 ProjectScore 后触发 rollup")
    void triggersRollupWhenCompletedSubmissionsExist() {
        LocalDate date = LocalDate.of(2026, 5, 29);
        when(projectRepository.findById(9L)).thenReturn(Optional.of(project()));

        InspTask task = InspTask.builder().id(20L).build();
        when(taskRepository.findByProjectIdAndTaskDate(9L, date)).thenReturn(List.of(task));

        InspSubmission sub = InspSubmission.builder()
                .id(30L).taskId(20L)
                .status(SubmissionStatus.COMPLETED)
                .finalScore(new BigDecimal("88"))
                .build();
        when(submissionRepository.findByTaskId(20L)).thenReturn(List.of(sub));
        when(scoreRepository.findByProjectIdAndCycleDate(9L, date)).thenReturn(Optional.empty());

        service().recomputeProjectScore(9L, date);

        // ProjectScore 已 save (count>0 路径), 随后级联 rollup
        verify(scoreRepository).save(any(ProjectScore.class));
        verify(orgUnitScoreRollupService).rollup(9L, date);
    }

    @Test
    @DisplayName("Phase 3.4: count==0 (本周期无完成提交) → 不调 rollup")
    void doesNotTriggerRollupWhenNoCompletedSubmissions() {
        LocalDate date = LocalDate.of(2026, 5, 29);
        when(projectRepository.findById(9L)).thenReturn(Optional.of(project()));
        // 无任何 task → count==0 提前 return, 不触发 rollup
        when(taskRepository.findByProjectIdAndTaskDate(9L, date)).thenReturn(List.of());

        service().recomputeProjectScore(9L, date);

        verify(orgUnitScoreRollupService, never()).rollup(anyLong(), any());
    }
}
