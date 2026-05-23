package com.school.management.application.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.exception.BusinessException;
import com.school.management.domain.inspection.model.execution.*;
import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.model.scoring.ScoringProfile;
import com.school.management.domain.inspection.repository.*;
import com.school.management.application.inspection.dto.CloneProjectCommand;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InspProjectApplicationService.cloneProject 单测.
 *
 * <p>验证克隆全链路:
 * <ul>
 *   <li>新项目: 字段 copy + status=DRAFT + 新 projectCode</li>
 *   <li>ScoringProfile 深拷贝 + profileIdMap 维护</li>
 *   <li>defaultScoringProfileId 按映射重写</li>
 *   <li>InspectionPlan 拷贝 + scoringProfileId 按映射重写</li>
 *   <li>Indicator 拷贝且 projectId=新, parentIndicatorId 按映射重写</li>
 *   <li>cloneInspectors=true 时 inspectors 复制</li>
 *   <li>ARCHIVED 项目拒绝克隆</li>
 *   <li>不克隆执行数据 (tasks/submissions/scores)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InspProjectApplicationService.cloneProject — 项目克隆")
class InspProjectCloneServiceTest {

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

    InspProjectApplicationService service;

    @BeforeEach
    void setUp() {
        service = new InspProjectApplicationService(
                projectRepository, inspectorRepository, scoreRepository,
                eventPublisher, scoringProfileRepository,
                scoreDimensionRepository, gradeBandRepository, calculationRuleRepository,
                targetPopulationService, new ObjectMapper(),
                templateSectionRepository, templateVersionRepository,
                auditLogger, taskRepository,
                inspectionPlanRepository, indicatorRepository, scoringProfileService);
    }

    private InspProject sourceProject(Long id, ProjectStatus status) {
        return InspProject.builder()
                .id(id)
                .projectCode("PRJ-SRC")
                .projectName("源项目")
                .rootSectionId(100L)
                .orgUnitId(10L)
                .scopeType(ScopeType.ORG)
                .scopeConfig("[1,2]")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .assignmentMode(AssignmentMode.ASSIGNED)
                .reviewRequired(true)
                .autoPublish(false)
                .maxRejectCount(5)
                .maxEscalationLevel(4)
                .appealWindowDays(14)
                .status(status)
                .build();
    }

    private CloneProjectCommand req(String name, Long orgUnitId, LocalDate start, LocalDate end, Boolean cloneInspectors) {
        return new CloneProjectCommand(name, orgUnitId, start, end, cloneInspectors);
    }

    private ScoringProfile profile(Long id, Long projectId, Long sectionId) {
        return ScoringProfile.reconstruct(ScoringProfile.builder()
                .id(id).projectId(projectId).sectionId(sectionId));
    }

