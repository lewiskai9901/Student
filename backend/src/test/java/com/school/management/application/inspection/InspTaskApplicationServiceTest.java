package com.school.management.application.inspection;

import com.school.management.application.event.TriggerService;
import com.school.management.domain.inspection.model.execution.*;
import com.school.management.domain.inspection.model.template.TemplateSection;
import com.school.management.domain.inspection.model.template.TemplateVersion;
import com.school.management.domain.inspection.repository.*;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import com.school.management.infrastructure.inspection.InspectionScopeHelper;
import com.school.management.infrastructure.metrics.InspectionMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * InspTaskApplicationService 应用服务单测.
 *
 * 用 Mockito 隔离 repository / 事件发布 / 度量 / 审计 / 触发服务,
 * 仅验证应用服务编排逻辑 (聚合根状态机已有专门测试).
 *
 * 覆盖: 多类型 task 创建 (createTask / createAdHoc / createTriggered / createSelfCheck /
 *       createCrossAudit) + 生命周期 (claim/start/submit/withdraw/reject/review/publish/
 *       cancel/assign/startReview/extendDeadline) + reassignDepartedInspector + 查询委托.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InspTaskApplicationService 应用服务")
class InspTaskApplicationServiceTest {

    @Mock InspTaskRepository taskRepository;
    @Mock InspProjectRepository projectRepository;
    @Mock InspSubmissionRepository submissionRepository;
    @Mock SubmissionDetailRepository detailRepository;
    @Mock TemplateSectionRepository sectionRepository;
    @Mock TemplateItemRepository itemRepository;
    @Mock ProjectScoreRepository scoreRepository;
    @Mock InspectionPlanRepository planRepository;
    @Mock TargetPopulationService targetPopulationService;
    @Mock ScoreAggregationService scoreAggregationService;
    @Mock SpringDomainEventPublisher eventPublisher;
    @Mock TemplateVersionRepository templateVersionRepository;
    @Mock InspectionAuditLogger auditLogger;
    @Mock TransactionTemplate transactionTemplate;
    @Mock JdbcTemplate jdbcTemplate;
    @Mock InspectionMetrics metrics;
    @Mock InspectionScopeHelper scopeHelper;
    @Mock InspTaskQueryService queryService;
    @Mock TriggerService triggerService;
    @Mock InspProjectAuthorizationGuard authGuard;

    ObjectMapper objectMapper = new ObjectMapper();

    InspTaskApplicationService service;

    @BeforeEach
    void setUp() {
        service = new InspTaskApplicationService(
                taskRepository, projectRepository, submissionRepository, detailRepository,
                sectionRepository, itemRepository, scoreRepository, planRepository,
                targetPopulationService, scoreAggregationService, eventPublisher, objectMapper,
                templateVersionRepository, auditLogger, transactionTemplate, jdbcTemplate,
                metrics, scopeHelper, queryService, authGuard);
    }

    // ---- helpers ----

    /** 一个无模板版本锁定的项目 — 让 preCheckTemplateDrift / populateSubmissions 早返回. */
    private InspProject bareProject(Long id) {
        return InspProject.builder().id(id).projectCode("PRJ-T").projectName("T")
                .status(ProjectStatus.PUBLISHED).build();
    }

    private InspTask taskInState(Long id, TaskStatus status) {
        return InspTask.reconstruct(InspTask.builder()
                .id(id).taskCode("TSK-1").projectId(7L)
                .taskDate(LocalDate.of(2026, 5, 1)).status(status));
    }

    private InspTask claimedTask(Long id) {
        InspTask t = taskInState(id, TaskStatus.CLAIMED);
        return t;
    }

