package com.school.management.domain.task.model.aggregate;

import com.school.management.domain.task.model.entity.TaskSubmission;
import com.school.management.domain.task.model.valueobject.TaskPriority;
import com.school.management.domain.task.model.valueobject.TaskStatus;
import com.school.management.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务聚合根单元测试
 * 测试任务的创建、接受、提交、审批等完整工作流
 */
@DisplayName("任务聚合根测试")
class TaskTest {

    private static final String TASK_CODE = "TASK-20260103-001";
    private static final String TITLE = "完成月度报表";
    private static final String DESCRIPTION = "请完成2026年1月的月度考核报表";
    private static final Long ASSIGNER_ID = 1L;
    private static final String ASSIGNER_NAME = "张经理";
    private static final Long DEPARTMENT_ID = 10L;
    private static final Long ASSIGNEE_ID = 100L;
    private static final String ASSIGNEE_NAME = "李员工";
    private static final Long APPROVER_ID = 200L;
    private static final String APPROVER_NAME = "王主管";

    private Task task;

    @BeforeEach
    void setUp() {
        task = createValidTask();
    }

    private Task createValidTask() {
        return Task.create(
            TASK_CODE,
            TITLE,
            DESCRIPTION,
            TaskPriority.NORMAL,
            ASSIGNER_ID,
            ASSIGNER_NAME,
            DEPARTMENT_ID,
            LocalDateTime.now().plusDays(7),
            Arrays.asList(ASSIGNEE_ID),
            1  // assignType
        );
    }

    @Nested
    @DisplayName("创建任务测试")
    class CreateTaskTest {

