package com.school.management.domain.inspection.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V108 isOverdue 路由 — 6 种 TaskType + 3 种 DeadlinePolicy 矩阵.
 */
@DisplayName("V108 InspTask.isOverdue() 多类型路由")
class InspTaskOverdueTest {

    private static InspTask scheduledTask(LocalDate dueDate) {
        InspTask t = InspTask.create("TSK-001", 1L, dueDate);
        return t;
    }

    private static InspTask adhocTask() {
        return InspTask.createAdHoc("TSK-002", 1L, 100L, "tester", "突击检查 e2e");
    }

    private static InspTask triggeredTask(LocalDate dueDate) {
        InspTask t = InspTask.createTriggered("TSK-003", 1L, "Appeal", 999L, "申诉触发");
        // 模拟手动设 deadline
        return InspTask.reconstruct(InspTask.builder()
                .id(t.getId())
                .taskCode(t.getTaskCode())
                .projectId(t.getProjectId())
                .taskDate(dueDate)
                .status(t.getStatus())
                .taskType(TaskType.TRIGGERED)
                .deadlinePolicy(DeadlinePolicy.RELAXED)
                .source(t.getSource()));
    }

    @Nested
    @DisplayName("AD_HOC (deadlinePolicy=NONE)")
    class AdHocTests {
        @Test
        @DisplayName("即使 task_date 是过去, 也永不逾期")
        void adhocNeverOverdue() {
            InspTask t = adhocTask();
            assertThat(t.getTaskType()).isEqualTo(TaskType.AD_HOC);
            assertThat(t.getDeadlinePolicy()).isEqualTo(DeadlinePolicy.NONE);
            assertThat(t.isOverdue()).isFalse();
        }
    }

    @Nested
    @DisplayName("SCHEDULED (deadlinePolicy=STRICT)")
    class ScheduledTests {
        @Test
        @DisplayName("task_date < today 且未审核 → 逾期")
        void scheduledOverdueWhenPastAndNotReviewed() {
            InspTask t = scheduledTask(LocalDate.now().minusDays(3));
            assertThat(t.getTaskType()).isEqualTo(TaskType.SCHEDULED);
            assertThat(t.getDeadlinePolicy()).isEqualTo(DeadlinePolicy.STRICT);
            assertThat(t.isOverdue()).isTrue();
        }

        @Test
        @DisplayName("task_date = today → 不算逾期")
        void scheduledTodayNotOverdue() {
            InspTask t = scheduledTask(LocalDate.now());
            assertThat(t.isOverdue()).isFalse();
        }

        @Test
        @DisplayName("task_date < today 但已审核 → 不算逾期")
        void scheduledReviewedNotOverdue() {
            InspTask t = scheduledTask(LocalDate.now().minusDays(3));
            // 模拟流转到 REVIEWED — 直接用 reconstruct
            InspTask reviewed = InspTask.reconstruct(InspTask.builder()
                    .id(1L)
                    .taskCode("TSK-001")
                    .projectId(1L)
                    .taskDate(LocalDate.now().minusDays(3))
                    .status(TaskStatus.REVIEWED)
                    .taskType(TaskType.SCHEDULED)
                    .deadlinePolicy(DeadlinePolicy.STRICT));
            assertThat(reviewed.isOverdue()).isFalse();
        }
    }

    @Nested
    @DisplayName("DeadlinePolicy.defaultFor() 推导")
    class DefaultPolicyTests {
        @Test
        void scheduledDefaultsToStrict() {
            assertThat(DeadlinePolicy.defaultFor(TaskType.SCHEDULED))
                    .isEqualTo(DeadlinePolicy.STRICT);
        }
        @Test
        void adhocDefaultsToNone() {
            assertThat(DeadlinePolicy.defaultFor(TaskType.AD_HOC))
                    .isEqualTo(DeadlinePolicy.NONE);
        }
        @Test
        void triggeredDefaultsToRelaxed() {
            assertThat(DeadlinePolicy.defaultFor(TaskType.TRIGGERED))
                    .isEqualTo(DeadlinePolicy.RELAXED);
        }
        @Test
        void selfCheckDefaultsToNone() {
            assertThat(DeadlinePolicy.defaultFor(TaskType.SELF_CHECK))
                    .isEqualTo(DeadlinePolicy.NONE);
        }
        @Test
        void crossAuditDefaultsToStrict() {
            assertThat(DeadlinePolicy.defaultFor(TaskType.CROSS_AUDIT))
                    .isEqualTo(DeadlinePolicy.STRICT);
        }
    }