    private void stubInjectedTrigger() {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "triggerService", triggerService);
    }

    // ============================================================
    @Nested
    @DisplayName("createTask — 计划任务创建")
    class CreateTaskTests {
        @Test
        @DisplayName("生成 taskCode 并保存为 PENDING + 发事件 + 度量")
        void shouldCreateScheduledTask() {
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            // populateSubmissions: 项目无 rootSectionId 且无计划 → 早返回
            when(planRepository.findByProjectId(7L)).thenReturn(List.of());

            InspTask saved = service.createTask(7L, LocalDate.of(2026, 5, 1), null, null, null);

            assertThat(saved.getStatus()).isEqualTo(TaskStatus.PENDING);
            assertThat(saved.getTaskCode()).startsWith("TSK-");
            assertThat(saved.getTaskType()).isEqualTo(TaskType.SCHEDULED);
            verify(eventPublisher).publishAll(any());
            verify(metrics).taskCreated();
        }

        @Test
        @DisplayName("项目不存在: 跳过 drift 检查仍可创建")
        void shouldCreateEvenWhenProjectMissing() {
            when(projectRepository.findById(7L)).thenReturn(Optional.empty());
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspTask saved = service.createTask(7L, LocalDate.of(2026, 5, 1), null, null, null);

            assertThat(saved).isNotNull();
            verify(metrics).taskCreated();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("createAdHocTask — 临时抽查")
    class CreateAdHocTests {
        @Test
        @DisplayName("项目允许抽查: 创建即 CLAIMED 类型 AD_HOC")
        void shouldCreateAdHoc() {
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            when(queryService.isProjectAllowAdHoc(7L)).thenReturn(true);
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(planRepository.findByProjectId(7L)).thenReturn(List.of());

            InspTask saved = service.createAdHocTask(7L, 99L, "张三", "突击检查");

            assertThat(saved.getStatus()).isEqualTo(TaskStatus.CLAIMED);
            assertThat(saved.getTaskType()).isEqualTo(TaskType.AD_HOC);
            assertThat(saved.getInspectorId()).isEqualTo(99L);
            verify(metrics).taskCreated();
            verify(metrics).taskClaimed();
        }

        @Test
        @DisplayName("项目不存在抛 IllegalArgumentException")
        void shouldRejectWhenProjectMissing() {
            when(projectRepository.findById(7L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.createAdHocTask(7L, 99L, "张三", "原因"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("项目不存在");
        }

        @Test
        @DisplayName("项目不允许抽查抛 IllegalStateException")
        void shouldRejectWhenNotAllowed() {
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            when(queryService.isProjectAllowAdHoc(7L)).thenReturn(false);
            assertThatThrownBy(() -> service.createAdHocTask(7L, 99L, "张三", "原因"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("不允许临时抽查");
        }

        @Test
        @DisplayName("空白 reason 抛 IllegalArgumentException")
        void shouldRejectBlankReason() {
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            when(queryService.isProjectAllowAdHoc(7L)).thenReturn(true);
            assertThatThrownBy(() -> service.createAdHocTask(7L, 99L, "张三", "  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("发起原因");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("createTriggeredTask — 事件触发任务")
    class CreateTriggeredTests {
        @Test
        @DisplayName("项目不存在: 返回 null 不抛")
        void shouldReturnNullWhenProjectMissing() {
            when(projectRepository.findById(7L)).thenReturn(Optional.empty());
            InspTask result = service.createTriggeredTask(7L, "Appeal", 5L, "申诉触发");
            assertThat(result).isNull();
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("已存在同源 task: 跳过返回 null")
        void shouldDedupExistingTriggeredTask() {
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("Appeal"), eq(5L)))
                    .thenReturn(1);
            InspTask result = service.createTriggeredTask(7L, "Appeal", 5L, "申诉触发");
            assertThat(result).isNull();
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("无重复: 创建 TRIGGERED 任务")
        void shouldCreateTriggered() {
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("Appeal"), eq(5L)))
                    .thenReturn(0);
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(planRepository.findByProjectId(7L)).thenReturn(List.of());

            InspTask saved = service.createTriggeredTask(7L, "Appeal", 5L, "申诉触发");

            assertThat(saved).isNotNull();
            assertThat(saved.getTaskType()).isEqualTo(TaskType.TRIGGERED);
            assertThat(saved.getStatus()).isEqualTo(TaskStatus.PENDING);
            verify(metrics).taskCreated();
        }

        @Test
        @DisplayName("dedup 查询异常: 不阻断, 继续创建")
        void shouldStillCreateWhenDedupQueryFails() {
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
                    .thenThrow(new RuntimeException("db down"));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(planRepository.findByProjectId(7L)).thenReturn(List.of());

            InspTask saved = service.createTriggeredTask(7L, "Appeal", 5L, "原因");
            assertThat(saved).isNotNull();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("createSelfCheckTask — 自查任务")
    class CreateSelfCheckTests {
        @Test
        @DisplayName("项目允许自查: 创建 SELF_CHECK 即 CLAIMED")
        void shouldCreateSelfCheck() {
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(7L)))
                    .thenReturn(1);
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(planRepository.findByProjectId(7L)).thenReturn(List.of());

            InspTask saved = service.createSelfCheckTask(7L, 88L, "李四", "自查");

            assertThat(saved.getTaskType()).isEqualTo(TaskType.SELF_CHECK);
            assertThat(saved.getStatus()).isEqualTo(TaskStatus.CLAIMED);
            assertThat(saved.getInspectorId()).isEqualTo(88L);
            verify(metrics).taskClaimed();
        }

        @Test
        @DisplayName("项目不允许自查抛 IllegalStateException")
        void shouldRejectWhenNotAllowed() {
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(7L)))
                    .thenReturn(0);
            assertThatThrownBy(() -> service.createSelfCheckTask(7L, 88L, "李四", "自查"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("不允许自查");
        }

        @Test
        @DisplayName("空白 reason 抛 IllegalArgumentException")
        void shouldRejectBlankReason() {
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(7L)))
                    .thenReturn(1);
            assertThatThrownBy(() -> service.createSelfCheckTask(7L, 88L, "李四", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("自查必须填写原因");
        }

        @Test
        @DisplayName("项目不存在抛 IllegalArgumentException")
        void shouldRejectWhenProjectMissing() {
            when(projectRepository.findById(7L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.createSelfCheckTask(7L, 88L, "李四", "自查"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("项目不存在");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("createCrossAuditTask — 互查任务")
    class CreateCrossAuditTests {
        @Test
        @DisplayName("有 reason + dueDate: 创建 CROSS_AUDIT")
        void shouldCreateCrossAudit() {
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(planRepository.findByProjectId(7L)).thenReturn(List.of());

            InspTask saved = service.createCrossAuditTask(7L, 99L, "王五",
                    LocalDate.of(2026, 6, 1), "互查");

            assertThat(saved.getTaskType()).isEqualTo(TaskType.CROSS_AUDIT);
            assertThat(saved.getStatus()).isEqualTo(TaskStatus.CLAIMED);
            verify(metrics).taskClaimed();
        }

        @Test
        @DisplayName("空白 reason 抛 IllegalArgumentException")
        void shouldRejectBlankReason() {
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            assertThatThrownBy(() -> service.createCrossAuditTask(7L, 99L, "王五",
                    LocalDate.of(2026, 6, 1), " "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("互查必须填写原因");
        }

        @Test
        @DisplayName("缺 dueDate 抛 IllegalArgumentException")
        void shouldRejectMissingDueDate() {
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            assertThatThrownBy(() -> service.createCrossAuditTask(7L, 99L, "王五", null, "互查"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("截止日期");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("查询方法")
    class QueryTests {
        @Test
        @DisplayName("getTask 委托 repository")
        void shouldGetTask() {
            InspTask t = taskInState(1L, TaskStatus.PENDING);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            assertThat(service.getTask(1L)).containsSame(t);
        }

        @Test
        @DisplayName("listTasksByProject 委托 repository")
        void shouldListByProject() {
            when(taskRepository.findByProjectId(7L)).thenReturn(List.of(taskInState(1L, TaskStatus.PENDING)));
            assertThat(service.listTasksByProject(7L)).hasSize(1);
        }

        @Test
        @DisplayName("listMyTasks 委托 findByInspectorOrReviewerId")
        void shouldListMyTasks() {
            when(taskRepository.findByInspectorOrReviewerId(5L)).thenReturn(List.of());
            assertThat(service.listMyTasks(5L)).isEmpty();
            verify(taskRepository).findByInspectorOrReviewerId(5L);
        }

        @Test
        @DisplayName("listAllTasks / listAvailableTasks 委托 repository")
        void shouldListAllAndAvailable() {
            when(taskRepository.findAll()).thenReturn(List.of());
            when(taskRepository.findAvailableTasks()).thenReturn(List.of());
            assertThat(service.listAllTasks()).isEmpty();
            assertThat(service.listAvailableTasks()).isEmpty();
        }

        @Test
        @DisplayName("listAvailableTasksForUser: 无计划任务直接保留")
        void shouldKeepTaskWithoutPlan() {
            InspTask t = taskInState(1L, TaskStatus.PENDING); // inspectionPlanId=null
            when(taskRepository.findAvailableTasks()).thenReturn(List.of(t));
            List<InspTask> result = service.listAvailableTasksForUser(5L);
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("listAvailableTasksForUser: 计划指定他人时过滤掉")
        void shouldFilterTaskAssignedToOthers() {
            InspTask t = InspTask.reconstruct(InspTask.builder()
                    .id(1L).taskCode("TSK-1").projectId(7L)
                    .taskDate(LocalDate.of(2026, 5, 1)).status(TaskStatus.PENDING)
                    .inspectionPlanId(33L));
            when(taskRepository.findAvailableTasks()).thenReturn(List.of(t));
            InspectionPlan plan = mock(InspectionPlan.class);
            when(plan.getInspectorIds()).thenReturn("100,200");
            when(planRepository.findById(33L)).thenReturn(Optional.of(plan));

            List<InspTask> result = service.listAvailableTasksForUser(5L);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("listAvailableTasksForUser: 计划包含该用户时保留")
        void shouldKeepTaskWhenUserInPlan() {
            InspTask t = InspTask.reconstruct(InspTask.builder()
                    .id(1L).taskCode("TSK-1").projectId(7L)
                    .taskDate(LocalDate.of(2026, 5, 1)).status(TaskStatus.PENDING)
                    .inspectionPlanId(33L));
            when(taskRepository.findAvailableTasks()).thenReturn(List.of(t));
            InspectionPlan plan = mock(InspectionPlan.class);
            when(plan.getInspectorIds()).thenReturn("5,200");
            when(planRepository.findById(33L)).thenReturn(Optional.of(plan));

            assertThat(service.listAvailableTasksForUser(5L)).hasSize(1);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("repopulateSubmissions")
    class RepopulateTests {
        @Test
        @DisplayName("已有目标计数 > 0: 跳过填充")
        void shouldSkipWhenAlreadyPopulated() {
            InspTask t = InspTask.reconstruct(InspTask.builder()
                    .id(1L).taskCode("TSK-1").projectId(7L)
                    .taskDate(LocalDate.of(2026, 5, 1)).status(TaskStatus.PENDING)
                    .totalTargets(5));
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            InspTask result = service.repopulateSubmissions(1L);
            assertThat(result).isSameAs(t);
            verify(projectRepository, never()).findById(any());
        }

        @Test
        @DisplayName("任务不存在抛 IllegalArgumentException")
        void shouldRejectWhenTaskMissing() {
            when(taskRepository.findById(1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.repopulateSubmissions(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("任务不存在");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("生命周期 — claim / start / submit / withdraw")
    class LifecycleTests {
        @Test
        @DisplayName("claimTask: PENDING → CLAIMED + 事件 + 度量")
        void shouldClaim() {
            InspTask t = taskInState(1L, TaskStatus.PENDING);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspTask saved = service.claimTask(1L, 99L, "张三");

            assertThat(saved.getStatus()).isEqualTo(TaskStatus.CLAIMED);
            assertThat(saved.getInspectorId()).isEqualTo(99L);
            verify(eventPublisher).publishAll(any());
            verify(metrics).taskClaimed();
        }

        @Test
        @DisplayName("claimTask: 任务不存在抛")
        void shouldRejectClaimNotFound() {
            when(taskRepository.findById(1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.claimTask(1L, 99L, "张三"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("startTask: CLAIMED → IN_PROGRESS")
        void shouldStart() {
            InspTask t = taskInState(1L, TaskStatus.CLAIMED);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspTask saved = service.startTask(1L);
            assertThat(saved.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("submitTask: IN_PROGRESS → SUBMITTED + 度量")
        void shouldSubmit() {
            InspTask t = taskInState(1L, TaskStatus.IN_PROGRESS);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(metrics.startSubmitTimer()).thenReturn(null);
            // tryComputeProjectScore: 项目不存在 → 早返回
            when(projectRepository.findById(7L)).thenReturn(Optional.empty());

            InspTask saved = service.submitTask(1L);

            assertThat(saved.getStatus()).isEqualTo(TaskStatus.SUBMITTED);
            verify(metrics).taskSubmitted();
            verify(metrics).recordSubmitDuration(any());
        }

        @Test
        @DisplayName("submitTask: 全部任务已提交时触发项目分数计算")
        void shouldComputeProjectScoreWhenAllSubmitted() {
            InspTask t = taskInState(1L, TaskStatus.IN_PROGRESS);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(metrics.startSubmitTimer()).thenReturn((Timer.Sample) null);
            InspProject p = bareProject(7L);
            when(projectRepository.findById(7L)).thenReturn(Optional.of(p));
            // 项目下所有任务 — 提交后该任务 SUBMITTED
            when(taskRepository.findByProjectId(7L)).thenReturn(List.of(t));
            // computeProjectScore: dateTasks 有 1 个, 但 submissions 空 → count==0 早返回
            when(submissionRepository.findByTaskId(1L)).thenReturn(List.of());

            InspTask saved = service.submitTask(1L);
            assertThat(saved.getStatus()).isEqualTo(TaskStatus.SUBMITTED);
        }

        @Test
        @DisplayName("withdrawTask: SUBMITTED → IN_PROGRESS, 重开已完成 submissions")
        void shouldWithdrawAndReopenSubmissions() {
            InspTask t = taskInState(1L, TaskStatus.SUBMITTED);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            InspSubmission completed = InspSubmission.reconstruct(InspSubmission.builder()
                    .id(10L).taskId(1L).status(SubmissionStatus.COMPLETED));
            InspSubmission pending = InspSubmission.reconstruct(InspSubmission.builder()
                    .id(11L).taskId(1L).status(SubmissionStatus.PENDING));
            when(submissionRepository.findByTaskId(1L)).thenReturn(List.of(completed, pending));

            InspTask saved = service.withdrawTask(1L);

            assertThat(saved.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(completed.getStatus()).isEqualTo(SubmissionStatus.IN_PROGRESS);
            // pending 未变
            assertThat(pending.getStatus()).isEqualTo(SubmissionStatus.PENDING);
            verify(submissionRepository).save(completed);
            verify(submissionRepository, never()).save(pending);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("rejectTask — 驳回")
    class RejectTests {
        @Test
        @DisplayName("SUBMITTED → IN_PROGRESS, 重开 submissions, 审计, 触发通知")
        void shouldRejectAndAuditAndTrigger() {
            stubInjectedTrigger();
            InspTask t = taskInState(1L, TaskStatus.SUBMITTED);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            InspSubmission completed = InspSubmission.reconstruct(InspSubmission.builder()
                    .id(10L).taskId(1L).status(SubmissionStatus.COMPLETED));
            when(submissionRepository.findByTaskId(1L)).thenReturn(List.of(completed));

            InspTask saved = service.rejectTask(1L, "证据不足");

            assertThat(saved.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(saved.getRejectionCount()).isEqualTo(1);
            assertThat(completed.getStatus()).isEqualTo(SubmissionStatus.IN_PROGRESS);
            verify(auditLogger).log(eq("InspTask"), eq(1L), anyString(),
                    eq("TASK_REJECTED"), eq("证据不足"), anyMap());
            verify(triggerService).fire(any(), anyMap());
        }

        @Test
        @DisplayName("项目级 maxRejectCount 传入 reject")
        void shouldUseProjectMaxRejectCount() {
            InspTask t = taskInState(1L, TaskStatus.SUBMITTED);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            InspProject p = InspProject.builder().id(7L).projectCode("P").projectName("P")
                    .status(ProjectStatus.PUBLISHED).maxRejectCount(5).build();
            when(projectRepository.findById(7L)).thenReturn(Optional.of(p));
            when(submissionRepository.findByTaskId(1L)).thenReturn(List.of());

            InspTask saved = service.rejectTask(1L, "comment");
            assertThat(saved.getRejectionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("triggerService 未注入: 不抛, 仍正常驳回")
        void shouldRejectWhenTriggerServiceAbsent() {
            InspTask t = taskInState(1L, TaskStatus.SUBMITTED);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            when(submissionRepository.findByTaskId(1L)).thenReturn(List.of());

            InspTask saved = service.rejectTask(1L, "c");
            assertThat(saved.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("任务不存在抛")
        void shouldRejectWhenTaskMissing() {
            when(taskRepository.findById(1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.rejectTask(1L, "c"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("reassignDepartedInspector — 离职重派")
    class ReassignTests {
        @Test
        @DisplayName("userId 为空抛 IllegalArgumentException")
        void shouldRejectNullUserId() {
            assertThatThrownBy(() -> service.reassignDepartedInspector(null, "离职", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("fallback 用户不存在抛 IllegalArgumentException")
        void shouldRejectInvalidFallback() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(500L)))
                    .thenReturn(0);
            assertThatThrownBy(() -> service.reassignDepartedInspector(5L, "离职", 500L, "顶替"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fallbackInspectorId");
        }

        @Test
        @DisplayName("无任务: affected 返回 0")
        void shouldReturnZeroWhenNoTasks() {
            when(taskRepository.findByInspectorId(5L)).thenReturn(List.of());
            int affected = service.reassignDepartedInspector(5L, "离职", null, null);
            assertThat(affected).isZero();
        }

        @Test
        @DisplayName("CLAIMED 任务被重派: affected=1, unclaim + 重派 + 审计")
        void shouldReassignClaimedTask() {
            InspTask claimed = InspTask.reconstruct(InspTask.builder()
                    .id(1L).taskCode("TSK-1").projectId(7L)
                    .taskDate(LocalDate.of(2026, 5, 1)).status(TaskStatus.CLAIMED)
                    .inspectorId(5L).inspectorName("张三"));
            when(taskRepository.findByInspectorId(5L)).thenReturn(List.of(claimed));
            when(taskRepository.findById(1L)).thenReturn(Optional.of(claimed));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(500L)))
                    .thenReturn(1);
            // transactionTemplate.execute 直接跑 callback
            when(transactionTemplate.execute(any())).thenAnswer(inv ->
                    ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(
                            mock(org.springframework.transaction.TransactionStatus.class)));

            int affected = service.reassignDepartedInspector(5L, "离职", 500L, "顶替");

            assertThat(affected).isEqualTo(1);
            assertThat(claimed.getInspectorId()).isEqualTo(500L);
            assertThat(claimed.getStatus()).isEqualTo(TaskStatus.PENDING);
            verify(auditLogger).log(eq("InspTask"), eq(1L), anyString(),
                    eq("TASK_INSPECTOR_REASSIGNED"), eq("离职"), anyMap());
        }

        @Test
        @DisplayName("SUBMITTED 任务不在重派范围: affected=0")
        void shouldSkipNonReassignableTask() {
            InspTask submitted = InspTask.reconstruct(InspTask.builder()
                    .id(1L).taskCode("TSK-1").projectId(7L)
                    .taskDate(LocalDate.of(2026, 5, 1)).status(TaskStatus.SUBMITTED)
                    .inspectorId(5L));
            when(taskRepository.findByInspectorId(5L)).thenReturn(List.of(submitted));
            int affected = service.reassignDepartedInspector(5L, "离职", null, null);
            assertThat(affected).isZero();
            verify(transactionTemplate, never()).execute(any());
        }
    }

    // ============================================================
    @Nested
    @DisplayName("extendTaskDeadline / startReview / reviewTask / publishTask / cancelTask / assignTask")
    class OtherLifecycleTests {
        @Test
        @DisplayName("extendTaskDeadline: 推后期限 + 审计")
        void shouldExtendDeadline() {
            InspTask t = taskInState(1L, TaskStatus.IN_PROGRESS);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspTask saved = service.extendTaskDeadline(1L, LocalDate.of(2026, 6, 1));

            assertThat(saved.getExtendedTo()).isEqualTo(LocalDate.of(2026, 6, 1));
            verify(auditLogger).log(eq("InspTask"), eq(1L), anyString(),
                    eq("TASK_DEADLINE_EXTENDED"), isNull(), anyMap());
        }

        @Test
        @DisplayName("startReview: SUBMITTED → UNDER_REVIEW")
        void shouldStartReview() {
            InspTask t = taskInState(1L, TaskStatus.SUBMITTED);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspTask saved = service.startReview(1L, 50L, "审核员");
            assertThat(saved.getStatus()).isEqualTo(TaskStatus.UNDER_REVIEW);
            assertThat(saved.getReviewerId()).isEqualTo(50L);
        }

        @Test
        @DisplayName("reviewTask: UNDER_REVIEW → REVIEWED + 度量")
        void shouldReviewTask() {
            InspTask t = taskInState(1L, TaskStatus.UNDER_REVIEW);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspTask saved = service.reviewTask(1L, "通过");
            assertThat(saved.getStatus()).isEqualTo(TaskStatus.REVIEWED);
            assertThat(saved.getReviewComment()).isEqualTo("通过");
            verify(metrics).taskReviewed("approved");
        }

        @Test
        @DisplayName("publishTask: REVIEWED → PUBLISHED + 重算项目分数")
        void shouldPublishTask() {
            InspTask t = taskInState(1L, TaskStatus.REVIEWED);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));

            InspTask saved = service.publishTask(1L);

            assertThat(saved.getStatus()).isEqualTo(TaskStatus.PUBLISHED);
            verify(metrics).taskPublished();
            verify(scoreAggregationService).recomputeProjectScore(eq(7L), any());
        }

        @Test
        @DisplayName("publishTask: autoPublish=true 项目, SUBMITTED 可直接发布")
        void shouldPublishWithAutoPublish() {
            InspTask t = taskInState(1L, TaskStatus.SUBMITTED);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            InspProject p = InspProject.builder().id(7L).projectCode("P").projectName("P")
                    .status(ProjectStatus.PUBLISHED).autoPublish(true).build();
            when(projectRepository.findById(7L)).thenReturn(Optional.of(p));

            InspTask saved = service.publishTask(1L);
            assertThat(saved.getStatus()).isEqualTo(TaskStatus.PUBLISHED);
        }

        @Test
        @DisplayName("publishTask: 重算分数抛异常不阻断发布")
        void shouldPublishEvenWhenRecomputeFails() {
            InspTask t = taskInState(1L, TaskStatus.REVIEWED);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(projectRepository.findById(7L)).thenReturn(Optional.of(bareProject(7L)));
            doThrow(new RuntimeException("boom")).when(scoreAggregationService)
                    .recomputeProjectScore(anyLong(), any());

            InspTask saved = service.publishTask(1L);
            assertThat(saved.getStatus()).isEqualTo(TaskStatus.PUBLISHED);
        }

        @Test
        @DisplayName("cancelTask: PENDING → CANCELLED")
        void shouldCancelTask() {
            InspTask t = taskInState(1L, TaskStatus.PENDING);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspTask saved = service.cancelTask(1L);
            assertThat(saved.getStatus()).isEqualTo(TaskStatus.CANCELLED);
            verify(eventPublisher).publishAll(any());
        }

        @Test
        @DisplayName("assignTask: PENDING 任务设置 inspector")
        void shouldAssignTask() {
            InspTask t = taskInState(1L, TaskStatus.PENDING);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(t));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            InspTask saved = service.assignTask(1L, 77L, "指派检查员");
            assertThat(saved.getInspectorId()).isEqualTo(77L);
            assertThat(saved.getInspectorName()).isEqualTo("指派检查员");
        }

        @Test
        @DisplayName("startReview 任务不存在抛")
        void shouldRejectStartReviewNotFound() {
            when(taskRepository.findById(1L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.startReview(1L, 50L, "x"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getTaskTypeKpi / listAdHocAllowedProjects / getInspectionMode / updateInspectionMode — 委托")
    class DelegationTests {
        @Test
        @DisplayName("getTaskTypeKpi 委托 queryService")
        void shouldDelegateKpi() {
            when(queryService.getTaskTypeKpi(7L)).thenReturn(java.util.Map.of("k", "v"));
            assertThat(service.getTaskTypeKpi(7L)).containsEntry("k", "v");
        }

        @Test
        @DisplayName("listAdHocAllowedProjects 委托 queryService")
        void shouldDelegateAdHocList() {
            when(queryService.listAdHocAllowedProjects()).thenReturn(List.of());
            assertThat(service.listAdHocAllowedProjects()).isEmpty();
        }

        @Test
        @DisplayName("getInspectionMode 委托 queryService")
        void shouldDelegateGetMode() {
            when(queryService.getInspectionMode(7L)).thenReturn(java.util.Map.of("mode", "HYBRID"));
            assertThat(service.getInspectionMode(7L)).containsEntry("mode", "HYBRID");
        }

        @Test
        @DisplayName("updateInspectionMode 委托 queryService")
        void shouldDelegateUpdateMode() {
            when(queryService.updateInspectionMode(7L, "HYBRID", true, false, 3))
                    .thenReturn(java.util.Map.of("ok", true));
            assertThat(service.updateInspectionMode(7L, "HYBRID", true, false, 3))
                    .containsEntry("ok", true);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("populateSubmissions — 自动填充 (通过 createTask 间接覆盖)")
    class PopulateTests {
        @Test
        @DisplayName("项目有 rootSectionId 但无子分区: 不创建 submission")
        void shouldNotPopulateWhenNoChildSections() {
            InspProject p = InspProject.builder().id(7L).projectCode("P").projectName("P")
                    .status(ProjectStatus.PUBLISHED).rootSectionId(100L).build();
            when(projectRepository.findById(7L)).thenReturn(Optional.of(p));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(sectionRepository.findByParentSectionId(100L)).thenReturn(List.of());

            InspTask saved = service.createTask(7L, LocalDate.of(2026, 5, 1), null, null, null);

            assertThat(saved).isNotNull();
            verify(submissionRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("项目从检查计划取 rootSectionId")
        void shouldFallbackToPlanRootSection() {
            InspProject p = InspProject.builder().id(7L).projectCode("P").projectName("P")
                    .status(ProjectStatus.PUBLISHED).build();
            when(projectRepository.findById(7L)).thenReturn(Optional.of(p));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            InspectionPlan plan = mock(InspectionPlan.class);
            when(plan.getRootSectionId()).thenReturn(200L);
            when(planRepository.findByProjectId(7L)).thenReturn(List.of(plan));
            when(sectionRepository.findByParentSectionId(200L)).thenReturn(List.of());

            InspTask saved = service.createTask(7L, LocalDate.of(2026, 5, 1), null, null, null);
            assertThat(saved).isNotNull();
            verify(sectionRepository).findByParentSectionId(200L);
        }
    }
}
