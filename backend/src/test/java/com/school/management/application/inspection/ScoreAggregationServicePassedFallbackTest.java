package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import com.school.management.domain.inspection.model.scoring.GradeBand;
import com.school.management.domain.inspection.model.scoring.ScoringProfile;
import com.school.management.domain.inspection.repository.*;
import com.school.management.domain.inspection.service.ScoreCalculationDomainService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 规模公平性 Phase 5 — 无评分配置回退路径的及格语义测试.
 *
 * <p>系统为等级制 (gradeBand 优/良/合格), 没有配置化的及格线; 回退路径不再臆造
 * 60 分硬编码及格线. grade 仍按 gradeBand 算, passed 在无配置及格线时置 null (未知),
 * 与正路 {@code isPassed} 不写死 60 的理念一致.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreAggregationService.computeScoreFields — 回退路径不臆造 60 及格线")
class ScoreAggregationServicePassedFallbackTest {

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
    @Mock com.school.management.domain.inspection.service.NormalizationBasisResolver normalizationBasisResolver;
    @Mock OrgUnitScoreRollupService orgUnitScoreRollupService;

    private ScoreAggregationService service() {
        return new ScoreAggregationService(
                submissionRepository, detailRepository, taskRepository, projectRepository,
                planRepository, scoreRepository, scoringProfileRepository, dimensionRepository,
                ruleRepository, gradeBandRepository, sectionRepository, escalationPolicyRepository,
                observationRepository, scoreCalculationService, normalizationBasisResolver,
                orgUnitScoreRollupService, new ObjectMapper());
    }

    private SubmissionDetail detail(BigDecimal score, BigDecimal maxScore) {
        return SubmissionDetail.builder()
                .score(score)
                .scoringConfig("{\"maxScore\":" + maxScore.toPlainString() + "}")
                .build();
    }

    /**
     * profileId==null + 分区 ScoringProfile/GradeBand 命中.
     * sum=80, maxPossible=100 → pct=80 (旧逻辑会判 passed=true);
     * 断言 grade 正确, passed==null (不再硬判 60).
     */
    @Test
    @DisplayName("分区 GradeBand 命中: grade 命中, passed 为 null 而非旧 60 判定")
    void gradeBandHit_passedIsNull_notHardcoded60() {
        Long sectionId = 100L;
        ScoringProfile sectionProfile = ScoringProfile.reconstruct(
                ScoringProfile.builder().id(7L).sectionId(sectionId));
        GradeBand band = GradeBand.reconstruct(GradeBand.builder()
                .id(1L).scoringProfileId(7L)
                .gradeCode("A").gradeName("优秀")
                .minScore(new BigDecimal("70")).maxScore(new BigDecimal("100")));

        when(scoringProfileRepository.findBySectionId(sectionId))
                .thenReturn(Optional.of(sectionProfile));
        when(gradeBandRepository.findByScoringProfileId(7L))
                .thenReturn(List.of(band));

        List<SubmissionDetail> details = List.of(
                detail(new BigDecimal("80"), new BigDecimal("100")));

        ScoreAggregationService.ScoreFields fields =
                service().computeScoreFields(null, details, sectionId, null, null);

        assertThat(fields.grade).isEqualTo("优秀");
        // 旧逻辑: pct=80 >= 60 → passed=true. 现在不臆造 60 → passed=null.
        assertThat(fields.passed).isNull();
        assertThat(fields.finalScore).isEqualByComparingTo("80");
    }

    /**
     * 回退到分区 scoringConfig JSON 的 gradeBands 路径同样不臆造 60.
     */
    @Test
    @DisplayName("scoringConfig JSON gradeBands 命中: passed 为 null 而非旧 60 判定")
    void jsonGradeBandHit_passedIsNull_notHardcoded60() {
        Long sectionId = 200L;
        // 无分区级 ScoringProfile → 走 scoringConfig JSON 回退
        when(scoringProfileRepository.findBySectionId(sectionId))
                .thenReturn(Optional.empty());

        com.school.management.domain.inspection.model.template.TemplateSection section =
                mock(com.school.management.domain.inspection.model.template.TemplateSection.class);
        when(section.getScoringConfig()).thenReturn(
                "{\"gradeBands\":[{\"label\":\"合格\",\"minPercent\":60,\"maxPercent\":100}]}");
        when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));

        List<SubmissionDetail> details = List.of(
                detail(new BigDecimal("90"), new BigDecimal("100")));

        ScoreAggregationService.ScoreFields fields =
                service().computeScoreFields(null, details, sectionId, null, null);

        assertThat(fields.grade).isEqualTo("合格");
        // 旧逻辑: pct=90 >= 60 → passed=true. 现在 → passed=null.
        assertThat(fields.passed).isNull();
    }
}
