package com.school.management.application.inspection;

import com.school.management.application.event.TriggerService;
import com.school.management.domain.inspection.event.InspectionTriggerPoints;
import com.school.management.domain.inspection.model.corrective.CasePriority;
import com.school.management.domain.inspection.model.corrective.CaseStatus;
import com.school.management.domain.inspection.model.corrective.CorrectiveCase;
import com.school.management.domain.inspection.model.corrective.CorrectiveSubtask;
import com.school.management.domain.inspection.model.execution.InspProject;
import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.repository.CorrectiveCaseRepository;
import com.school.management.domain.inspection.repository.CorrectiveSubtaskRepository;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CorrectiveCaseApplicationService 应用服务单测.
 *
 * 用 Mockito 隔离 repository / 事件发布 / 触发服务 / 审计 / 事务模板,
 * 验证应用服务编排: CRUD / lifecycle / 离职重派 / 子任务管理.
 * 聚合根状态机本身已有独立测试, 此处聚焦服务层 orchestration.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CorrectiveCaseApplicationService 应用服务")
class CorrectiveCaseApplicationServiceTest {

    @Mock CorrectiveCaseRepository caseRepository;
    @Mock CorrectiveSubtaskRepository subtaskRepository;
    @Mock SpringDomainEventPublisher eventPublisher;
    @Mock InspectionAuditLogger auditLogger;
    @Mock InspProjectRepository projectRepository;
    @Mock InspSubmissionRepository submissionRepository;
    @Mock TransactionTemplate transactionTemplate;
    @Mock JdbcTemplate jdbcTemplate;
    @Mock TriggerService triggerService;

    CorrectiveCaseApplicationService service;

    @BeforeEach
    void setUp() {
        service = new CorrectiveCaseApplicationService(
                caseRepository, subtaskRepository, eventPublisher, auditLogger,
                projectRepository, submissionRepository, transactionTemplate, jdbcTemplate);
        // triggerService is @Autowired(required=false) — inject via reflection
        org.springframework.test.util.ReflectionTestUtils.setField(service, "triggerService", triggerService);
    }

    /** 构造一个处于指定状态的案例. */
    private CorrectiveCase caseInState(Long id, CaseStatus status) {
        return CorrectiveCase.reconstruct(CorrectiveCase.builder()
                .id(id).caseCode("CASE-" + id).issueDescription("问题")
                .priority(CasePriority.HIGH).status(status)
                .targetType("ORG").targetId(7L).targetName("一班")
                .escalationLevel(0).createdBy(1L));
    }

    private CorrectiveSubtask subtaskInState(Long id, String status) {
        return CorrectiveSubtask.reconstruct(CorrectiveSubtask.builder()
                .id(id).caseId(100L).subtaskName("子任务").status(status).createdBy(1L));
    }

    // ============================================================
    @Nested
    @DisplayName("createCase — 创建整改案例")
    class CreateCaseTests {

        @Test
        @DisplayName("从 submission 继承 orgUnitId 并保存为 OPEN, 发布创建事件 + 触发点")
        void shouldInheritOrgUnitFromSubmission() {
            InspSubmission sub = mock(InspSubmission.class);
            when(sub.getOrgUnitId()).thenReturn(55L);
            when(submissionRepository.findById(9L)).thenReturn(Optional.of(sub));
            when(caseRepository.save(any())).thenAnswer(inv -> {
                CorrectiveCase c = inv.getArgument(0);
                return CorrectiveCase.reconstruct(CorrectiveCase.builder()
                        .id(200L).caseCode(c.getCaseCode()).issueDescription(c.getIssueDescription())
                        .priority(c.getPriority()).status(c.getStatus()).orgUnitId(c.getOrgUnitId()));
            });

            CorrectiveCase result = service.createCase("CASE-X", "墙皮脱落",
                    CasePriority.HIGH, 9L, null, 3L, 4L,
                    "ORG", 7L, "一班", "重新粉刷",
                    LocalDateTime.of(2026, 6, 1, 10, 0), 1L);

            ArgumentCaptor<CorrectiveCase> cap = ArgumentCaptor.forClass(CorrectiveCase.class);
            verify(caseRepository).save(cap.capture());
            assertThat(cap.getValue().getOrgUnitId()).isEqualTo(55L);
            assertThat(cap.getValue().getStatus()).isEqualTo(CaseStatus.OPEN);
            assertThat(result.getId()).isEqualTo(200L);
            verify(eventPublisher).publish(any());
            verify(triggerService).fire(eq(InspectionTriggerPoints.INSP_CORRECTIVE_CREATED), any());
        }

