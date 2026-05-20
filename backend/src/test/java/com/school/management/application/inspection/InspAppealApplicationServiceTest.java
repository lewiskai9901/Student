package com.school.management.application.inspection;

import com.school.management.application.event.TriggerService;
import com.school.management.domain.inspection.event.InspectionTriggerPoints;
import com.school.management.domain.inspection.model.appeal.AppealStatus;
import com.school.management.domain.inspection.model.appeal.InspAppeal;
import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.ProjectStatus;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import com.school.management.domain.inspection.model.execution.TargetType;
import com.school.management.domain.inspection.repository.InspAppealRepository;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import com.school.management.domain.inspection.repository.SubmissionDetailRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import com.school.management.infrastructure.metrics.InspectionMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * InspAppealApplicationService 应用服务单测.
 *
 * 用 Mockito 隔离 repository / 事件发布 / 触发器 / 审计 / 指标,
 * 验证申诉工作流编排: submit / approve / reject / withdraw + 查询.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InspAppealApplicationService 申诉应用服务")
class InspAppealApplicationServiceTest {

    @Mock InspAppealRepository appealRepository;
    @Mock SubmissionDetailRepository detailRepository;
    @Mock InspSubmissionRepository submissionRepository;
    @Mock InspTaskRepository taskRepository;
    @Mock InspProjectRepository projectRepository;
    @Mock SpringDomainEventPublisher eventPublisher;
    @Mock InspectionAuditLogger auditLogger;
    @Mock InspectionMetrics metrics;
    @Mock TriggerService triggerService;

    @InjectMocks InspAppealApplicationService service;

    /**
     * {@code triggerService} 是 {@code @Autowired(required=false)} 的非 final 字段, 不在
     * {@code @RequiredArgsConstructor} 生成的构造器参数里. Mockito 的 {@code @InjectMocks}
     * 用构造器注入满足 8 个 final 依赖后, 不会再对剩余字段做字段注入, 因此该字段会保持 null,
     * 导致 {@code fireTrigger()} 直接 return. 这里手动把 mock 注入该字段.
     */
    @BeforeEach
    void wireTriggerService() {
        ReflectionTestUtils.setField(service, "triggerService", triggerService);
    }

    // -------- fixtures --------

    private SubmissionDetail detail(Long id, Long submissionId) {
        return SubmissionDetail.reconstruct(
                SubmissionDetail.builder().id(id).submissionId(submissionId));
    }

    private InspSubmission submission(Long id, Long taskId, TargetType tt, Long targetId) {
        return InspSubmission.reconstruct(
                InspSubmission.builder().id(id).taskId(taskId).targetType(tt).targetId(targetId));
    }

    private InspTask task(Long id, Long projectId, LocalDateTime publishedAt) {
        return InspTask.builder().id(id).taskCode("TSK-1").projectId(projectId)
                .publishedAt(publishedAt).build();
    }

    private InspAppeal pendingAppeal(Long id, Long submitterUserId) {
        InspAppeal a = InspAppeal.builder().id(id).appealCode("APL-1")
                .submissionDetailId(7L).submitterUserId(submitterUserId)
                .status(AppealStatus.PENDING).build();
        return a;
    }

    // ============================================================
    @Nested
    @DisplayName("submitAppeal")
    class SubmitTests {

