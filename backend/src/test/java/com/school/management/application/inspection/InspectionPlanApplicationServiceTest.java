package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.execution.InspectionPlan;
import com.school.management.domain.inspection.model.execution.ProjectStatus;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import com.school.management.domain.inspection.repository.InspectionPlanRepository;
import com.school.management.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * InspectionPlanApplicationService 测试 — 评级引擎完美架构 (2026-05-23):
 * scoringProfileId 已撤销, 仅保留 ratersPerTarget 校验.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InspectionPlanApplicationService — 调度组应用服务")
class InspectionPlanApplicationServiceTest {

    @Mock InspectionPlanRepository planRepository;
    @Mock InspProjectRepository projectRepository;
    @Mock InspTaskRepository taskRepository;
    @Mock com.school.management.domain.inspection.repository.ProjectInspectorRepository projectInspectorRepository;
    @Mock com.school.management.infrastructure.persistence.inspection.execution.InspectionPlanInspectorMapper planInspectorMapper;

    private InspectionPlanApplicationService service() {
        return new InspectionPlanApplicationService(planRepository, projectRepository, taskRepository,
                projectInspectorRepository, planInspectorMapper);
    }

    private InspProject publishedProject() {
        return InspProject.reconstruct(InspProject.builder()
                .id(9L).projectCode("P").projectName("P")
                .status(ProjectStatus.PUBLISHED));
    }

    /** Stub project_inspector pool to include the given user IDs (active INSPECTOR role). */
    private void stubProjectInspectors(Long projectId, Long... userIds) {
        java.util.List<com.school.management.domain.inspection.model.execution.ProjectInspector> list =
                new java.util.ArrayList<>();
        for (Long uid : userIds) {
            list.add(com.school.management.domain.inspection.model.execution.ProjectInspector.reconstruct(
                    com.school.management.domain.inspection.model.execution.ProjectInspector.builder()
                            .id(uid).projectId(projectId).userId(uid).userName("U" + uid)
                            .role(com.school.management.domain.inspection.model.execution.InspectorRole.INSPECTOR)
                            .isActive(true)));
        }
        when(projectInspectorRepository.findByProjectId(projectId)).thenReturn(list);
    }

    @Nested
    @DisplayName("createPlan — 调度配置 + 持久化")
    class CreatePlanTests {

        @Test
        @DisplayName("默认 ratersPerTarget=1")
        void shouldDefaultRatersToOne() {
            when(projectRepository.findById(9L)).thenReturn(Optional.of(publishedProject()));
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<InspectionPlan> cap = ArgumentCaptor.forClass(InspectionPlan.class);
            service().createPlan(9L, "调度组A", null, null, null,
                    "REGULAR", "DAILY", 1, null, null, false,
                    null, null, 100L);

            org.mockito.Mockito.verify(planRepository).save(cap.capture());
            assertThat(cap.getValue().getRatersPerTarget()).isEqualTo(1);
        }

        @Test
        @DisplayName("显式 ratersPerTarget 被采用")
        void shouldUseExplicitRaters() {
            when(projectRepository.findById(9L)).thenReturn(Optional.of(publishedProject()));
            stubProjectInspectors(9L, 10L, 20L, 30L);
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<InspectionPlan> cap = ArgumentCaptor.forClass(InspectionPlan.class);
            service().createPlan(9L, "调度组A", null, null,
                    "[10,20,30]",  // 3 名检查员
                    "REGULAR", "DAILY", 1, null, null, false,
                    3, null, 100L);

            org.mockito.Mockito.verify(planRepository).save(cap.capture());
            assertThat(cap.getValue().getRatersPerTarget()).isEqualTo(3);
        }

        @Test
        @DisplayName("ratersPerTarget 超过调度组检查员数 → 抛 BusinessException")
        void shouldRejectRatersExceedingInspectorCount() {
            when(projectRepository.findById(9L)).thenReturn(Optional.of(publishedProject()));
            // 注: rater 数量检查在 inspector pool 校验之前触发, 不需要 stub projectInspectorRepository

            assertThatThrownBy(() -> service().createPlan(9L, "调度组A", null, null,
                    "[10,20]",   // 仅 2 名检查员
                    "REGULAR", "DAILY", 1, null, null, false,
                    5, null, 100L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("超过调度组可用检查员数");
        }

        @Test
        @DisplayName("ratersPerTarget < 1 → 抛 IllegalArgumentException")
        void shouldRejectRatersBelowOne() {
            when(projectRepository.findById(9L)).thenReturn(Optional.of(publishedProject()));

            assertThatThrownBy(() -> service().createPlan(9L, "调度组A", null, null, null,
                    "REGULAR", "DAILY", 1, null, null, false,
                    0, null, 100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ratersPerTarget");
        }

        @Test
        @DisplayName("inspectorIds 为空 (项目全员可领取) → 多人评分不在调度组层硬校验人数")
        void shouldSkipInspectorCountCheckWhenInspectorListEmpty() {
            when(projectRepository.findById(9L)).thenReturn(Optional.of(publishedProject()));
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> service().createPlan(9L, "调度组A", null, null, null,
                    "REGULAR", "DAILY", 1, null, null, false,
                    5, null, 100L)).doesNotThrowAnyException();
        }
    }
}
