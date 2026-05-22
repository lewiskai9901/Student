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
 * InspectionPlanApplicationService 测试 — 评分配置下沉 (2026-05-23):
 * 调度组评分方案预填 + ratersPerTarget 校验。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InspectionPlanApplicationService — 评分配置下沉")
class InspectionPlanApplicationServiceTest {

    @Mock InspectionPlanRepository planRepository;
    @Mock InspProjectRepository projectRepository;
    @Mock InspTaskRepository taskRepository;

    private InspectionPlanApplicationService service() {
        return new InspectionPlanApplicationService(planRepository, projectRepository, taskRepository);
    }

    private InspProject publishedProject(Long defaultProfileId) {
        return InspProject.reconstruct(InspProject.builder()
                .id(9L).projectCode("P").projectName("P")
                .status(ProjectStatus.PUBLISHED)
                .defaultScoringProfileId(defaultProfileId));
    }

    @Nested
    @DisplayName("createPlan — 评分方案预填 + 持久化")
    class CreatePlanTests {

        @Test
        @DisplayName("调度组未指定评分方案 → 预填项目 defaultScoringProfileId")
        void shouldPrefillFromProjectDefault() {
            when(projectRepository.findById(9L)).thenReturn(Optional.of(publishedProject(555L)));
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<InspectionPlan> cap = ArgumentCaptor.forClass(InspectionPlan.class);
            service().createPlan(9L, "调度组A", null, null, null,
                    "REGULAR", "DAILY", 1, null, null, false,
                    null, null, 100L);

            org.mockito.Mockito.verify(planRepository).save(cap.capture());
            assertThat(cap.getValue().getScoringProfileId()).isEqualTo(555L);
            assertThat(cap.getValue().getRatersPerTarget()).isEqualTo(1);
        }

        @Test
        @DisplayName("显式指定评分方案 → 覆盖项目默认; 显式 ratersPerTarget 被采用")
        void shouldUseExplicitScoringConfig() {
            when(projectRepository.findById(9L)).thenReturn(Optional.of(publishedProject(555L)));
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ArgumentCaptor<InspectionPlan> cap = ArgumentCaptor.forClass(InspectionPlan.class);
            service().createPlan(9L, "调度组A", null, null,
                    "[10,20,30]",  // 3 名检查员
                    "REGULAR", "DAILY", 1, null, null, false,
                    888L, 3, 100L);

            org.mockito.Mockito.verify(planRepository).save(cap.capture());
            assertThat(cap.getValue().getScoringProfileId()).isEqualTo(888L);
            assertThat(cap.getValue().getRatersPerTarget()).isEqualTo(3);
        }

        @Test
        @DisplayName("ratersPerTarget 超过调度组检查员数 → 抛 BusinessException")
        void shouldRejectRatersExceedingInspectorCount() {
            when(projectRepository.findById(9L)).thenReturn(Optional.of(publishedProject(null)));

            assertThatThrownBy(() -> service().createPlan(9L, "调度组A", null, null,
                    "[10,20]",   // 仅 2 名检查员
                    "REGULAR", "DAILY", 1, null, null, false,
                    888L, 5, 100L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("超过调度组可用检查员数");
        }

        @Test
        @DisplayName("ratersPerTarget < 1 → 抛 IllegalArgumentException")
        void shouldRejectRatersBelowOne() {
            when(projectRepository.findById(9L)).thenReturn(Optional.of(publishedProject(null)));

            assertThatThrownBy(() -> service().createPlan(9L, "调度组A", null, null, null,
                    "REGULAR", "DAILY", 1, null, null, false,
                    null, 0, 100L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ratersPerTarget");
        }

        @Test
        @DisplayName("inspectorIds 为空 (项目全员可领取) → 多人评分不在调度组层硬校验人数")
        void shouldSkipInspectorCountCheckWhenInspectorListEmpty() {
            when(projectRepository.findById(9L)).thenReturn(Optional.of(publishedProject(null)));
            when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // ratersPerTarget=5 但 inspectorIds 为空 → 不抛
            assertThatCode(() -> service().createPlan(9L, "调度组A", null, null, null,
                    "REGULAR", "DAILY", 1, null, null, false,
                    888L, 5, 100L)).doesNotThrowAnyException();
        }
    }
}