        @Test
        @DisplayName("扣分明细不存在 → IllegalArgumentException")
        void shouldRejectWhenDetailMissing() {
            when(detailRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.submitAppeal(99L, 1L, "张三", "理由", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("扣分明细不存在");
        }

        @Test
        @DisplayName("同一明细已有 PENDING 申诉 → IllegalStateException 拒绝重复")
        void shouldRejectDuplicatePending() {
            when(detailRepository.findById(7L)).thenReturn(Optional.of(detail(7L, null)));
            when(appealRepository.findBySubmissionDetailId(7L))
                    .thenReturn(List.of(pendingAppeal(1L, 1L)));
            assertThatThrownBy(() -> service.submitAppeal(7L, 1L, "张三", "理由", null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("已存在待审核的申诉");
            verify(appealRepository, never()).save(any());
        }

        @Test
        @DisplayName("无关联 submission 时直接创建 PENDING 申诉并发事件 + 触发器 + 指标")
        void shouldCreateAppealWithoutSubmissionChain() {
            when(detailRepository.findById(7L)).thenReturn(Optional.of(detail(7L, null)));
            when(appealRepository.findBySubmissionDetailId(7L)).thenReturn(List.of());
            when(appealRepository.save(any(InspAppeal.class))).thenAnswer(inv -> {
                InspAppeal a = inv.getArgument(0);
                return InspAppeal.builder().id(500L).appealCode(a.getAppealCode())
                        .submissionDetailId(a.getSubmissionDetailId())
                        .submitterUserId(a.getSubmitterUserId())
                        .status(AppealStatus.PENDING).build();
            });

            InspAppeal saved = service.submitAppeal(7L, 1L, "张三", "理由", "att", new BigDecimal("3"));

            assertThat(saved.getId()).isEqualTo(500L);
            assertThat(saved.getStatus()).isEqualTo(AppealStatus.PENDING);
            verify(eventPublisher).publishAll(any());
            verify(triggerService).fire(eq(InspectionTriggerPoints.INSP_APPEAL_SUBMITTED), any());
            verify(metrics).appealSubmitted();
        }

        @Test
        @DisplayName("解析 submission→task 链: 任务未发布 → 拒绝申诉")
        void shouldRejectWhenTaskNotPublished() {
            when(detailRepository.findById(7L)).thenReturn(Optional.of(detail(7L, 20L)));
            when(appealRepository.findBySubmissionDetailId(7L)).thenReturn(List.of());
            when(submissionRepository.findById(20L))
                    .thenReturn(Optional.of(submission(20L, 30L, TargetType.ORG, 8L)));
            when(taskRepository.findById(30L))
                    .thenReturn(Optional.of(task(30L, 40L, null)));

            assertThatThrownBy(() -> service.submitAppeal(7L, 1L, "张三", "理由", null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("任务尚未发布");
        }

        @Test
        @DisplayName("超出申诉时效窗 → 拒绝申诉")
        void shouldRejectWhenWindowExpired() {
            when(detailRepository.findById(7L)).thenReturn(Optional.of(detail(7L, 20L)));
            when(appealRepository.findBySubmissionDetailId(7L)).thenReturn(List.of());
            when(submissionRepository.findById(20L))
                    .thenReturn(Optional.of(submission(20L, 30L, TargetType.PLACE, 8L)));
            // 发布于 100 天前, 默认窗 7 天 → 早过期
            when(taskRepository.findById(30L))
                    .thenReturn(Optional.of(task(30L, 40L, LocalDateTime.now().minusDays(100))));
            when(projectRepository.findById(40L))
                    .thenReturn(Optional.of(InspProject.builder().id(40L)
                            .projectCode("P").projectName("P").status(ProjectStatus.PUBLISHED).build()));

            assertThatThrownBy(() -> service.submitAppeal(7L, 1L, "张三", "理由", null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("申诉时效已过");
        }

        @Test
        @DisplayName("任务在时效窗内: 成功创建, ORG 目标映射为 ORG_UNIT subjectType")
        void shouldCreateWhenWithinWindow() {
            when(detailRepository.findById(7L)).thenReturn(Optional.of(detail(7L, 20L)));
            when(appealRepository.findBySubmissionDetailId(7L)).thenReturn(List.of());
            when(submissionRepository.findById(20L))
                    .thenReturn(Optional.of(submission(20L, 30L, TargetType.ORG, 8L)));
            when(taskRepository.findById(30L))
                    .thenReturn(Optional.of(task(30L, 40L, LocalDateTime.now().minusDays(1))));
            when(projectRepository.findById(40L))
                    .thenReturn(Optional.of(InspProject.builder().id(40L)
                            .projectCode("P").projectName("P")
                            .appealWindowDays(30).status(ProjectStatus.PUBLISHED).build()));
            ArgumentCaptor<InspAppeal> captor = ArgumentCaptor.forClass(InspAppeal.class);
            when(appealRepository.save(captor.capture())).thenAnswer(inv -> {
                InspAppeal a = inv.getArgument(0);
                return InspAppeal.builder().id(501L).appealCode(a.getAppealCode())
                        .submissionDetailId(a.getSubmissionDetailId())
                        .submitterUserId(a.getSubmitterUserId())
                        .status(AppealStatus.PENDING).build();
            });

            InspAppeal saved = service.submitAppeal(7L, 1L, "张三", "理由", null, null);

            assertThat(saved.getId()).isEqualTo(501L);
            assertThat(captor.getValue().getSubjectType()).isEqualTo("ORG_UNIT");
            assertThat(captor.getValue().getSubjectId()).isEqualTo(8L);
            assertThat(captor.getValue().getTaskId()).isEqualTo(30L);
            assertThat(captor.getValue().getProjectId()).isEqualTo(40L);
        }

        @Test
        @DisplayName("DB 唯一约束命中 (DuplicateKeyException) → 转译为重复提交 IllegalStateException")
        void shouldTranslateDuplicateKey() {
            when(detailRepository.findById(7L)).thenReturn(Optional.of(detail(7L, null)));
            when(appealRepository.findBySubmissionDetailId(7L)).thenReturn(List.of());
            when(appealRepository.save(any(InspAppeal.class)))
                    .thenThrow(new DuplicateKeyException("uk hit"));

            assertThatThrownBy(() -> service.submitAppeal(7L, 1L, "张三", "理由", null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("已存在待审核的申诉")
                    .hasCauseInstanceOf(DuplicateKeyException.class);
        }

        @Test
        @DisplayName("触发器服务为 null 时不影响主流程")
        void shouldWorkWhenTriggerServiceNull() {
            InspAppealApplicationService noTrigger = new InspAppealApplicationService(
                    appealRepository, detailRepository, submissionRepository, taskRepository,
                    projectRepository, eventPublisher, auditLogger, metrics);
            when(detailRepository.findById(7L)).thenReturn(Optional.of(detail(7L, null)));
            when(appealRepository.findBySubmissionDetailId(7L)).thenReturn(List.of());
            when(appealRepository.save(any(InspAppeal.class))).thenAnswer(inv -> {
                InspAppeal a = inv.getArgument(0);
                return InspAppeal.builder().id(502L).appealCode(a.getAppealCode())
                        .submissionDetailId(a.getSubmissionDetailId())
                        .submitterUserId(a.getSubmitterUserId())
                        .status(AppealStatus.PENDING).build();
            });

            InspAppeal saved = noTrigger.submitAppeal(7L, 1L, "张三", "理由", null, null);

            assertThat(saved.getId()).isEqualTo(502L);
            verify(metrics).appealSubmitted();
        }

        @Test
        @DisplayName("触发器抛异常被吞掉, 不影响申诉创建")
        void shouldSwallowTriggerException() {
            when(detailRepository.findById(7L)).thenReturn(Optional.of(detail(7L, null)));
            when(appealRepository.findBySubmissionDetailId(7L)).thenReturn(List.of());
            when(appealRepository.save(any(InspAppeal.class))).thenAnswer(inv -> {
                InspAppeal a = inv.getArgument(0);
                return InspAppeal.builder().id(503L).appealCode(a.getAppealCode())
                        .submissionDetailId(a.getSubmissionDetailId())
                        .submitterUserId(a.getSubmitterUserId())
                        .status(AppealStatus.PENDING).build();
            });
            org.mockito.Mockito.doThrow(new RuntimeException("trigger boom"))
                    .when(triggerService).fire(anyString(), any());

            InspAppeal saved = service.submitAppeal(7L, 1L, "张三", "理由", null, null);

            assertThat(saved.getId()).isEqualTo(503L);
            verify(metrics).appealSubmitted();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("approve")
    class ApproveTests {

        @Test
        @DisplayName("申诉不存在 → IllegalArgumentException")
        void shouldRejectWhenMissing() {
            when(appealRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.approve(99L, 2L, "审核员", "ok", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("申诉不存在");
        }

        @Test
        @DisplayName("成功通过: 状态变 APPROVED + 事件 + 触发器 + 审计 + 指标")
        void shouldApproveSuccessfully() {
            InspAppeal a = pendingAppeal(10L, 1L);
            when(appealRepository.findById(10L)).thenReturn(Optional.of(a));
            when(appealRepository.save(any(InspAppeal.class))).thenAnswer(inv -> inv.getArgument(0));

            InspAppeal saved = service.approve(10L, 2L, "审核员", "同意", new BigDecimal("5"));

            assertThat(saved.getStatus()).isEqualTo(AppealStatus.APPROVED);
            assertThat(saved.getReviewerId()).isEqualTo(2L);
            assertThat(saved.getFinalAdjustment()).isEqualByComparingTo("5");
            verify(eventPublisher).publishAll(any());
            verify(triggerService).fire(eq(InspectionTriggerPoints.INSP_APPEAL_REVIEWED), any());
            verify(auditLogger).log(eq("InspAppeal"), eq(10L), eq("APL-1"),
                    eq("APPEAL_APPROVED"), eq("同意"), any(Map.class));
            verify(metrics).appealResolved("approved");
        }

        @Test
        @DisplayName("非 PENDING 状态申诉 → 聚合根抛 IllegalStateException")
        void shouldRejectNonPending() {
            InspAppeal a = InspAppeal.builder().id(10L).appealCode("APL-1")
                    .submitterUserId(1L).status(AppealStatus.WITHDRAWN).build();
            when(appealRepository.findById(10L)).thenReturn(Optional.of(a));
            assertThatThrownBy(() -> service.approve(10L, 2L, "审核员", "ok", null))
                    .isInstanceOf(IllegalStateException.class);
            verify(appealRepository, never()).save(any());
        }
    }

    // ============================================================
    @Nested
    @DisplayName("reject")
    class RejectTests {

        @Test
        @DisplayName("申诉不存在 → IllegalArgumentException")
        void shouldRejectWhenMissing() {
            when(appealRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.reject(99L, 2L, "审核员", "驳回理由"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("成功驳回: 状态变 REJECTED + 事件 + 触发器 + 审计 + 指标")
        void shouldRejectSuccessfully() {
            InspAppeal a = pendingAppeal(11L, 1L);
            when(appealRepository.findById(11L)).thenReturn(Optional.of(a));
            when(appealRepository.save(any(InspAppeal.class))).thenAnswer(inv -> inv.getArgument(0));

            InspAppeal saved = service.reject(11L, 2L, "审核员", "证据不足");

            assertThat(saved.getStatus()).isEqualTo(AppealStatus.REJECTED);
            assertThat(saved.getReviewerComment()).isEqualTo("证据不足");
            verify(eventPublisher).publishAll(any());
            verify(triggerService).fire(eq(InspectionTriggerPoints.INSP_APPEAL_REVIEWED), any());
            verify(auditLogger).log(eq("InspAppeal"), eq(11L), eq("APL-1"),
                    eq("APPEAL_REJECTED"), eq("证据不足"), any(Map.class));
            verify(metrics).appealResolved("rejected");
        }

        @Test
        @DisplayName("驳回未填理由 → 聚合根抛 IllegalArgumentException")
        void shouldRejectWhenNoComment() {
            InspAppeal a = pendingAppeal(11L, 1L);
            when(appealRepository.findById(11L)).thenReturn(Optional.of(a));
            assertThatThrownBy(() -> service.reject(11L, 2L, "审核员", "  "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("withdraw")
    class WithdrawTests {

        @Test
        @DisplayName("申诉不存在 → IllegalArgumentException")
        void shouldRejectWhenMissing() {
            when(appealRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.withdraw(99L, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("提交人本人撤回: 状态变 WITHDRAWN")
        void shouldWithdrawByOwner() {
            InspAppeal a = pendingAppeal(12L, 1L);
            when(appealRepository.findById(12L)).thenReturn(Optional.of(a));
            when(appealRepository.save(any(InspAppeal.class))).thenAnswer(inv -> inv.getArgument(0));

            InspAppeal saved = service.withdraw(12L, 1L);

            assertThat(saved.getStatus()).isEqualTo(AppealStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("非提交人撤回 → 聚合根抛 IllegalStateException")
        void shouldRejectWithdrawByOther() {
            InspAppeal a = pendingAppeal(12L, 1L);
            when(appealRepository.findById(12L)).thenReturn(Optional.of(a));
            assertThatThrownBy(() -> service.withdraw(12L, 999L))
                    .isInstanceOf(IllegalStateException.class);
            verify(appealRepository, never()).save(any());
        }
    }

    // ============================================================
    @Nested
    @DisplayName("查询方法")
    class QueryTests {

        @Test
        @DisplayName("getAppeal 透传 repository 结果")
        void shouldGetAppeal() {
            InspAppeal a = pendingAppeal(20L, 1L);
            when(appealRepository.findById(20L)).thenReturn(Optional.of(a));
            assertThat(service.getAppeal(20L)).containsSame(a);
        }

        @Test
        @DisplayName("getAppeal 不存在返回空 Optional")
        void shouldGetEmptyAppeal() {
            when(appealRepository.findById(20L)).thenReturn(Optional.empty());
            assertThat(service.getAppeal(20L)).isEmpty();
        }

        @Test
        @DisplayName("listMyAppeals 按提交人查询")
        void shouldListMyAppeals() {
            InspAppeal a = pendingAppeal(21L, 5L);
            when(appealRepository.findBySubmitterUserId(5L)).thenReturn(List.of(a));
            assertThat(service.listMyAppeals(5L)).containsExactly(a);
        }

        @Test
        @DisplayName("listPending 按 PENDING 状态查询")
        void shouldListPending() {
            InspAppeal a = pendingAppeal(22L, 1L);
            when(appealRepository.findByStatus(AppealStatus.PENDING)).thenReturn(List.of(a));
            assertThat(service.listPending()).containsExactly(a);
            verify(appealRepository).findByStatus(AppealStatus.PENDING);
        }

        @Test
        @DisplayName("listByProject 按项目查询")
        void shouldListByProject() {
            InspAppeal a = pendingAppeal(23L, 1L);
            when(appealRepository.findByProjectId(40L)).thenReturn(List.of(a));
            assertThat(service.listByProject(40L)).containsExactly(a);
        }

        @Test
        @DisplayName("查询方法不触碰审计/指标/事件")
        void queriesAreSideEffectFree() {
            when(appealRepository.findByStatus(AppealStatus.PENDING)).thenReturn(List.of());
            service.listPending();
            verifyNoInteractions(auditLogger, metrics, eventPublisher);
            verify(appealRepository, times(1)).findByStatus(any(AppealStatus.class));
        }
    }
}