        @Test
        @DisplayName("成功创建任务")
        void shouldCreateTaskSuccessfully() {
            assertNotNull(task);
            assertEquals(TASK_CODE, task.getTaskCode());
            assertEquals(TITLE, task.getTitle());
            assertEquals(DESCRIPTION, task.getDescription());
            assertEquals(TaskPriority.NORMAL, task.getPriority());
            assertEquals(ASSIGNER_ID, task.getAssignerId());
            assertEquals(ASSIGNER_NAME, task.getAssignerName());
            assertEquals(DEPARTMENT_ID, task.getDepartmentId());
            assertEquals(TaskStatus.PENDING, task.getStatus());
            assertEquals(1, task.getTargetIds().size());
            assertTrue(task.getTargetIds().contains(ASSIGNEE_ID));
            assertNotNull(task.getCreatedAt());
            assertNotNull(task.getUpdatedAt());
            assertFalse(task.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("创建紧急优先级任务")
        void shouldCreateUrgentPriorityTask() {
            Task urgentPriorityTask = Task.create(
                TASK_CODE,
                TITLE,
                DESCRIPTION,
                TaskPriority.URGENT,
                ASSIGNER_ID,
                ASSIGNER_NAME,
                DEPARTMENT_ID,
                LocalDateTime.now().plusDays(3),
                Arrays.asList(ASSIGNEE_ID),
                1
            );

            assertEquals(TaskPriority.URGENT, urgentPriorityTask.getPriority());
        }

        @Test
        @DisplayName("创建多人任务")
        void shouldCreateMultiAssigneeTask() {
            List<Long> assignees = Arrays.asList(100L, 101L, 102L);
            Task multiTask = Task.create(
                TASK_CODE,
                TITLE,
                DESCRIPTION,
                TaskPriority.NORMAL,
                ASSIGNER_ID,
                ASSIGNER_NAME,
                DEPARTMENT_ID,
                LocalDateTime.now().plusDays(7),
                assignees,
                2
            );

            assertEquals(3, multiTask.getTargetIds().size());
        }
    }

    @Nested
    @DisplayName("接受任务测试")
    class AcceptTaskTest {

        @Test
        @DisplayName("成功接受任务")
        void shouldAcceptTaskSuccessfully() {
            task.clearDomainEvents();

            task.accept(ASSIGNEE_ID);

            assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
            assertNotNull(task.getAcceptedAt());
            assertFalse(task.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("非PENDING状态不能接受")
        void shouldFailToAcceptNonPendingTask() {
            task.accept(ASSIGNEE_ID);

            assertThrows(BusinessException.class, () ->
                task.accept(ASSIGNEE_ID)
            );
        }
    }

    @Nested
    @DisplayName("提交任务测试")
    class SubmitTaskTest {

        @BeforeEach
        void acceptTask() {
            task.accept(ASSIGNEE_ID);
            task.clearDomainEvents();
        }

        @Test
        @DisplayName("成功提交任务")
        void shouldSubmitTaskSuccessfully() {
            TaskSubmission submission = task.submit(
                ASSIGNEE_ID,
                ASSIGNEE_NAME,
                "已完成月度报表，请审核",
                Arrays.asList(1L, 2L)
            );

            assertEquals(TaskStatus.SUBMITTED, task.getStatus());
            assertNotNull(task.getSubmittedAt());
            assertNotNull(submission);
            assertEquals(ASSIGNEE_ID, submission.getSubmitterId());
            assertEquals(1, task.getSubmissions().size());
            assertFalse(task.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("PENDING状态不能提交")
        void shouldFailToSubmitPendingTask() {
            Task pendingTask = createValidTask();

            assertThrows(BusinessException.class, () ->
                pendingTask.submit(ASSIGNEE_ID, ASSIGNEE_NAME, "content", null)
            );
        }
    }

    @Nested
    @DisplayName("审批任务测试")
    class ApproveTaskTest {

        @BeforeEach
        void submitTask() {
            task.accept(ASSIGNEE_ID);
            task.submit(ASSIGNEE_ID, ASSIGNEE_NAME, "已完成", null);
            task.clearDomainEvents();
        }

        @Test
        @DisplayName("审批通过")
        void shouldApproveTaskSuccessfully() {
            task.approve(APPROVER_ID, APPROVER_NAME, "审批通过，完成质量良好");

            assertEquals(TaskStatus.COMPLETED, task.getStatus());
            assertNotNull(task.getCompletedAt());
            assertTrue(task.isCompleted());
            assertFalse(task.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("审批驳回")
        void shouldRejectTaskSuccessfully() {
            task.reject(APPROVER_ID, APPROVER_NAME, "数据有误，请重新核对");

            assertEquals(TaskStatus.REJECTED, task.getStatus());
            assertFalse(task.isCompleted());
            assertFalse(task.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("非SUBMITTED状态不能审批")
        void shouldFailToApproveNonSubmittedTask() {
            Task inProgressTask = createValidTask();
            inProgressTask.accept(ASSIGNEE_ID);

            assertThrows(BusinessException.class, () ->
                inProgressTask.approve(APPROVER_ID, APPROVER_NAME, "comment")
            );
        }
    }

    @Nested
    @DisplayName("重新提交测试")
    class ResubmitTaskTest {

        @BeforeEach
        void rejectTask() {
            task.accept(ASSIGNEE_ID);
            task.submit(ASSIGNEE_ID, ASSIGNEE_NAME, "第一次提交", null);
            task.reject(APPROVER_ID, APPROVER_NAME, "需要修改");
            task.clearDomainEvents();
        }

        @Test
        @DisplayName("被驳回后重新提交")
        void shouldResubmitRejectedTask() {
            TaskSubmission submission = task.resubmit(
                ASSIGNEE_ID,
                ASSIGNEE_NAME,
                "修改后重新提交",
                Arrays.asList(3L)
            );

            assertEquals(TaskStatus.SUBMITTED, task.getStatus());
            assertNotNull(submission);
            assertEquals(2, task.getSubmissions().size());
        }

        @Test
        @DisplayName("非REJECTED状态不能重新提交")
        void shouldFailToResubmitNonRejectedTask() {
            Task inProgressTask = createValidTask();
            inProgressTask.accept(ASSIGNEE_ID);

            assertThrows(BusinessException.class, () ->
                inProgressTask.resubmit(ASSIGNEE_ID, ASSIGNEE_NAME, "content", null)
            );
        }
    }

    @Nested
    @DisplayName("工作流信息测试")
    class WorkflowInfoTest {

        @Test
        @DisplayName("设置工作流信息")
        void shouldSetWorkflowInfo() {
            task.setWorkflowInfo(1L, "process-001", "审批节点", Arrays.asList(APPROVER_ID));

            assertEquals(1L, task.getWorkflowTemplateId());
            assertEquals("process-001", task.getProcessInstanceId());
            assertEquals("审批节点", task.getCurrentNode());
            assertEquals(1, task.getCurrentApprovers().size());
        }

        @Test
        @DisplayName("更新当前审批节点")
        void shouldUpdateCurrentNode() {
            task.setWorkflowInfo(1L, "process-001", "初审", Arrays.asList(100L));

            task.updateCurrentNode("复审", Arrays.asList(200L, 201L));

            assertEquals("复审", task.getCurrentNode());
            assertEquals(2, task.getCurrentApprovers().size());
        }
    }

    @Nested
    @DisplayName("业务状态判断测试")
    class BusinessStateTest {

        @Test
        @DisplayName("判断任务是否过期")
        void shouldCheckIfOverdue() {
            Task overdueTask = Task.create(
                TASK_CODE,
                TITLE,
                DESCRIPTION,
                TaskPriority.NORMAL,
                ASSIGNER_ID,
                ASSIGNER_NAME,
                DEPARTMENT_ID,
                LocalDateTime.now().minusDays(1), // 过期日期
                Arrays.asList(ASSIGNEE_ID),
                1
            );

            assertTrue(overdueTask.isOverdue());
        }

        @Test
        @DisplayName("已完成任务不算过期")
        void shouldNotBeOverdueWhenCompleted() {
            task.accept(ASSIGNEE_ID);
            task.submit(ASSIGNEE_ID, ASSIGNEE_NAME, "done", null);
            task.approve(APPROVER_ID, APPROVER_NAME, "ok");

            // 即使截止日期已过，已完成的任务不算过期
            assertFalse(task.isOverdue());
        }

        @Test
        @DisplayName("判断任务是否可编辑")
        void shouldCheckIfEditable() {
            assertTrue(task.isEditable()); // PENDING状态可编辑

            task.accept(ASSIGNEE_ID);
            assertFalse(task.isEditable()); // IN_PROGRESS状态不可编辑
        }

        @Test
        @DisplayName("判断任务是否已完成")
        void shouldCheckIfCompleted() {
            assertFalse(task.isCompleted());

            task.accept(ASSIGNEE_ID);
            task.submit(ASSIGNEE_ID, ASSIGNEE_NAME, "done", null);
            task.approve(APPROVER_ID, APPROVER_NAME, "ok");

            assertTrue(task.isCompleted());
        }
    }

    @Nested
    @DisplayName("完整工作流测试")
    class FullWorkflowTest {

        @Test
        @DisplayName("完整的任务审批流程")
        void shouldCompleteFullWorkflow() {
            // 初始状态
            assertEquals(TaskStatus.PENDING, task.getStatus());

            // 接受任务
            task.accept(ASSIGNEE_ID);
            assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
            assertNotNull(task.getAcceptedAt());

            // 提交任务
            task.submit(ASSIGNEE_ID, ASSIGNEE_NAME, "完成报告", Arrays.asList(1L));
            assertEquals(TaskStatus.SUBMITTED, task.getStatus());
            assertNotNull(task.getSubmittedAt());
            assertEquals(1, task.getSubmissions().size());

            // 审批通过
            task.approve(APPROVER_ID, APPROVER_NAME, "审批通过");
            assertEquals(TaskStatus.COMPLETED, task.getStatus());
            assertNotNull(task.getCompletedAt());
            assertTrue(task.isCompleted());
        }

        @Test
        @DisplayName("任务驳回后重新提交通过流程")
        void shouldCompleteResubmitWorkflow() {
            // 接受 -> 提交 -> 驳回
            task.accept(ASSIGNEE_ID);
            task.submit(ASSIGNEE_ID, ASSIGNEE_NAME, "第一次提交", null);
            task.reject(APPROVER_ID, APPROVER_NAME, "需要修改");
            assertEquals(TaskStatus.REJECTED, task.getStatus());

            // 重新提交 -> 通过
            task.resubmit(ASSIGNEE_ID, ASSIGNEE_NAME, "修改后提交", null);
            assertEquals(TaskStatus.SUBMITTED, task.getStatus());

            task.approve(APPROVER_ID, APPROVER_NAME, "修改后通过");
            assertEquals(TaskStatus.COMPLETED, task.getStatus());

            // 验证提交记录
            assertEquals(2, task.getSubmissions().size());
        }
    }

    @Nested
    @DisplayName("Reconstruct测试")
    class ReconstructTest {

        @Test
        @DisplayName("从持久化重建任务")
        void shouldReconstructTask() {
            Task reconstructed = Task.reconstruct(
                1L,
                TASK_CODE,
                TITLE,
                DESCRIPTION,
                TaskPriority.URGENT,
                ASSIGNER_ID,
                ASSIGNER_NAME,
                1,
                DEPARTMENT_ID,
                LocalDateTime.now().plusDays(7),
                TaskStatus.IN_PROGRESS,
                Arrays.asList(ASSIGNEE_ID),
                LocalDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                1,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
            );

            assertEquals(1L, reconstructed.getId());
            assertEquals(TASK_CODE, reconstructed.getTaskCode());
            assertEquals(TaskStatus.IN_PROGRESS, reconstructed.getStatus());
            assertEquals(TaskPriority.URGENT, reconstructed.getPriority());
            // 重建不应该生成领域事件
            assertTrue(reconstructed.getDomainEvents().isEmpty());
        }
    }
}
