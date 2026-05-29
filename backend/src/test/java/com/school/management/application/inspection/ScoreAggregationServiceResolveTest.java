package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.scoring.ScoringProfile;
import com.school.management.domain.inspection.repository.*;
import com.school.management.domain.inspection.service.ScoreCalculationDomainService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 评分配置解析规则测试 — 评级引擎完美架构 (2026-05-23).
 *
 * <p>新解析规则: 按 (projectId, sectionId) 在 ScoringProfileRepository 查找,
 * 调度组与项目不再持有 scoringProfileId 指针.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreAggregationService.resolveScoringProfileId — (project, section) 解析")
class ScoreAggregationServiceResolveTest {

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

    private ScoringProfile profile(Long id, Long projectId, Long sectionId) {
        return ScoringProfile.reconstruct(ScoringProfile.builder()
                .id(id).projectId(projectId).sectionId(sectionId));
    }

    @Test
    @DisplayName("(project, section) 命中 ScoringProfile → 返回 profile.id")
    void shouldReturnMatchedProfileId() {
        when(scoringProfileRepository.findByProjectIdAndSectionId(9L, 100L))
                .thenReturn(Optional.of(profile(555L, 9L, 100L)));

        assertThat(service().resolveScoringProfileId(9L, 100L)).isEqualTo(555L);
    }

    @Test
    @DisplayName("无匹配 → 返回 null")
    void shouldReturnNullWhenNoMatch() {
        when(scoringProfileRepository.findByProjectIdAndSectionId(9L, 100L))
                .thenReturn(Optional.empty());

        assertThat(service().resolveScoringProfileId(9L, 100L)).isNull();
    }

    @Test
    @DisplayName("projectId 为空 → 直接 null, 不查仓库")
    void shouldReturnNullWhenProjectIdMissing() {
        assertThat(service().resolveScoringProfileId(null, 100L)).isNull();
        verifyNoInteractions(scoringProfileRepository);
    }

    @Test
    @DisplayName("sectionId 为空 → 直接 null, 不查仓库")
    void shouldReturnNullWhenSectionIdMissing() {
        assertThat(service().resolveScoringProfileId(9L, null)).isNull();
        verifyNoInteractions(scoringProfileRepository);
    }
}
