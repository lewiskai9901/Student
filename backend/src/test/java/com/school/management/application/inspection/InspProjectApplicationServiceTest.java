package com.school.management.application.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.application.inspection.dto.ProjectStatsSummary;
import com.school.management.application.inspection.dto.ProjectTaskStats;
import com.school.management.domain.inspection.model.execution.*;
import com.school.management.domain.inspection.model.template.TemplateSection;
import com.school.management.domain.inspection.model.template.TemplateVersion;
import com.school.management.domain.inspection.repository.*;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * InspProjectApplicationService 应用服务单测.
 *
 * 用 Mockito 隔离 repository / 事件发布 / 目标人群解析 / 审计日志,
 * 仅验证应用服务的编排逻辑 (而非聚合根, 后者已有专门状态机测试).
 *
 * 重点: lifecycle 5 个动作 (publish/pause/resume/complete/archive)
 *       + listProjectsWithStats N+1 消除聚合
 *       + upgradeTemplateVersion 漂移升级
 *       + getTemplateVersionStatus 漂移检测
 *       + 删除级联
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InspProjectApplicationService 应用服务")
class InspProjectApplicationServiceTest {

    @Mock InspProjectRepository projectRepository;
    @Mock ProjectInspectorRepository inspectorRepository;
    @Mock ProjectScoreRepository scoreRepository;
    @Mock SpringDomainEventPublisher eventPublisher;
    @Mock ScoringProfileRepository scoringProfileRepository;
    @Mock TargetPopulationService targetPopulationService;
    @Mock TemplateSectionRepository templateSectionRepository;
    @Mock TemplateVersionRepository templateVersionRepository;
    @Mock InspectionAuditLogger auditLogger;
    @Mock InspTaskRepository taskRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    InspProjectApplicationService service;

    @BeforeEach
    void setUp() {
        service = new InspProjectApplicationService(
                projectRepository, inspectorRepository, scoreRepository,
                eventPublisher, scoringProfileRepository, targetPopulationService,
                objectMapper, templateSectionRepository, templateVersionRepository,
                auditLogger, taskRepository);
    }

    private InspProject draft(Long id) {
        return InspProject.builder().id(id).projectCode("PRJ-T").projectName("T")
                .rootSectionId(100L).status(ProjectStatus.DRAFT).build();
    }

    private InspProject inState(Long id, ProjectStatus status) {
        return InspProject.builder().id(id).projectCode("PRJ-T").projectName("T")
                .rootSectionId(100L).status(status).build();
    }

    private TemplateSection sectionWithTemplate(Long sectionId, Long templateId) {
        TemplateSection.Builder b = TemplateSection.builder()
                .sectionCode("SEC-T").sectionName("分区").parentSectionId(null)
                .createdBy(1L).templateId(templateId);
        TemplateSection s = TemplateSection.reconstruct(b);
        s.setId(sectionId);
        return s;
    }

    // ============================================================
    @Nested
    @DisplayName("createProject")
    class CreateTests {
        @Test
        @DisplayName("生成 projectCode 并保存为 DRAFT")
        void shouldCreateAndSave() {
            when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            InspProject p = service.createProject("项目A", 100L, LocalDate.of(2026, 5, 1), 999L);
            assertThat(p.getProjectName()).isEqualTo("项目A");
            assertThat(p.getStatus()).isEqualTo(ProjectStatus.DRAFT);
            assertThat(p.getProjectCode()).startsWith("PRJ-");
            verify(projectRepository).save(any(InspProject.class));
        }
    }

    // ============================================================
    @Nested
    @DisplayName("listProjectsWithStats — N+1 消除")
    class ListWithStatsTests {
        @Test
        @DisplayName("空项目列表返回空 list, 不触发任何 stats 查询")
        void shouldReturnEmptyForNoProjects() {
            when(projectRepository.findAll()).thenReturn(List.of());
            List<ProjectStatsSummary> result = service.listProjectsWithStats(null);
            assertThat(result).isEmpty();
            verify(taskRepository, never()).findStatsByProjectIds(any());
            verify(inspectorRepository, never()).countByProjectIds(any());
        }