        @Test
        @DisplayName("无 submission 但 targetType=ORG 时, orgUnitId 兜底取 targetId")
        void shouldFallbackOrgUnitToTargetIdWhenOrg() {
            when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createCase("CASE-Y", "问题", CasePriority.LOW, null, null, null, null,
                    "ORG", 88L, "二班", null, null, 1L);

            ArgumentCaptor<CorrectiveCase> cap = ArgumentCaptor.forClass(CorrectiveCase.class);
            verify(caseRepository).save(cap.capture());
            assertThat(cap.getValue().getOrgUnitId()).isEqualTo(88L);
        }

        @Test
        @DisplayName("submission 查不到 orgUnitId 且非 ORG 目标时 orgUnitId 为 null")
        void shouldLeaveOrgUnitNullWhenNoSource() {
            when(submissionRepository.findById(9L)).thenReturn(Optional.empty());
            when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.createCase("CASE-Z", "问题", CasePriority.MEDIUM, 9L, null, null, null,
                    "PLACE", 1L, "教室", null, null, 1L);

            ArgumentCaptor<CorrectiveCase> cap = ArgumentCaptor.forClass(CorrectiveCase.class);
            verify(caseRepository).save(cap.capture());
            assertThat(cap.getValue().getOrgUnitId()).isNull();
        }

        @Test
        @DisplayName("triggerService 为 null 时不触发, 也不影响保存")
        void shouldNotFailWhenTriggerServiceNull() {
            org.springframework.test.util.ReflectionTestUtils.setField(service, "triggerService", null);
            when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CorrectiveCase result = service.createCase("CASE-N", "问题", CasePriority.HIGH,
                    null, null, null, null, "ORG", 1L, "班", null, null, 1L);

            assertThat(result).isNotNull();
            verify(eventPublisher).publish(any());
        }

        @Test
        @DisplayName("triggerService.fire 抛异常被吞掉, 不影响主流程")
        void shouldSwallowTriggerException() {
            when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                    .when(triggerService).fire(anyString(), any());

            CorrectiveCase result = service.createCase("CASE-T", "问题", CasePriority.HIGH,
                    null, null, null, null, "ORG", 1L, "班", null, null, 1L);

            assertThat(result).isNotNull();
            verify(caseRepository).save(any());
        }
    }

    // ============================================================
    @Nested
    @DisplayName("查询方法")
    class QueryTests {

        @Test
        @DisplayName("getCase 委托 repository.findById")
        void shouldGetCaseById() {
            CorrectiveCase c = caseInState(1L, CaseStatus.OPEN);
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
            assertThat(service.getCase(1L)).containsSame(c);
        }

        @Test
        @DisplayName("getCaseByCaseCode 委托 repository.findByCaseCode")
        void shouldGetCaseByCode() {
            CorrectiveCase c = caseInState(1L, CaseStatus.OPEN);
            when(caseRepository.findByCaseCode("CASE-1")).thenReturn(Optional.of(c));
            assertThat(service.getCaseByCaseCode("CASE-1")).containsSame(c);
        }

        @Test
        @DisplayName("listByProject 委托 repository.findByProjectId")
        void shouldListByProject() {
            List<CorrectiveCase> list = List.of(caseInState(1L, CaseStatus.OPEN));
            when(caseRepository.findByProjectId(3L)).thenReturn(list);
            assertThat(service.listByProject(3L)).isEqualTo(list);
        }

