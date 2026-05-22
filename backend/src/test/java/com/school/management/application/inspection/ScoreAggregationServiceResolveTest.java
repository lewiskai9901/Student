package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.InspectionPlan;
import com.school.management.domain.inspection.model.execution.TaskStatus;
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
 * 评分配置解析规则测试 (评分配置下沉 2026-05-23).
 *
 * <p>解析规则: task.planId 非空 → 调度组 scoringProfileId; 否则/调度组未配 → 项目 defaultScoringProfileId。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreAggregationService.resolveScoringProfileId — 评分配置解析规则")
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

    private ScoreAggregationService service() {
        return new ScoreAggregationService(
                submissionRepository, detailRepository, taskRepository, projectRepository,
                planRepository, scoreRepository, scoringProfileRepository, dimensionRepository,
                ruleRepository, gradeBandRepository, sectionRepository, escalationPolicyRepository,
                observationRepository, scoreCalculationService, new ObjectMapper());
    }

    private InspProject project(Long defaultProfileId) {
        return InspProject.reconstruct(InspProject.builder()
                .id(9L).projectCode("P").projectName("P")
                .defaultScoringProfileId(defaultProfileId));
    }

    private InspTask taskWithPlan(Long planId) {
        return InspTask.reconstruct(InspTask.builder()
                .id(7L).taskCode("TSK").projectId(9L).status(TaskStatus.PENDING)
                .inspectionPlanId(planId));
    }

    private InspectionPlan plan(Long scoringProfileId) {
        return InspectionPlan.reconstruct(InspectionPlan.builder()
                .id(3L).projectId(9L).planName("调度组A")
                .scoringProfileId(scoringProfileId).ratersPerTarget(1));
    }

    @Test
    @DisplayName("分支1: 任务有 planId 且调度组配了评分方案 → 用调度组 scoringProfileId")
    void shouldUsePlanScoringProfileWhenPlanIdPresent() {
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan(888L)));

        Long resolved = service().resolveScoringProfileId(taskWithPlan(3L), project(111L));

        assertThat(resolved).isEqualTo(888L);
        verify(planRepository).findById(3L);
    }

    @Test
    @DisplayName("分支2: 任务无 planId → 回退项目 defaultScoringProfileId")
    void shouldFallbackToProjectDefaultWhenNoPlanId() {
        Long resolved = service().resolveScoringProfileId(taskWithPlan(null), project(111L));

        assertThat(resolved).isEqualTo(111L);
        verifyNoInteractions(planRepository);
    }

    @Test
    @DisplayName("分支2b: 任务有 planId 但调度组未配评分方案 → 回退项目默认")
    void shouldFallbackToProjectDefaultWhenPlanHasNoProfile() {
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan(null)));

        Long resolved = service().resolveScoringProfileId(taskWithPlan(3L), project(111L));

        assertThat(resolved).isEqualTo(111L);
    }

    @Test
    @DisplayName("task 为 null → 直接回退项目默认")
    void shouldFallbackToProjectDefaultWhenTaskNull() {
        assertThat(service().resolveScoringProfileId(null, project(111L))).isEqualTo(111L);
    }

    @Test
    @DisplayName("调度组与项目都未配评分方案 → 返回 null (走简单汇总路径)")
    void shouldReturnNullWhenNothingConfigured() {
        assertThat(service().resolveScoringProfileId(taskWithPlan(null), project(null))).isNull();
    }
}