        @Test
        @DisplayName("两个项目: 正确组装 stats + inspectorCount")
        void shouldAggregateStatsForMultipleProjects() {
            InspProject p1 = inState(1L, ProjectStatus.PUBLISHED);
            InspProject p2 = inState(2L, ProjectStatus.DRAFT);
            when(projectRepository.findAll()).thenReturn(List.of(p1, p2));
            when(taskRepository.findStatsByProjectIds(List.of(1L, 2L))).thenReturn(List.of(
                    new ProjectTaskStats(1L, 10, 5, 2, 1),
                    new ProjectTaskStats(2L, 0, 0, 0, 0)
            ));
            when(inspectorRepository.countByProjectIds(List.of(1L, 2L)))
                    .thenReturn(Map.of(1L, 3, 2L, 0));

            List<ProjectStatsSummary> result = service.listProjectsWithStats(null);

            assertThat(result).hasSize(2);
            ProjectStatsSummary s1 = result.get(0);
            assertThat(s1.getProject().getId()).isEqualTo(1L);
            assertThat(s1.getTaskTotal()).isEqualTo(10);
            assertThat(s1.getTaskDone()).isEqualTo(5);
            assertThat(s1.getTaskOverdue()).isEqualTo(2);
            assertThat(s1.getTaskPendingReview()).isEqualTo(1);
            assertThat(s1.getInspectorCount()).isEqualTo(3);

            ProjectStatsSummary s2 = result.get(1);
            assertThat(s2.getProject().getId()).isEqualTo(2L);
            assertThat(s2.getTaskTotal()).isZero();
            assertThat(s2.getInspectorCount()).isZero();
        }

        @Test
        @DisplayName("status 过滤: 传 DRAFT 仅查 DRAFT 项目")
        void shouldFilterByStatus() {
            InspProject p = inState(1L, ProjectStatus.DRAFT);
            when(projectRepository.findByStatus(ProjectStatus.DRAFT)).thenReturn(List.of(p));
            when(taskRepository.findStatsByProjectIds(List.of(1L))).thenReturn(List.of());
            when(inspectorRepository.countByProjectIds(List.of(1L))).thenReturn(Map.of());

            service.listProjectsWithStats(ProjectStatus.DRAFT);

            verify(projectRepository).findByStatus(ProjectStatus.DRAFT);
            verify(projectRepository, never()).findAll();
        }