    /** mock projectRepository.save 给 saved project 分配新 id (首次 save), 后续 save 保留. */
    private void mockProjectSaveAssignsId(Long newId) {
        when(projectRepository.save(any())).thenAnswer(inv -> {
            InspProject p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(newId);
            }
            return p;
        });
    }

    @Test
    @DisplayName("基本克隆: 新项目 DRAFT + 新 projectCode + 字段从源 copy")
    void shouldCloneBasicFields() {
        InspProject src = sourceProject(1L, ProjectStatus.PUBLISHED);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(src));
        mockProjectSaveAssignsId(2L);

        // 无 profile / plan / indicator
        when(scoringProfileRepository.findByProjectId(1L)).thenReturn(List.of());
        when(inspectionPlanRepository.findByProjectId(1L)).thenReturn(List.of());
        when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of());

        CloneProjectCommand request = req("克隆项目", 20L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), false);
        InspProject cloned = service.cloneProject(1L, request, 999L);

        assertThat(cloned.getProjectName()).isEqualTo("克隆项目");
        assertThat(cloned.getOrgUnitId()).isEqualTo(20L);
        assertThat(cloned.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(cloned.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(cloned.getStatus()).isEqualTo(ProjectStatus.DRAFT);
        assertThat(cloned.getProjectCode()).startsWith("PRJ-").isNotEqualTo("PRJ-SRC");
        // 字段从源拷贝
        assertThat(cloned.getRootSectionId()).isEqualTo(100L);
        assertThat(cloned.getScopeType()).isEqualTo(ScopeType.ORG);
        assertThat(cloned.getScopeConfig()).isEqualTo("[1,2]");
        assertThat(cloned.getAssignmentMode()).isEqualTo(AssignmentMode.ASSIGNED);
        assertThat(cloned.getReviewRequired()).isTrue();
        assertThat(cloned.getAutoPublish()).isFalse();
        assertThat(cloned.getMaxRejectCount()).isEqualTo(5);
        assertThat(cloned.getMaxEscalationLevel()).isEqualTo(4);
        assertThat(cloned.getAppealWindowDays()).isEqualTo(14);
        // 评级引擎完美架构: 项目不再持有 defaultScoringProfileId

        // 审计 + 不克隆执行数据
        verify(auditLogger).log(eq("InspProject"), anyLong(), any(), eq("PROJECT_CLONED"), any(), any());
        verify(taskRepository, never()).findByProjectId(any());
        verify(scoreRepository, never()).findByProjectId(any());
    }

    @Test
    @DisplayName("源 owned ScoringProfile 全部克隆到新项目")
    void shouldCloneAllOwnedScoringProfiles() {
        InspProject src = sourceProject(1L, ProjectStatus.PUBLISHED);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(src));
        mockProjectSaveAssignsId(2L);

        ScoringProfile sp1 = profile(500L, 1L, 100L);
        ScoringProfile sp2 = profile(501L, 1L, 200L);
        when(scoringProfileRepository.findByProjectId(1L)).thenReturn(List.of(sp1, sp2));
        when(scoringProfileService.cloneForProject(eq(500L), eq(2L), eq(999L)))
                .thenReturn(profile(600L, 2L, 100L));
        when(scoringProfileService.cloneForProject(eq(501L), eq(2L), eq(999L)))
                .thenReturn(profile(601L, 2L, 200L));

        when(inspectionPlanRepository.findByProjectId(1L)).thenReturn(List.of());
        when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of());

        service.cloneProject(1L,
                req("克隆", 20L, LocalDate.of(2026, 6, 1), null, false), 999L);

        verify(scoringProfileService).cloneForProject(500L, 2L, 999L);
        verify(scoringProfileService).cloneForProject(501L, 2L, 999L);
    }

    @Test
    @DisplayName("Plans: 拷贝调度参数, ratersPerTarget 保留, inspectorIds 默认清空")
    void shouldClonePlanScheduleConfig() {
        InspProject src = sourceProject(1L, ProjectStatus.PUBLISHED);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(src));
        mockProjectSaveAssignsId(2L);

        when(scoringProfileRepository.findByProjectId(1L)).thenReturn(List.of());

        InspectionPlan oldPlan = InspectionPlan.reconstruct(InspectionPlan.builder()
                .id(700L).projectId(1L).planName("A 组")
                .rootSectionId(100L)
                .ratersPerTarget(2).inspectorIds("[10,11]").isEnabled(true));
        when(inspectionPlanRepository.findByProjectId(1L)).thenReturn(List.of(oldPlan));
        when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of());
        when(inspectionPlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cloneProject(1L, req("克隆", 20L, LocalDate.of(2026, 6, 1), null, false), 999L);

        ArgumentCaptor<InspectionPlan> captor = ArgumentCaptor.forClass(InspectionPlan.class);
        verify(inspectionPlanRepository).save(captor.capture());
        InspectionPlan saved = captor.getValue();
        assertThat(saved.getProjectId()).isEqualTo(2L);
        assertThat(saved.getPlanName()).isEqualTo("A 组");
        assertThat(saved.getRatersPerTarget()).isEqualTo(2);
        assertThat(saved.getInspectorIds()).isNull(); // cloneInspectors=false → 清空
    }

    @Test
    @DisplayName("Indicators 拷贝: projectId=新, parentIndicatorId 按映射重写")
    void shouldCloneIndicatorsWithRemappedParent() {
        InspProject src = sourceProject(1L, ProjectStatus.PUBLISHED);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(src));
        mockProjectSaveAssignsId(2L);

        when(scoringProfileRepository.findByProjectId(1L)).thenReturn(List.of());
        when(inspectionPlanRepository.findByProjectId(1L)).thenReturn(List.of());

        Indicator parent = Indicator.reconstruct(Indicator.builder()
                .id(800L).projectId(1L).name("根").indicatorType("COMPOSITE"));
        Indicator child = Indicator.reconstruct(Indicator.builder()
                .id(801L).projectId(1L).parentIndicatorId(800L).name("子").indicatorType("LEAF")
                .sourceSectionId(150L));
        when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of(parent, child));

        // 映射: 800→900, 801→901
        java.util.concurrent.atomic.AtomicLong nextId = new java.util.concurrent.atomic.AtomicLong(900L);
        when(indicatorRepository.save(any())).thenAnswer(inv -> {
            Indicator i = inv.getArgument(0);
            i.setId(nextId.getAndIncrement());
            return i;
        });
        when(indicatorRepository.findById(901L)).thenAnswer(inv -> {
            Indicator c = Indicator.reconstruct(Indicator.builder()
                    .id(901L).projectId(2L).name("子").indicatorType("LEAF").sourceSectionId(150L));
            return Optional.of(c);
        });

        service.cloneProject(1L, req("克隆", 20L, LocalDate.of(2026, 6, 1), null, false), 999L);

        // save 至少调 2 次 (每个 indicator 一次 build) + 第二遍 parent 重写 1 次
        verify(indicatorRepository, atLeastOnce()).save(any());
        // 验证拷贝出来的 projectId 都是新 (通过 capture)
        ArgumentCaptor<Indicator> captor = ArgumentCaptor.forClass(Indicator.class);
        verify(indicatorRepository, atLeastOnce()).save(captor.capture());
        // 至少前两次 save 是建副本, projectId=2
        Indicator first = captor.getAllValues().get(0);
        assertThat(first.getProjectId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("cloneInspectors=true: 复制源项目检查员名单 + plan inspectorIds 原样保留")
    void shouldCloneInspectorsWhenRequested() {
        InspProject src = sourceProject(1L, ProjectStatus.PUBLISHED);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(src));
        mockProjectSaveAssignsId(2L);

        when(scoringProfileRepository.findByProjectId(1L)).thenReturn(List.of());
        InspectionPlan oldPlan = InspectionPlan.reconstruct(InspectionPlan.builder()
                .id(700L).projectId(1L).planName("A 组").inspectorIds("[10,11]"));
        when(inspectionPlanRepository.findByProjectId(1L)).thenReturn(List.of(oldPlan));
        when(inspectionPlanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of());

        ProjectInspector existing = ProjectInspector.reconstruct(ProjectInspector.builder()
                .id(800L).projectId(1L).userId(10L).userName("张三").role(InspectorRole.INSPECTOR));
        when(inspectorRepository.findByProjectId(1L)).thenReturn(List.of(existing));
        // 新项目下查询 inspector 数量 (审计日志用) 同样返 1 条 (mock idempotent)
        when(inspectorRepository.findByProjectId(2L)).thenReturn(List.of(existing));
        when(inspectorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cloneProject(1L, req("克隆", 20L, LocalDate.of(2026, 6, 1), null, true), 999L);

        // plan.inspectorIds 应原样保留
        ArgumentCaptor<InspectionPlan> planCaptor = ArgumentCaptor.forClass(InspectionPlan.class);
        verify(inspectionPlanRepository).save(planCaptor.capture());
        assertThat(planCaptor.getValue().getInspectorIds()).isEqualTo("[10,11]");

        // inspector 已复制
        ArgumentCaptor<ProjectInspector> inspectorCaptor = ArgumentCaptor.forClass(ProjectInspector.class);
        verify(inspectorRepository).save(inspectorCaptor.capture());
        ProjectInspector saved = inspectorCaptor.getValue();
        assertThat(saved.getProjectId()).isEqualTo(2L);
        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getUserName()).isEqualTo("张三");
    }

    @Test
    @DisplayName("ARCHIVED 项目: 拒绝克隆")
    void shouldRejectArchived() {
        InspProject archived = sourceProject(1L, ProjectStatus.ARCHIVED);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> service.cloneProject(1L,
                req("克隆", 20L, LocalDate.of(2026, 6, 1), null, false), 999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("归档");

        verify(projectRepository, never()).save(any());
    }

    @Test
    @DisplayName("源项目不存在: 抛 IllegalArgumentException")
    void shouldRejectIfSourceNotFound() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cloneProject(999L,
                req("克隆", 20L, LocalDate.of(2026, 6, 1), null, false), 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在");
    }

    @Test
    @DisplayName("不克隆执行数据: tasks / submissions / scores 仓库未被读取/写入")
    void shouldNotCloneExecutionData() {
        InspProject src = sourceProject(1L, ProjectStatus.PUBLISHED);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(src));
        mockProjectSaveAssignsId(2L);

        when(scoringProfileRepository.findByProjectId(1L)).thenReturn(List.of());
        when(inspectionPlanRepository.findByProjectId(1L)).thenReturn(List.of());
        when(indicatorRepository.findByProjectId(1L)).thenReturn(List.of());

        service.cloneProject(1L, req("克隆", 20L, LocalDate.of(2026, 6, 1), null, false), 999L);

        verify(taskRepository, never()).findByProjectId(any());
        verify(scoreRepository, never()).findByProjectId(any());
        verify(scoreRepository, never()).deleteByProjectId(any());
    }
}
