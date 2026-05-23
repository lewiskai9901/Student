package com.school.management.application.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.event.ProjectCreatedEvent;
import com.school.management.domain.inspection.exception.LastLeadRemovalException;
import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.execution.InspectorRole;
import com.school.management.domain.inspection.model.execution.ProjectInspector;
import com.school.management.domain.inspection.repository.*;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Phase A LEAD 语义化测试: 验证
 * 1. createProject → 发布 ProjectCreatedEvent (id 不为空)
 * 2. removeInspector → 仅剩 1 LEAD 时拒绝 (LastLeadRemovalException)
 * 3. removeInspector → 多个 LEAD 时允许
 * 4. removeInspector → 非 LEAD 角色 不受不变量限制
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InspProject LEAD 语义化")
class InspProjectLeadSemanticsTest {

    @Mock InspProjectRepository projectRepository;
    @Mock ProjectInspectorRepository inspectorRepository;
    @Mock ProjectScoreRepository scoreRepository;
    @Mock SpringDomainEventPublisher eventPublisher;
    @Mock ScoringProfileRepository scoringProfileRepository;
    @Mock ScoreDimensionRepository scoreDimensionRepository;
    @Mock GradeBandRepository gradeBandRepository;
    @Mock CalculationRuleRepository calculationRuleRepository;
    @Mock TargetPopulationService targetPopulationService;
    @Mock TemplateSectionRepository templateSectionRepository;
    @Mock TemplateVersionRepository templateVersionRepository;
    @Mock InspectionAuditLogger auditLogger;
    @Mock InspTaskRepository taskRepository;
    @Mock InspectionPlanRepository inspectionPlanRepository;
    @Mock IndicatorRepository indicatorRepository;
    @Mock ScoringProfileApplicationService scoringProfileService;

    ObjectMapper objectMapper = new ObjectMapper();

    InspProjectApplicationService service;

    @BeforeEach
    void setUp() {
        service = new InspProjectApplicationService(
                projectRepository, inspectorRepository, scoreRepository,
                eventPublisher, scoringProfileRepository,
                scoreDimensionRepository, gradeBandRepository, calculationRuleRepository,
                targetPopulationService,
                objectMapper, templateSectionRepository, templateVersionRepository,
                auditLogger, taskRepository,
                inspectionPlanRepository, indicatorRepository, scoringProfileService);
    }

    @Test
    @DisplayName("createProject 应该发布 ProjectCreatedEvent 携带正确的 projectId / createdBy / orgUnitId")
    void createProject_publishesProjectCreatedEvent() {
        Long createdBy = 100L;
        Long orgUnitId = 200L;
        // mock save 后 id 被填充
        when(projectRepository.save(any(InspProject.class))).thenAnswer(inv -> {
            InspProject p = inv.getArgument(0);
            p.setId(999L);
            return p;
        });

        InspProject result = service.createProject("Test Project", null,
                LocalDate.now(), orgUnitId, createdBy);

        assertThat(result.getId()).isEqualTo(999L);

        // 验证 publishAll 被调用, 且其中包含 ProjectCreatedEvent
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.school.management.domain.shared.event.DomainEvent>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(eventPublisher).publishAll(captor.capture());

        List<com.school.management.domain.shared.event.DomainEvent> events = captor.getValue();
        ProjectCreatedEvent created = events.stream()
                .filter(e -> e instanceof ProjectCreatedEvent)
                .map(e -> (ProjectCreatedEvent) e)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected ProjectCreatedEvent to be published"));

        assertThat(created.getProjectId()).isEqualTo(999L);
        assertThat(created.getCreatedBy()).isEqualTo(createdBy);
        assertThat(created.getOrgUnitId()).isEqualTo(orgUnitId);
        assertThat(created.getProjectName()).isEqualTo("Test Project");
    }

    @Test
    @DisplayName("removeInspector 移除最后一个 LEAD 时抛出 LastLeadRemovalException")
    void removeInspector_lastLead_throws() {
        ProjectInspector onlyLead = ProjectInspector.reconstruct(
                ProjectInspector.builder()
                        .id(1L)
                        .projectId(10L)
                        .userId(100L)
                        .userName("Alice")
                        .role(InspectorRole.LEAD)
                        .isActive(true));

        when(inspectorRepository.findById(1L)).thenReturn(Optional.of(onlyLead));
        when(inspectorRepository.countActiveByProjectIdAndRole(10L, InspectorRole.LEAD)).thenReturn(1);

        assertThatThrownBy(() -> service.removeInspector(1L))
                .isInstanceOf(LastLeadRemovalException.class)
                .hasMessageContaining("10");

        verify(inspectorRepository, never()).deleteById(1L);
    }

    @Test
    @DisplayName("removeInspector 多 LEAD 时允许移除其中一个")
    void removeInspector_multipleLeads_allowed() {
        ProjectInspector lead = ProjectInspector.reconstruct(
                ProjectInspector.builder()
                        .id(2L)
                        .projectId(10L)
                        .userId(200L)
                        .userName("Bob")
                        .role(InspectorRole.LEAD)
                        .isActive(true));

        when(inspectorRepository.findById(2L)).thenReturn(Optional.of(lead));
        when(inspectorRepository.countActiveByProjectIdAndRole(10L, InspectorRole.LEAD)).thenReturn(2);

        service.removeInspector(2L);

        verify(inspectorRepository).deleteById(2L);
    }

    @Test
    @DisplayName("removeInspector 非 LEAD 角色不受不变量限制")
    void removeInspector_nonLead_bypassesInvariant() {
        ProjectInspector inspector = ProjectInspector.reconstruct(
                ProjectInspector.builder()
                        .id(3L)
                        .projectId(10L)
                        .userId(300L)
                        .userName("Carol")
                        .role(InspectorRole.INSPECTOR)
                        .isActive(true));

        when(inspectorRepository.findById(3L)).thenReturn(Optional.of(inspector));

        service.removeInspector(3L);

        verify(inspectorRepository).deleteById(3L);
        verify(inspectorRepository, never()).countActiveByProjectIdAndRole(eq(10L), any(InspectorRole.class));
    }

    @Test
    @DisplayName("removeInspector 幂等: id 已不存在时静默跳过")
    void removeInspector_notFound_silentSkip() {
        when(inspectorRepository.findById(99L)).thenReturn(Optional.empty());

        service.removeInspector(99L);

        verify(inspectorRepository, never()).deleteById(any());
    }
}