        @Test
        @DisplayName("缺失 stats 行的项目, 各计数兜底为 0")
        void shouldDefaultMissingStatsToZero() {
            InspProject p = inState(1L, ProjectStatus.PUBLISHED);
            when(projectRepository.findAll()).thenReturn(List.of(p));
            when(taskRepository.findStatsByProjectIds(any())).thenReturn(List.of()); // 没返回 1
            when(inspectorRepository.countByProjectIds(any())).thenReturn(Map.of());

            List<ProjectStatsSummary> result = service.listProjectsWithStats(null);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTaskTotal()).isZero();
            assertThat(result.get(0).getInspectorCount()).isZero();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("publishProject — 发布前置校验")
    class PublishTests {
        @Test
        @DisplayName("scopeConfig 为空时拒绝发布")
        void shouldRejectWhenNoScope() {
            InspProject p = draft(1L);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            assertThatThrownBy(() -> service.publishProject(1L, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("检查范围");
        }

        @Test
        @DisplayName("startDate 为空时拒绝发布")
        void shouldRejectWhenNoStartDate() {
            InspProject p = InspProject.builder().id(1L).projectCode("X").projectName("X")
                    .rootSectionId(100L).status(ProjectStatus.DRAFT)
                    .scopeType(ScopeType.ORG).scopeConfig("[1]").build();
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            assertThatThrownBy(() -> service.publishProject(1L, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("开始日期");
        }

        @Test
        @DisplayName("范围内无目标时拒绝发布")
        void shouldRejectWhenNoTargets() {
            InspProject p = InspProject.builder().id(1L).projectCode("X").projectName("X")
                    .rootSectionId(100L).status(ProjectStatus.DRAFT)
                    .scopeType(ScopeType.ORG).scopeConfig("[1]")
                    .startDate(LocalDate.of(2026, 5, 1)).build();
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            when(targetPopulationService.resolveTargets(any(), any(), any())).thenReturn(List.of());
            assertThatThrownBy(() -> service.publishProject(1L, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("有效的检查对象");
        }

        @Test
        @DisplayName("项目不存在抛 IllegalArgumentException")
        void shouldRejectIfNotFound() {
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.publishProject(999L, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("项目不存在");
        }

        @Test
        @DisplayName("成功发布: 取最新模板版本 + 调聚合 + 发事件")
        void shouldPublishSuccessfully() {
            InspProject p = InspProject.builder().id(1L).projectCode("X").projectName("X")
                    .rootSectionId(100L).status(ProjectStatus.DRAFT)
                    .scopeType(ScopeType.ORG).scopeConfig("[1]")
                    .startDate(LocalDate.of(2026, 5, 1)).build();
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            when(targetPopulationService.resolveTargets(any(), any(), any()))
                    .thenReturn(List.of(new TargetPopulationService.TargetInfo(1L, "T", null)));
            TemplateSection section = sectionWithTemplate(100L, 50L);
            when(templateSectionRepository.findById(100L)).thenReturn(Optional.of(section));
            TemplateVersion latest = TemplateVersion.reconstruct(500L, 50L, 2, "{}", null, 1L, java.time.LocalDateTime.now());
            when(templateVersionRepository.findLatestByTemplateId(50L)).thenReturn(Optional.of(latest));
            when(scoringProfileRepository.findBySectionId(100L)).thenReturn(Optional.empty());
            when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspProject saved = service.publishProject(1L, null);

            assertThat(saved.getStatus()).isEqualTo(ProjectStatus.PUBLISHED);
            assertThat(saved.getTemplateVersionId()).isEqualTo(500L);
            verify(eventPublisher).publishAll(any());
        }
    }

    // ============================================================
    @Nested
    @DisplayName("lifecycle: pause / resume / complete / archive")
    class LifecycleTests {
        @Test
        @DisplayName("pauseProject: PUBLISHED → PAUSED + 事件发布")
        void shouldPause() {
            InspProject p = inState(1L, ProjectStatus.PUBLISHED);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            InspProject saved = service.pauseProject(1L);
            assertThat(saved.getStatus()).isEqualTo(ProjectStatus.PAUSED);
            verify(eventPublisher).publishAll(any());
        }

        @Test
        @DisplayName("resumeProject: PAUSED → PUBLISHED + 事件")
        void shouldResume() {
            InspProject p = inState(1L, ProjectStatus.PAUSED);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            InspProject saved = service.resumeProject(1L);
            assertThat(saved.getStatus()).isEqualTo(ProjectStatus.PUBLISHED);
        }

        @Test
        @DisplayName("completeProject: PUBLISHED → COMPLETED + 事件")
        void shouldComplete() {
            InspProject p = inState(1L, ProjectStatus.PUBLISHED);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            InspProject saved = service.completeProject(1L);
            assertThat(saved.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        }

        @Test
        @DisplayName("archiveProject: COMPLETED → ARCHIVED")
        void shouldArchive() {
            InspProject p = inState(1L, ProjectStatus.COMPLETED);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            InspProject saved = service.archiveProject(1L);
            assertThat(saved.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        }

        @Test
        @DisplayName("pause 不存在的项目抛 IllegalArgumentException")
        void shouldRejectPauseNotFound() {
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.pauseProject(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("deleteProject — 级联删除")
    class DeleteTests {
        @Test
        @DisplayName("删除时同时清 score / inspector / project")
        void shouldCascadeDelete() {
            service.deleteProject(1L);
            verify(scoreRepository).deleteByProjectId(1L);
            verify(inspectorRepository).deleteByProjectId(1L);
            verify(projectRepository).deleteById(1L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("upgradeTemplateVersion — 模板漂移升级")
    class UpgradeTemplateVersionTests {
        @Test
        @DisplayName("PUBLISHED 项目: 锁到模板最新版本 + 审计")
        void shouldUpgradeToLatest() {
            InspProject p = InspProject.builder().id(1L).projectCode("X").projectName("X")
                    .rootSectionId(100L).templateVersionId(10L).status(ProjectStatus.PUBLISHED).build();
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            TemplateSection section = sectionWithTemplate(100L, 50L);
            when(templateSectionRepository.findById(100L)).thenReturn(Optional.of(section));
            TemplateVersion latest = TemplateVersion.reconstruct(20L, 50L, 2, "{}", null, 1L, java.time.LocalDateTime.now());
            when(templateVersionRepository.findLatestByTemplateId(50L)).thenReturn(Optional.of(latest));
            when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspProject saved = service.upgradeTemplateVersion(1L);

            assertThat(saved.getTemplateVersionId()).isEqualTo(20L);
            verify(auditLogger).log(eq("InspProject"), eq(1L), eq("X"),
                    eq("PROJECT_TEMPLATE_UPGRADED"), any(), any(Map.class));
        }

        @Test
        @DisplayName("已是最新: 不重新锁定也不审计")
        void shouldNoOpWhenAlreadyLatest() {
            InspProject p = InspProject.builder().id(1L).projectCode("X").projectName("X")
                    .rootSectionId(100L).templateVersionId(20L).status(ProjectStatus.PUBLISHED).build();
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            TemplateSection section = sectionWithTemplate(100L, 50L);
            when(templateSectionRepository.findById(100L)).thenReturn(Optional.of(section));
            TemplateVersion latest = TemplateVersion.reconstruct(20L, 50L, 2, "{}", null, 1L, java.time.LocalDateTime.now());
            when(templateVersionRepository.findLatestByTemplateId(50L)).thenReturn(Optional.of(latest));

            InspProject saved = service.upgradeTemplateVersion(1L);

            assertThat(saved.getTemplateVersionId()).isEqualTo(20L);
            verify(projectRepository, never()).save(any());
            verify(auditLogger, never()).log(anyString(), anyLong(), anyString(), anyString(), any(), any());
        }

        @Test
        @DisplayName("DRAFT 项目: 拒绝升级")
        void shouldRejectUpgradeDraft() {
            InspProject p = draft(1L);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            assertThatThrownBy(() -> service.upgradeTemplateVersion(1L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("多模板项目 (rootSectionId=null): 拒绝升级")
        void shouldRejectUpgradeMultiTemplate() {
            InspProject p = InspProject.builder().id(1L).projectCode("X").projectName("X")
                    .rootSectionId(null).status(ProjectStatus.PUBLISHED).build();
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            assertThatThrownBy(() -> service.upgradeTemplateVersion(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("多模板");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getTemplateVersionStatus — 漂移检测")
    class TemplateVersionStatusTests {
        @Test
        @DisplayName("当前版本 != 最新: drifted=true")
        void shouldDetectDrift() {
            InspProject p = InspProject.builder().id(1L).projectCode("X").projectName("X")
                    .rootSectionId(100L).templateVersionId(10L).status(ProjectStatus.PUBLISHED).build();
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            TemplateSection section = sectionWithTemplate(100L, 50L);
            when(templateSectionRepository.findById(100L)).thenReturn(Optional.of(section));
            TemplateVersion current = TemplateVersion.reconstruct(10L, 50L, 1, "{}", null, 1L, java.time.LocalDateTime.now());
            TemplateVersion latest = TemplateVersion.reconstruct(20L, 50L, 2, "{}", null, 1L, java.time.LocalDateTime.now());
            when(templateVersionRepository.findById(10L)).thenReturn(Optional.of(current));
            when(templateVersionRepository.findLatestByTemplateId(50L)).thenReturn(Optional.of(latest));

            Map<String, Object> result = service.getTemplateVersionStatus(1L);

            assertThat(result.get("drifted")).isEqualTo(true);
            assertThat(result.get("currentVersionId")).isEqualTo(10L);
            assertThat(result.get("latestVersionId")).isEqualTo(20L);
            assertThat(result.get("currentVersionNumber")).isEqualTo(1);
            assertThat(result.get("latestVersionNumber")).isEqualTo(2);
        }

        @Test
        @DisplayName("当前版本 == 最新: drifted=false")
        void shouldNotDriftWhenSame() {
            InspProject p = InspProject.builder().id(1L).projectCode("X").projectName("X")
                    .rootSectionId(100L).templateVersionId(20L).status(ProjectStatus.PUBLISHED).build();
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            TemplateSection section = sectionWithTemplate(100L, 50L);
            when(templateSectionRepository.findById(100L)).thenReturn(Optional.of(section));
            TemplateVersion latest = TemplateVersion.reconstruct(20L, 50L, 2, "{}", null, 1L, java.time.LocalDateTime.now());
            when(templateVersionRepository.findById(20L)).thenReturn(Optional.of(latest));
            when(templateVersionRepository.findLatestByTemplateId(50L)).thenReturn(Optional.of(latest));

            Map<String, Object> result = service.getTemplateVersionStatus(1L);
            assertThat(result.get("drifted")).isEqualTo(false);
        }

        @Test
        @DisplayName("多模板项目: multiTemplate=true 且不查模板版本")
        void shouldHandleMultiTemplate() {
            InspProject p = InspProject.builder().id(1L).projectCode("X").projectName("X")
                    .rootSectionId(null).status(ProjectStatus.PUBLISHED).build();
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));

            Map<String, Object> result = service.getTemplateVersionStatus(1L);
            assertThat(result.get("multiTemplate")).isEqualTo(true);
            assertThat(result.get("drifted")).isEqualTo(false);
            verify(templateSectionRepository, never()).findById(any());
        }
    }

    // ============================================================
    @Nested
    @DisplayName("updatePolicyConfig")
    class UpdatePolicyConfigTests {
        @Test
        @DisplayName("更新策略 + 写审计日志")
        void shouldUpdateAndAudit() {
            InspProject p = inState(1L, ProjectStatus.PUBLISHED);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
            when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            service.updatePolicyConfig(1L, 5, 4, 14, 999L);
            verify(projectRepository).save(any());
            verify(auditLogger).log(eq("InspProject"), eq(1L), anyString(),
                    eq("PROJECT_POLICY_UPDATED"), any(), any(Map.class));
        }

        @Test
        @DisplayName("项目不存在抛")
        void shouldRejectIfNotFound() {
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updatePolicyConfig(999L, 5, 4, 14, 999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