        @Test
        @DisplayName("listBySubmission 委托 repository.findBySubmissionId")
        void shouldListBySubmission() {
            List<CorrectiveCase> list = List.of(caseInState(1L, CaseStatus.OPEN));
            when(caseRepository.findBySubmissionId(9L)).thenReturn(list);
            assertThat(service.listBySubmission(9L)).isEqualTo(list);
        }

        @Test
        @DisplayName("listByAssignee 委托 repository.findByAssigneeId")
        void shouldListByAssignee() {
            List<CorrectiveCase> list = List.of(caseInState(1L, CaseStatus.ASSIGNED));
            when(caseRepository.findByAssigneeId(5L)).thenReturn(list);
            assertThat(service.listByAssignee(5L)).isEqualTo(list);
        }

        @Test
        @DisplayName("listByStatus 委托 repository.findByStatus")
        void shouldListByStatus() {
            List<CorrectiveCase> list = List.of(caseInState(1L, CaseStatus.SUBMITTED));
            when(caseRepository.findByStatus(CaseStatus.SUBMITTED)).thenReturn(list);
            assertThat(service.listByStatus(CaseStatus.SUBMITTED)).isEqualTo(list);
        }

        @Test
        @DisplayName("listByTask 委托 repository.findByTaskId")
        void shouldListByTask() {
            List<CorrectiveCase> list = List.of(caseInState(1L, CaseStatus.OPEN));
            when(caseRepository.findByTaskId(4L)).thenReturn(list);
            assertThat(service.listByTask(4L)).isEqualTo(list);
        }

        @Test
        @DisplayName("listAll 委托 repository.findAll")
        void shouldListAll() {
            List<CorrectiveCase> list = List.of(caseInState(1L, CaseStatus.OPEN));
            when(caseRepository.findAll()).thenReturn(list);
            assertThat(service.listAll()).isEqualTo(list);
        }

