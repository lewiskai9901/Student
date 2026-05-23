package com.school.management.application.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.execution.ProjectStatus;
import com.school.management.domain.inspection.model.scoring.ScoringProfile;
import com.school.management.domain.inspection.repository.CalculationRuleRepository;
import com.school.management.domain.inspection.repository.GradeBandRepository;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import com.school.management.domain.inspection.repository.ProjectInspectorRepository;
import com.school.management.domain.inspection.repository.ProjectScoreRepository;
import com.school.management.domain.inspection.repository.ScoreDimensionRepository;
import com.school.management.domain.inspection.repository.ScoringProfileRepository;
import com.school.management.domain.inspection.repository.TemplateSectionRepository;
import com.school.management.domain.inspection.repository.TemplateVersionRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 InspProjectApplicationService.deleteProject 触发评分配置级联清理.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InspProjectApplicationService.deleteProject 级联清理 ScoringProfile")
class InspProjectApplicationServiceDeleteCascadeTest {

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

    InspProjectApplicationService service;

    @BeforeEach
    void setUp() {
        service = new InspProjectApplicationService(
                projectRepository, inspectorRepository, scoreRepository,
                eventPublisher, scoringProfileRepository,
                scoreDimensionRepository, gradeBandRepository, calculationRuleRepository,
                targetPopulationService, new ObjectMapper(),
                templateSectionRepository, templateVersionRepository,
                auditLogger, taskRepository);
    }

    @Test
    @DisplayName("删除项目: 先清子实体, 再 deleteByProjectId, 最后删项目本身")
    void shouldCascadeDeleteOwnedScoringProfiles() {
        InspProject project = InspProject.builder()
                .id(700L).projectCode("PRJ-X").projectName("T")
                .rootSectionId(100L).status(ProjectStatus.DRAFT).build();
        when(projectRepository.findById(700L)).thenReturn(Optional.of(project));
        when(taskRepository.findByProjectId(700L)).thenReturn(List.of()); // 无任务可删

        ScoringProfile owned1 = ScoringProfile.reconstruct(ScoringProfile.builder()
                .id(500L).sectionId(100L).projectId(700L));
        ScoringProfile owned2 = ScoringProfile.reconstruct(ScoringProfile.builder()
                .id(501L).sectionId(200L).projectId(700L));
        when(scoringProfileRepository.findByProjectId(700L)).thenReturn(List.of(owned1, owned2));
        when(scoringProfileRepository.deleteByProjectId(700L)).thenReturn(2);

        service.deleteProject(700L);

        // 子实体清理 (针对每个 owned profile)
        verify(scoreDimensionRepository).deleteByScoringProfileId(500L);
        verify(scoreDimensionRepository).deleteByScoringProfileId(501L);
        verify(calculationRuleRepository).deleteByScoringProfileId(500L);
        verify(calculationRuleRepository).deleteByScoringProfileId(501L);
        verify(gradeBandRepository).deleteByScoringProfileId(500L);
        verify(gradeBandRepository).deleteByScoringProfileId(501L);

        // 顺序: 子 → profile → project
        var io = inOrder(scoreDimensionRepository, scoringProfileRepository, projectRepository);
        io.verify(scoreDimensionRepository).deleteByScoringProfileId(500L);
        io.verify(scoringProfileRepository).deleteByProjectId(700L);
        io.verify(projectRepository).deleteById(700L);
    }

    @Test
    @DisplayName("项目无 owned profile: 调用 deleteByProjectId 返回 0 仍正常删项目")
    void shouldDeleteProjectWithNoOwnedProfile() {
        InspProject project = InspProject.builder()
                .id(700L).projectCode("PRJ-X").projectName("T")
                .rootSectionId(100L).status(ProjectStatus.DRAFT).build();
        when(projectRepository.findById(700L)).thenReturn(Optional.of(project));
        when(taskRepository.findByProjectId(700L)).thenReturn(List.of());
        when(scoringProfileRepository.findByProjectId(700L)).thenReturn(List.of());
        when(scoringProfileRepository.deleteByProjectId(700L)).thenReturn(0);

        service.deleteProject(700L);

        verify(scoringProfileRepository).deleteByProjectId(eq(700L));
        verify(projectRepository).deleteById(700L);
    }
}