    @Nested
    @DisplayName("KPI 路由 — 只 SCHEDULED 计入计划完成率")
    class KpiTests {
        @Test
        void scheduledCountsTowardKpi() {
            assertThat(TaskType.SCHEDULED.countsTowardScheduledKpi()).isTrue();
        }
        @Test
        void adhocDoesNotCountTowardKpi() {
            assertThat(TaskType.AD_HOC.countsTowardScheduledKpi()).isFalse();
        }
        @Test
        void selfCheckDoesNotCountTowardKpi() {
            assertThat(TaskType.SELF_CHECK.countsTowardScheduledKpi()).isFalse();
        }
    }

    @Nested
    @DisplayName("SELF_CHECK 自查")
    class SelfCheckTests {
        @Test
        @DisplayName("createSelfCheck 工厂 — type=SELF_CHECK + policy=NONE + status=CLAIMED + inspector=本人")
        void selfCheckFactoryBasic() {
            InspTask t = InspTask.createSelfCheck("TSK-SC", 5L, 999L, "alice", "月度自评");
            assertThat(t.getTaskType()).isEqualTo(TaskType.SELF_CHECK);
            assertThat(t.getDeadlinePolicy()).isEqualTo(DeadlinePolicy.NONE);
            assertThat(t.getStatus()).isEqualTo(TaskStatus.CLAIMED);
            assertThat(t.getInspectorId()).isEqualTo(999L);
            assertThat(t.isOverdue()).isFalse();
            assertThat(t.getSource().reason()).isEqualTo("月度自评");
            assertThat(t.getSource().isManual()).isTrue();
        }
    }

    @Nested
    @DisplayName("CROSS_AUDIT 互查")
    class CrossAuditTests {
        @Test
        @DisplayName("createCrossAudit 工厂 — type=CROSS_AUDIT + policy=STRICT + dueDate 必填")
        void crossAuditFactoryBasic() {
            InspTask t = InspTask.createCrossAudit("TSK-CA", 5L, 100L, "auditor",
                    LocalDate.now().plusDays(7), "互查");
            assertThat(t.getTaskType()).isEqualTo(TaskType.CROSS_AUDIT);
            assertThat(t.getDeadlinePolicy()).isEqualTo(DeadlinePolicy.STRICT);
            assertThat(t.getStatus()).isEqualTo(TaskStatus.CLAIMED);
            assertThat(t.getTaskDate()).isEqualTo(LocalDate.now().plusDays(7));
            assertThat(t.isOverdue()).isFalse();  // 7 天后才到期
        }

        @Test
        @DisplayName("CROSS_AUDIT 过 due → 逾期生效")
        void crossAuditOverdueWhenPastDue() {
            InspTask t = InspTask.createCrossAudit("TSK-CA-2", 5L, 100L, "auditor",
                    LocalDate.now().minusDays(2), "互查超期");
            assertThat(t.isOverdue()).isTrue();
        }
    }

    @Nested
    @DisplayName("submit() lateSubmission 路由")
    class SubmitLateRouting {
        @Test
        @DisplayName("AD_HOC submit() 永不标 late")
        void adhocSubmitNeverLate() {
            InspTask t = adhocTask();
            t.start();
            t.submit();
            assertThat(t.getLateSubmission()).isFalse();
            assertThat(t.getLateDays()).isZero();
        }

        @Test
        @DisplayName("SCHEDULED submit() 当 due 已过 → late=true")
        void scheduledSubmitLateWhenPastDue() {
            // 用 reconstruct 模拟 task_date=2 天前, 状态 IN_PROGRESS (可 submit)
            InspTask t = InspTask.reconstruct(InspTask.builder()
                    .id(99L)
                    .taskCode("TSK-LATE")
                    .projectId(1L)
                    .taskDate(LocalDate.now().minusDays(2))
                    .status(TaskStatus.IN_PROGRESS)
                    .inspectorId(100L)
                    .taskType(TaskType.SCHEDULED)
                    .deadlinePolicy(DeadlinePolicy.STRICT));
            t.submit();
            assertThat(t.getLateSubmission()).isTrue();
            assertThat(t.getLateDays()).isGreaterThanOrEqualTo(2);
        }
    }
}