        @Test
        @DisplayName("listOverdue 用当前时间委托 repository.findOverdue")
        void shouldListOverdue() {
            List<CorrectiveCase> list = List.of(caseInState(1L, CaseStatus.IN_PROGRESS));
            when(caseRepository.findOverdue(any(LocalDateTime.class))).thenReturn(list);
            assertThat(service.listOverdue()).isEqualTo(list);
            verify(caseRepository).findOverdue(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("deleteCase 委托 repository.deleteById")
        void shouldDeleteCase() {
            service.deleteCase(5L);
            verify(caseRepository).deleteById(5L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("assignCase — 分配责任人")
    class AssignTests {

        @Test
        @DisplayName("OPEN 案例分配后状态变 ASSIGNED, 发布事件 + 触发点")
        void shouldAssign() {
            CorrectiveCase c = caseInState(1L, CaseStatus.OPEN);
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

            CorrectiveCase result = service.assignCase(1L, 5L, "张三");

            assertThat(result.getStatus()).isEqualTo(CaseStatus.ASSIGNED);
            assertThat(result.getAssigneeId()).isEqualTo(5L);
            verify(caseRepository).save(c);
            verify(eventPublisher).publishAll(any());
            verify(triggerService).fire(eq(InspectionTriggerPoints.INSP_CORRECTIVE_ASSIGNED), any());
        }

        @Test
        @DisplayName("案例不存在抛 IllegalArgumentException")
        void shouldRejectIfNotFound() {
            when(caseRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.assignCase(999L, 5L, "张三"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("整改案例不存在");
        }

        @Test
        @DisplayName("非 OPEN/REJECTED 状态分配抛 IllegalStateException")
        void shouldRejectInvalidState() {
            CorrectiveCase c = caseInState(1L, CaseStatus.SUBMITTED);
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
            assertThatThrownBy(() -> service.assignCase(1L, 5L, "张三"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("startWork / submitCorrection")
    class WorkTests {

        @Test
        @DisplayName("startWork: ASSIGNED → IN_PROGRESS")
        void shouldStartWork() {
            CorrectiveCase c = caseInState(1L, CaseStatus.ASSIGNED);
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
            when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CorrectiveCase result = service.startWork(1L);
            assertThat(result.getStatus()).isEqualTo(CaseStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("startWork 不存在抛 IllegalArgumentException")
        void shouldRejectStartNotFound() {
            when(caseRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.startWork(9L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("startWork 非 ASSIGNED 抛 IllegalStateException")
        void shouldRejectStartInvalidState() {
            CorrectiveCase c = caseInState(1L, CaseStatus.OPEN);
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
            assertThatThrownBy(() -> service.startWork(1L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("submitCorrection: IN_PROGRESS → SUBMITTED, 记录整改说明 + 发事件")
        void shouldSubmitCorrection() {
            CorrectiveCase c = caseInState(1L, CaseStatus.IN_PROGRESS);
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

            CorrectiveCase result = service.submitCorrection(1L, "已修复", List.of(11L, 22L));

            assertThat(result.getStatus()).isEqualTo(CaseStatus.SUBMITTED);
            assertThat(result.getCorrectionNote()).isEqualTo("已修复");
            assertThat(result.getCorrectionEvidenceIds()).containsExactly(11L, 22L);
            verify(eventPublisher).publishAll(any());
        }

        @Test
        @DisplayName("submitCorrection 不存在抛 IllegalArgumentException")
        void shouldRejectSubmitNotFound() {
            when(caseRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.submitCorrection(9L, "x", List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("verifyCase / rejectCase")
    class VerifyRejectTests {

        @Test
        @DisplayName("verifyCase: SUBMITTED → VERIFIED, 触发 VERIFIED 点")
        void shouldVerify() {
            CorrectiveCase c = caseInState(1L, CaseStatus.SUBMITTED);
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

            CorrectiveCase result = service.verifyCase(1L, 9L, "李四", "通过");

            assertThat(result.getStatus()).isEqualTo(CaseStatus.VERIFIED);
            assertThat(result.getVerifierId()).isEqualTo(9L);
            verify(eventPublisher).publishAll(any());
            verify(triggerService).fire(eq(InspectionTriggerPoints.INSP_CORRECTIVE_VERIFIED), any());
        }

        @Test
        @DisplayName("verifyCase 不存在抛 IllegalArgumentException")
        void shouldRejectVerifyNotFound() {
            when(caseRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.verifyCase(9L, 1L, "x", "y"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("verifyCase 非 SUBMITTED 抛 IllegalStateException")
        void shouldRejectVerifyInvalidState() {
            CorrectiveCase c = caseInState(1L, CaseStatus.OPEN);
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
            assertThatThrownBy(() -> service.verifyCase(1L, 1L, "x", "y"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("rejectCase: SUBMITTED → REJECTED, 触发 REJECTED 点")
        void shouldReject() {
            CorrectiveCase c = caseInState(1L, CaseStatus.SUBMITTED);
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

            CorrectiveCase result = service.rejectCase(1L, 9L, "李四", "证据不足");

            assertThat(result.getStatus()).isEqualTo(CaseStatus.REJECTED);
            assertThat(result.getVerificationNote()).isEqualTo("证据不足");
            verify(eventPublisher).publishAll(any());
            verify(triggerService).fire(eq(InspectionTriggerPoints.INSP_CORRECTIVE_REJECTED), any());
        }

        @Test
        @DisplayName("rejectCase 不存在抛 IllegalArgumentException")
        void shouldRejectRejectNotFound() {
            when(caseRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.rejectCase(9L, 1L, "x", "y"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("closeCase / escalateCase")
    class CloseEscalateTests {

        @Test
        @DisplayName("closeCase: VERIFIED → CLOSED")
        void shouldClose() {
            CorrectiveCase c = caseInState(1L, CaseStatus.VERIFIED);
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
            when(caseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CorrectiveCase result = service.closeCase(1L);
            assertThat(result.getStatus()).isEqualTo(CaseStatus.CLOSED);
        }

        @Test
        @DisplayName("closeCase 不存在抛 IllegalArgumentException")
        void shouldRejectCloseNotFound() {
            when(caseRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.closeCase(9L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("escalateCase: 用项目级 maxEscalationLevel 升级, escalationLevel +1")
        void shouldEscalateWithProjectMax() {
            CorrectiveCase c = CorrectiveCase.reconstruct(CorrectiveCase.builder()
                    .id(1L).caseCode("CASE-1").issueDescription("问题")
                    .priority(CasePriority.HIGH).status(CaseStatus.ASSIGNED)
                    .projectId(3L).escalationLevel(0).createdBy(1L));
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
            InspProject project = mock(InspProject.class);
            when(project.getMaxEscalationLevel()).thenReturn(5);
            when(projectRepository.findById(3L)).thenReturn(Optional.of(project));

            CorrectiveCase result = service.escalateCase(1L);

            assertThat(result.getEscalationLevel()).isEqualTo(1);
            assertThat(result.getStatus()).isEqualTo(CaseStatus.OPEN);
            verify(eventPublisher).publishAll(any());
        }

        @Test
        @DisplayName("escalateCase: projectId 为 null 时用系统默认上限")
        void shouldEscalateWithoutProject() {
            CorrectiveCase c = caseInState(1L, CaseStatus.ASSIGNED);
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

            CorrectiveCase result = service.escalateCase(1L);

            assertThat(result.getEscalationLevel()).isEqualTo(1);
            verify(projectRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("escalateCase 不存在抛 IllegalArgumentException")
        void shouldRejectEscalateNotFound() {
            when(caseRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.escalateCase(9L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("escalateCase 已关闭案例抛 IllegalStateException")
        void shouldRejectEscalateClosed() {
            CorrectiveCase c = caseInState(1L, CaseStatus.CLOSED);
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
            assertThatThrownBy(() -> service.escalateCase(1L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("reassignDepartedAssignee — 离职重派")
    class ReassignTests {

        /** 让 transactionTemplate.execute 直接运行回调, 复制真实行为. */
        @SuppressWarnings("unchecked")
        private void runTransactionCallbacksInline() {
            when(transactionTemplate.execute(any())).thenAnswer(inv -> {
                TransactionCallback<Object> cb = inv.getArgument(0);
                return cb.doInTransaction(mock(org.springframework.transaction.TransactionStatus.class));
            });
        }

        @Test
        @DisplayName("userId 为 null 抛 IllegalArgumentException")
        void shouldRejectNullUserId() {
            assertThatThrownBy(() -> service.reassignDepartedAssignee(null, "离职", null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("userId 不能为空");
        }

        @Test
        @DisplayName("fallback 用户不存在抛 IllegalArgumentException")
        void shouldRejectInvalidFallbackUser() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(99L)))
                    .thenReturn(0);
            assertThatThrownBy(() -> service.reassignDepartedAssignee(5L, "离职", 99L, "继任"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fallbackAssigneeId");
        }

        @Test
        @DisplayName("无可重派案例时返回 0")
        void shouldReturnZeroWhenNoCases() {
            when(caseRepository.findByAssigneeId(5L)).thenReturn(List.of());
            int affected = service.reassignDepartedAssignee(5L, "离职", null, null);
            assertThat(affected).isZero();
        }

        @Test
        @DisplayName("跳过非 ASSIGNED/IN_PROGRESS 状态的案例")
        void shouldSkipNonActiveCases() {
            CorrectiveCase done = caseInState(1L, CaseStatus.CLOSED);
            when(caseRepository.findByAssigneeId(5L)).thenReturn(List.of(done));
            int affected = service.reassignDepartedAssignee(5L, "离职", null, null);
            assertThat(affected).isZero();
            verify(transactionTemplate, never()).execute(any());
        }

        @Test
        @DisplayName("ASSIGNED 案例无 fallback: unassign 回 OPEN, 写审计, 返回受影响数")
        void shouldUnassignWithoutFallback() {
            CorrectiveCase c = CorrectiveCase.reconstruct(CorrectiveCase.builder()
                    .id(1L).caseCode("CASE-1").issueDescription("问题")
                    .priority(CasePriority.HIGH).status(CaseStatus.ASSIGNED)
                    .assigneeId(5L).assigneeName("张三").escalationLevel(0).createdBy(1L));
            when(caseRepository.findByAssigneeId(5L)).thenReturn(List.of(c));
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
            runTransactionCallbacksInline();

            int affected = service.reassignDepartedAssignee(5L, "离职", null, null);

            assertThat(affected).isEqualTo(1);
            assertThat(c.getStatus()).isEqualTo(CaseStatus.OPEN);
            assertThat(c.getAssigneeId()).isNull();
            verify(caseRepository).save(c);
            verify(eventPublisher).publishAll(any());
            verify(auditLogger).log(eq("CorrectiveCase"), eq(1L), eq("CASE-1"),
                    eq("CASE_UNASSIGNED"), eq("离职"), any(Map.class));
        }

        @Test
        @DisplayName("带 fallback: 校验用户后重派给继任者")
        void shouldReassignToFallback() {
            CorrectiveCase c = CorrectiveCase.reconstruct(CorrectiveCase.builder()
                    .id(1L).caseCode("CASE-1").issueDescription("问题")
                    .priority(CasePriority.HIGH).status(CaseStatus.IN_PROGRESS)
                    .assigneeId(5L).assigneeName("张三").escalationLevel(0).createdBy(1L));
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(99L)))
                    .thenReturn(1);
            when(caseRepository.findByAssigneeId(5L)).thenReturn(List.of(c));
            when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
            runTransactionCallbacksInline();

            int affected = service.reassignDepartedAssignee(5L, "调岗", 99L, "继任者");

            assertThat(affected).isEqualTo(1);
            assertThat(c.getStatus()).isEqualTo(CaseStatus.ASSIGNED);
            assertThat(c.getAssigneeId()).isEqualTo(99L);
            assertThat(c.getAssigneeName()).isEqualTo("继任者");
        }

        @Test
        @DisplayName("某条 case 重新查找返回空: 该条计为失败, 不增加受影响数")
        void shouldCountFreshNotFoundAsFailure() {
            CorrectiveCase c = caseInState(1L, CaseStatus.ASSIGNED);
            when(caseRepository.findByAssigneeId(5L)).thenReturn(List.of(c));
            when(caseRepository.findById(1L)).thenReturn(Optional.empty());
            runTransactionCallbacksInline();

            int affected = service.reassignDepartedAssignee(5L, "离职", null, null);
            assertThat(affected).isZero();
        }

        @Test
        @DisplayName("transactionTemplate 返回 null (回滚) 时该条不计入")
        void shouldNotCountRolledBackCase() {
            CorrectiveCase c = caseInState(1L, CaseStatus.ASSIGNED);
            when(caseRepository.findByAssigneeId(5L)).thenReturn(List.of(c));
            when(transactionTemplate.execute(any())).thenReturn(null);

            int affected = service.reassignDepartedAssignee(5L, "离职", null, null);
            assertThat(affected).isZero();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("子任务管理")
    class SubtaskTests {

        @Test
        @DisplayName("createSubtask: 校验案例存在后保存子任务为 PENDING")
        void shouldCreateSubtask() {
            when(caseRepository.findById(100L)).thenReturn(Optional.of(caseInState(100L, CaseStatus.OPEN)));
            when(subtaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CorrectiveSubtask result = service.createSubtask(100L, "排查", "描述",
                    5L, 1, LocalDate.of(2026, 6, 1), 1L);

            assertThat(result.getCaseId()).isEqualTo(100L);
            assertThat(result.getSubtaskName()).isEqualTo("排查");
            assertThat(result.getStatus()).isEqualTo("PENDING");
            verify(subtaskRepository).save(any(CorrectiveSubtask.class));
        }

        @Test
        @DisplayName("createSubtask 案例不存在抛 IllegalArgumentException")
        void shouldRejectCreateSubtaskNoCase() {
            when(caseRepository.findById(100L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.createSubtask(100L, "x", "y", 1L, 0, null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("整改案例不存在");
        }

        @Test
        @DisplayName("updateSubtask 更新详情并保存")
        void shouldUpdateSubtask() {
            CorrectiveSubtask s = subtaskInState(7L, "PENDING");
            when(subtaskRepository.findById(7L)).thenReturn(Optional.of(s));

            CorrectiveSubtask result = service.updateSubtask(7L, "新名", "新描述",
                    9L, 2, LocalDate.of(2026, 7, 1));

            assertThat(result.getSubtaskName()).isEqualTo("新名");
            assertThat(result.getAssigneeId()).isEqualTo(9L);
            verify(subtaskRepository).save(s);
        }

        @Test
        @DisplayName("updateSubtask 不存在抛 IllegalArgumentException")
        void shouldRejectUpdateSubtaskNotFound() {
            when(subtaskRepository.findById(7L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateSubtask(7L, "x", "y", 1L, 0, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("子任务不存在");
        }

        @Test
        @DisplayName("startSubtask: PENDING → IN_PROGRESS")
        void shouldStartSubtask() {
            CorrectiveSubtask s = subtaskInState(7L, "PENDING");
            when(subtaskRepository.findById(7L)).thenReturn(Optional.of(s));

            CorrectiveSubtask result = service.startSubtask(7L);
            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            verify(subtaskRepository).save(s);
        }

        @Test
        @DisplayName("startSubtask 不存在抛 IllegalArgumentException")
        void shouldRejectStartSubtaskNotFound() {
            when(subtaskRepository.findById(7L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.startSubtask(7L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("startSubtask 非 PENDING 抛 IllegalStateException")
        void shouldRejectStartSubtaskInvalidState() {
            CorrectiveSubtask s = subtaskInState(7L, "COMPLETED");
            when(subtaskRepository.findById(7L)).thenReturn(Optional.of(s));
            assertThatThrownBy(() -> service.startSubtask(7L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("completeSubtask: IN_PROGRESS → COMPLETED")
        void shouldCompleteSubtask() {
            CorrectiveSubtask s = subtaskInState(7L, "IN_PROGRESS");
            when(subtaskRepository.findById(7L)).thenReturn(Optional.of(s));

            CorrectiveSubtask result = service.completeSubtask(7L);
            assertThat(result.getStatus()).isEqualTo("COMPLETED");
            assertThat(result.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("completeSubtask 不存在抛 IllegalArgumentException")
        void shouldRejectCompleteSubtaskNotFound() {
            when(subtaskRepository.findById(7L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.completeSubtask(7L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("blockSubtask: PENDING → BLOCKED")
        void shouldBlockSubtask() {
            CorrectiveSubtask s = subtaskInState(7L, "PENDING");
            when(subtaskRepository.findById(7L)).thenReturn(Optional.of(s));

            CorrectiveSubtask result = service.blockSubtask(7L);
            assertThat(result.getStatus()).isEqualTo("BLOCKED");
        }

        @Test
        @DisplayName("blockSubtask 已完成抛 IllegalStateException")
        void shouldRejectBlockCompleted() {
            CorrectiveSubtask s = subtaskInState(7L, "COMPLETED");
            when(subtaskRepository.findById(7L)).thenReturn(Optional.of(s));
            assertThatThrownBy(() -> service.blockSubtask(7L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("blockSubtask 不存在抛 IllegalArgumentException")
        void shouldRejectBlockSubtaskNotFound() {
            when(subtaskRepository.findById(7L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.blockSubtask(7L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getSubtasks 委托 repository.findByCaseId")
        void shouldGetSubtasks() {
            List<CorrectiveSubtask> list = List.of(subtaskInState(1L, "PENDING"));
            when(subtaskRepository.findByCaseId(100L)).thenReturn(list);
            assertThat(service.getSubtasks(100L)).isEqualTo(list);
        }

        @Test
        @DisplayName("deleteSubtask 委托 repository.deleteById")
        void shouldDeleteSubtask() {
            service.deleteSubtask(7L);
            verify(subtaskRepository).deleteById(7L);
        }
    }
}
