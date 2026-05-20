package com.school.management.application.inspection;

import com.school.management.domain.inspection.event.CaseClosedEvent;
import com.school.management.domain.inspection.event.CorrectiveCaseCreatedEvent;
import com.school.management.domain.inspection.event.EffectivenessFailedEvent;
import com.school.management.domain.inspection.event.SubmissionCompletedEvent;
import com.school.management.domain.inspection.event.TaskCancelledEvent;
import com.school.management.domain.inspection.event.TaskPublishedEvent;
import com.school.management.domain.inspection.model.analytics.CorrectiveSummary;
import com.school.management.domain.inspection.model.analytics.DailySummary;
import com.school.management.domain.inspection.model.analytics.InspectorSummary;
import com.school.management.domain.inspection.model.analytics.PeriodSummary;
import com.school.management.domain.inspection.model.analytics.PeriodType;
import com.school.management.domain.inspection.model.analytics.TrendDirection;
import com.school.management.domain.inspection.model.corrective.CaseStatus;
import com.school.management.domain.inspection.model.corrective.CorrectiveCase;
import com.school.management.domain.inspection.model.execution.InspSubmission;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.domain.inspection.model.execution.SubmissionStatus;
import com.school.management.domain.inspection.model.execution.TargetType;
import com.school.management.domain.inspection.model.execution.TaskStatus;
import com.school.management.domain.inspection.repository.CorrectiveCaseRepository;
import com.school.management.domain.inspection.repository.CorrectiveSummaryRepository;
import com.school.management.domain.inspection.repository.DailySummaryRepository;
import com.school.management.domain.inspection.repository.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.InspTaskRepository;
import com.school.management.domain.inspection.repository.InspectorSummaryRepository;
import com.school.management.domain.inspection.repository.ItemFrequencySummaryRepository;
import com.school.management.domain.inspection.repository.PeriodSummaryRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AnalyticsProjectionService 分析投影服务 (CQRS 写侧) 单测.
 *
 * 用 Mockito 隔离 8 个依赖, 覆盖:
 *  - rebuildDailySummary: 聚合 avg/min/max/扣分/加分/通过失败计数 + 排名
 *  - rebuildPeriodSummary: 从日汇总聚合 + 标准差 + 趋势 (UP/DOWN/STABLE) + 趋势百分比
 *  - updateInspectorSummary: 任务/提交计数 + 合规率
 *  - rebuildCorrectiveSummary: 整改案例计数
 *  - 6 个事件监听器的路由逻辑 + 空值/缺失短路
 *
 * 注: 监听器内部 catch 了所有异常, 因此异常路径以"不抛出"为断言.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsProjectionService 分析投影服务")
class AnalyticsProjectionServiceTest {

    @Mock InspTaskRepository taskRepository;
    @Mock InspSubmissionRepository submissionRepository;
    @Mock DailySummaryRepository dailySummaryRepository;
    @Mock PeriodSummaryRepository periodSummaryRepository;
    @Mock CorrectiveCaseRepository correctiveCaseRepository;
    @Mock InspectorSummaryRepository inspectorSummaryRepository;
    @Mock ItemFrequencySummaryRepository itemFrequencySummaryRepository;
    @Mock CorrectiveSummaryRepository correctiveSummaryRepository;
    @Mock SpringDomainEventPublisher eventPublisher;

    @InjectMocks AnalyticsProjectionService service;

    private static final LocalDate D1 = LocalDate.of(2026, 5, 1);

    // ── builders ───────────────────────────────────────────────

    private InspTask publishedTask(Long id, Long projectId) {
        return InspTask.builder()
                .id(id).taskCode("TK-" + id).projectId(projectId).taskDate(D1)
                .status(TaskStatus.PUBLISHED).build();
    }

    private InspTask taskInStatus(Long id, Long projectId, TaskStatus status, Long inspectorId) {
        return InspTask.builder()
                .id(id).taskCode("TK-" + id).projectId(projectId).taskDate(D1)
                .status(status).inspectorId(inspectorId).inspectorName("检查员" + inspectorId)
                .build();
    }

    private InspSubmission completedSub(Long taskId, Long targetId, BigDecimal finalScore,
                                        BigDecimal deduction, BigDecimal bonus, Boolean passed) {
        return InspSubmission.builder()
                .id(System.nanoTime())
                .taskId(taskId).targetType(TargetType.ORG).targetId(targetId)
                .targetName("T" + targetId).orgUnitId(targetId).orgUnitName("ORG" + targetId)
                .status(SubmissionStatus.COMPLETED)
                .finalScore(finalScore).deductionTotal(deduction).bonusTotal(bonus)
                .passed(passed).grade("A").build();
    }

    private DailySummary daily(Long projectId, String targetType, Long targetId, BigDecimal avg) {
        DailySummary d = new DailySummary();
        d.setProjectId(projectId);
        d.setTargetType(targetType);
        d.setTargetId(targetId);
        d.setTargetName("T" + targetId);
        d.setAvgScore(avg);
        return d;
    }

    private CorrectiveCase caseInStatus(Long id, Long projectId, CaseStatus status) {
        return CorrectiveCase.builder()
                .id(id).caseCode("CC-" + id).projectId(projectId)
                .issueDescription("问题").status(status).build();
    }

    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("rebuildDailySummary — 日汇总重建")
    class RebuildDailySummaryTests {

        @Test
        @DisplayName("无已发布任务: 删除旧汇总但不保存新汇总")
        void shouldClearWhenNoPublishedTasks() {
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1)).thenReturn(List.of());

            service.rebuildDailySummary(1L, D1);

            verify(dailySummaryRepository).deleteByProjectAndDate(1L, D1);
            verify(dailySummaryRepository, never()).save(any());
        }

        @Test
        @DisplayName("非 PUBLISHED 的任务被过滤掉")
        void shouldIgnoreNonPublishedTasks() {
            InspTask draft = taskInStatus(50L, 1L, TaskStatus.IN_PROGRESS, 99L);
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1)).thenReturn(List.of(draft));

            service.rebuildDailySummary(1L, D1);

            verify(submissionRepository, never()).findByTaskId(anyLong());
            verify(dailySummaryRepository, never()).save(any());
        }

        @Test
        @DisplayName("单目标多提交: avg/min/max 计算正确, count=2")
        void shouldComputeAvgMinMax() {
            InspTask t = publishedTask(50L, 1L);
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1)).thenReturn(List.of(t));
            when(submissionRepository.findByTaskId(50L)).thenReturn(List.of(
                    completedSub(50L, 7L, new BigDecimal("80"), new BigDecimal("5"),
                            new BigDecimal("2"), true),
                    completedSub(50L, 7L, new BigDecimal("90"), new BigDecimal("3"),
                            new BigDecimal("1"), false)
            ));
            when(dailySummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.rebuildDailySummary(1L, D1);

            ArgumentCaptor<DailySummary> cap = ArgumentCaptor.forClass(DailySummary.class);
            verify(dailySummaryRepository).save(cap.capture());
            DailySummary s = cap.getValue();
            assertThat(s.getInspectionCount()).isEqualTo(2);
            assertThat(s.getAvgScore()).isEqualByComparingTo("85.00");
            assertThat(s.getMinScore()).isEqualByComparingTo("80");
            assertThat(s.getMaxScore()).isEqualByComparingTo("90");
            assertThat(s.getTotalDeductions()).isEqualByComparingTo("8");
            assertThat(s.getTotalBonuses()).isEqualByComparingTo("3");
            assertThat(s.getPassCount()).isEqualTo(1);
            assertThat(s.getFailCount()).isEqualTo(1);
            assertThat(s.getRanking()).isEqualTo(1);
        }

        @Test
        @DisplayName("两个目标按 avgScore 降序排名: 高分 ranking=1")
        void shouldRankByAvgScoreDescending() {
            InspTask t = publishedTask(50L, 1L);
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1)).thenReturn(List.of(t));
            when(submissionRepository.findByTaskId(50L)).thenReturn(List.of(
                    completedSub(50L, 7L, new BigDecimal("60"), BigDecimal.ZERO,
                            BigDecimal.ZERO, true),
                    completedSub(50L, 8L, new BigDecimal("95"), BigDecimal.ZERO,
                            BigDecimal.ZERO, true)
            ));
            when(dailySummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.rebuildDailySummary(1L, D1);

            ArgumentCaptor<DailySummary> cap = ArgumentCaptor.forClass(DailySummary.class);
            verify(dailySummaryRepository, times(2)).save(cap.capture());
            DailySummary rank1 = cap.getAllValues().stream()
                    .filter(d -> d.getRanking() == 1).findFirst().orElseThrow();
            DailySummary rank2 = cap.getAllValues().stream()
                    .filter(d -> d.getRanking() == 2).findFirst().orElseThrow();
            assertThat(rank1.getTargetId()).isEqualTo(8L);
            assertThat(rank1.getAvgScore()).isEqualByComparingTo("95");
            assertThat(rank2.getTargetId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("每个保存的目标都发布 DailySummaryUpdatedEvent")
        void shouldPublishUpdatedEventPerTarget() {
            InspTask t = publishedTask(50L, 1L);
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1)).thenReturn(List.of(t));
            when(submissionRepository.findByTaskId(50L)).thenReturn(List.of(
                    completedSub(50L, 7L, new BigDecimal("80"), BigDecimal.ZERO,
                            BigDecimal.ZERO, true)
            ));
            when(dailySummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.rebuildDailySummary(1L, D1);

            verify(eventPublisher).publish(any());
        }

        @Test
        @DisplayName("提交 finalScore 全为 null: avg/min/max 保持 null")
        void shouldLeaveScoresNullWhenNoFinalScore() {
            InspTask t = publishedTask(50L, 1L);
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1)).thenReturn(List.of(t));
            when(submissionRepository.findByTaskId(50L)).thenReturn(List.of(
                    completedSub(50L, 7L, null, BigDecimal.ZERO, BigDecimal.ZERO, true)
            ));
            when(dailySummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.rebuildDailySummary(1L, D1);

            ArgumentCaptor<DailySummary> cap = ArgumentCaptor.forClass(DailySummary.class);
            verify(dailySummaryRepository).save(cap.capture());
            assertThat(cap.getValue().getAvgScore()).isNull();
            assertThat(cap.getValue().getInspectionCount()).isEqualTo(1);
        }
    }

    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("rebuildPeriodSummary — 周期汇总重建")
    class RebuildPeriodSummaryTests {

        @Test
        @DisplayName("WEEKLY periodEnd = start + 6 天")
        void shouldComputeWeeklyPeriodEnd() {
            when(dailySummaryRepository.findByProjectAndDateRange(eq(1L), eq(D1), any()))
                    .thenReturn(List.of());

            service.rebuildPeriodSummary(1L, PeriodType.WEEKLY, D1);

            verify(dailySummaryRepository).findByProjectAndDateRange(1L, D1, D1.plusDays(6));
            verify(periodSummaryRepository).deleteByProjectAndPeriod(
                    1L, PeriodType.WEEKLY, D1);
        }

        @Test
        @DisplayName("无日汇总: 不保存周期汇总, 但仍发布 PeriodSummaryCalculatedEvent")
        void shouldPublishEventEvenWhenEmpty() {
            when(dailySummaryRepository.findByProjectAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());

            service.rebuildPeriodSummary(1L, PeriodType.WEEKLY, D1);

            verify(periodSummaryRepository, never()).save(any());
            verify(eventPublisher).publish(any());
        }

        @Test
        @DisplayName("聚合日汇总 avg: (70,80,90) -> 80.00, min=70, max=90")
        void shouldAggregateDailyAvgScores() {
            when(dailySummaryRepository.findByProjectAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(
                            daily(1L, "ORG", 7L, new BigDecimal("70")),
                            daily(1L, "ORG", 7L, new BigDecimal("80")),
                            daily(1L, "ORG", 7L, new BigDecimal("90"))
                    ));
            when(correctiveCaseRepository.findByProjectId(1L)).thenReturn(List.of());
            when(periodSummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.rebuildPeriodSummary(1L, PeriodType.WEEKLY, D1);

            ArgumentCaptor<PeriodSummary> cap = ArgumentCaptor.forClass(PeriodSummary.class);
            verify(periodSummaryRepository).save(cap.capture());
            PeriodSummary ps = cap.getValue();
            assertThat(ps.getAvgScore()).isEqualByComparingTo("80.00");
            assertThat(ps.getMinScore()).isEqualByComparingTo("70");
            assertThat(ps.getMaxScore()).isEqualByComparingTo("90");
            assertThat(ps.getInspectionDays()).isEqualTo(3);
        }

        @Test
        @DisplayName("趋势 UP: 后半段均值高于前半段")
        void shouldDetectUpwardTrend() {
            // 4 days: first half (60,60)=60, second half (90,90)=90 -> UP
            when(dailySummaryRepository.findByProjectAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(
                            daily(1L, "ORG", 7L, new BigDecimal("60")),
                            daily(1L, "ORG", 7L, new BigDecimal("60")),
                            daily(1L, "ORG", 7L, new BigDecimal("90")),
                            daily(1L, "ORG", 7L, new BigDecimal("90"))
                    ));
            when(correctiveCaseRepository.findByProjectId(1L)).thenReturn(List.of());
            when(periodSummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.rebuildPeriodSummary(1L, PeriodType.WEEKLY, D1);

            ArgumentCaptor<PeriodSummary> cap = ArgumentCaptor.forClass(PeriodSummary.class);
            verify(periodSummaryRepository).save(cap.capture());
            PeriodSummary ps = cap.getValue();
            assertThat(ps.getTrendDirection()).isEqualTo(TrendDirection.UP);
            // trendPercent = (90-60)/60*100 = 50
            assertThat(ps.getTrendPercent()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("趋势 DOWN: 后半段均值低于前半段")
        void shouldDetectDownwardTrend() {
            when(dailySummaryRepository.findByProjectAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(
                            daily(1L, "ORG", 7L, new BigDecimal("90")),
                            daily(1L, "ORG", 7L, new BigDecimal("90")),
                            daily(1L, "ORG", 7L, new BigDecimal("60")),
                            daily(1L, "ORG", 7L, new BigDecimal("60"))
                    ));
            when(correctiveCaseRepository.findByProjectId(1L)).thenReturn(List.of());
            when(periodSummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.rebuildPeriodSummary(1L, PeriodType.WEEKLY, D1);

            ArgumentCaptor<PeriodSummary> cap = ArgumentCaptor.forClass(PeriodSummary.class);
            verify(periodSummaryRepository).save(cap.capture());
            assertThat(cap.getValue().getTrendDirection()).isEqualTo(TrendDirection.DOWN);
        }

        @Test
        @DisplayName("趋势 STABLE: 前后半段均值相等")
        void shouldDetectStableTrend() {
            when(dailySummaryRepository.findByProjectAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(
                            daily(1L, "ORG", 7L, new BigDecimal("80")),
                            daily(1L, "ORG", 7L, new BigDecimal("80"))
                    ));
            when(correctiveCaseRepository.findByProjectId(1L)).thenReturn(List.of());
            when(periodSummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.rebuildPeriodSummary(1L, PeriodType.WEEKLY, D1);

            ArgumentCaptor<PeriodSummary> cap = ArgumentCaptor.forClass(PeriodSummary.class);
            verify(periodSummaryRepository).save(cap.capture());
            assertThat(cap.getValue().getTrendDirection()).isEqualTo(TrendDirection.STABLE);
        }

        @Test
        @DisplayName("单日数据: 无趋势 (avgScores.size < 2), 无标准差")
        void shouldHaveNoTrendForSingleDay() {
            when(dailySummaryRepository.findByProjectAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(daily(1L, "ORG", 7L, new BigDecimal("80"))));
            when(correctiveCaseRepository.findByProjectId(1L)).thenReturn(List.of());
            when(periodSummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.rebuildPeriodSummary(1L, PeriodType.WEEKLY, D1);

            ArgumentCaptor<PeriodSummary> cap = ArgumentCaptor.forClass(PeriodSummary.class);
            verify(periodSummaryRepository).save(cap.capture());
            assertThat(cap.getValue().getTrendDirection()).isNull();
            assertThat(cap.getValue().getScoreStdDev()).isNull();
        }

        @Test
        @DisplayName("标准差: (70,90) 均值80, 方差100, stdDev=10.0000")
        void shouldComputeStandardDeviation() {
            when(dailySummaryRepository.findByProjectAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(
                            daily(1L, "ORG", 7L, new BigDecimal("70")),
                            daily(1L, "ORG", 7L, new BigDecimal("90"))
                    ));
            when(correctiveCaseRepository.findByProjectId(1L)).thenReturn(List.of());
            when(periodSummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.rebuildPeriodSummary(1L, PeriodType.WEEKLY, D1);

            ArgumentCaptor<PeriodSummary> cap = ArgumentCaptor.forClass(PeriodSummary.class);
            verify(periodSummaryRepository).save(cap.capture());
            assertThat(cap.getValue().getScoreStdDev()).isEqualByComparingTo("10.0000");
        }

        @Test
        @DisplayName("整改案例计数: total=2, closed=1 (CLOSED 或 VERIFIED)")
        void shouldCountCorrectiveCases() {
            when(dailySummaryRepository.findByProjectAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(daily(1L, "ORG", 7L, new BigDecimal("80"))));
            when(correctiveCaseRepository.findByProjectId(1L)).thenReturn(List.of(
                    caseInStatus(1L, 1L, CaseStatus.OPEN),
                    caseInStatus(2L, 1L, CaseStatus.VERIFIED)
            ));
            when(periodSummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.rebuildPeriodSummary(1L, PeriodType.WEEKLY, D1);

            ArgumentCaptor<PeriodSummary> cap = ArgumentCaptor.forClass(PeriodSummary.class);
            verify(periodSummaryRepository).save(cap.capture());
            assertThat(cap.getValue().getCorrectiveCount()).isEqualTo(2);
            assertThat(cap.getValue().getCorrectiveClosedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("MONTHLY periodEnd = 当月最后一天")
        void shouldComputeMonthlyPeriodEnd() {
            when(dailySummaryRepository.findByProjectAndDateRange(any(), any(), any()))
                    .thenReturn(List.of());

            service.rebuildPeriodSummary(1L, PeriodType.MONTHLY, D1);

            verify(dailySummaryRepository).findByProjectAndDateRange(
                    1L, D1, LocalDate.of(2026, 5, 31));
        }
    }

    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("updateInspectorSummary — 检查员绩效")
    class UpdateInspectorSummaryTests {

        @Test
        @DisplayName("无检查员任务: 全部计数为 0, 仍保存汇总")
        void shouldSaveZeroCountsWhenNoTasks() {
            when(inspectorSummaryRepository.findByInspectorAndPeriod(
                    eq(1L), eq(99L), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.empty());
            when(taskRepository.findByProjectIdAndTaskDate(eq(1L), any()))
                    .thenReturn(List.of());

            service.updateInspectorSummary(1L, 99L, "张三");

            ArgumentCaptor<InspectorSummary> cap = ArgumentCaptor.forClass(InspectorSummary.class);
            verify(inspectorSummaryRepository).save(cap.capture());
            InspectorSummary s = cap.getValue();
            assertThat(s.getTotalTasks()).isZero();
            assertThat(s.getCompletedTasks()).isZero();
            assertThat(s.getInspectorName()).isEqualTo("张三");
            assertThat(s.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("任务计数: 区分 completed/cancelled/expired, 别的检查员任务被排除")
        void shouldCountTaskStatuses() {
            when(inspectorSummaryRepository.findByInspectorAndPeriod(
                    eq(1L), eq(99L), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.empty());
            when(taskRepository.findByProjectIdAndTaskDate(eq(1L), any())).thenReturn(List.of(
                    taskInStatus(50L, 1L, TaskStatus.REVIEWED, 99L),
                    taskInStatus(51L, 1L, TaskStatus.CANCELLED, 99L),
                    taskInStatus(52L, 1L, TaskStatus.EXPIRED, 99L),
                    taskInStatus(53L, 1L, TaskStatus.REVIEWED, 88L) // 别的检查员
            ));
            when(submissionRepository.findByTaskId(anyLong())).thenReturn(List.of());

            service.updateInspectorSummary(1L, 99L, "张三");

            ArgumentCaptor<InspectorSummary> cap = ArgumentCaptor.forClass(InspectorSummary.class);
            verify(inspectorSummaryRepository).save(cap.capture());
            InspectorSummary s = cap.getValue();
            assertThat(s.getTotalTasks()).isEqualTo(3);
            assertThat(s.getCompletedTasks()).isEqualTo(1);
            assertThat(s.getCancelledTasks()).isEqualTo(1);
            assertThat(s.getExpiredTasks()).isEqualTo(1);
        }

        @Test
        @DisplayName("提交计数 + 合规率: 4 提交 1 不合格 -> 75.0000%")
        void shouldComputeComplianceRate() {
            when(inspectorSummaryRepository.findByInspectorAndPeriod(
                    eq(1L), eq(99L), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.empty());
            InspTask t = taskInStatus(50L, 1L, TaskStatus.REVIEWED, 99L);
            when(taskRepository.findByProjectIdAndTaskDate(eq(1L), any()))
                    .thenReturn(List.of(t));
            when(submissionRepository.findByTaskId(50L)).thenReturn(List.of(
                    completedSub(50L, 7L, new BigDecimal("80"), BigDecimal.ZERO,
                            BigDecimal.ZERO, true),
                    completedSub(50L, 7L, new BigDecimal("90"), BigDecimal.ZERO,
                            BigDecimal.ZERO, true),
                    completedSub(50L, 7L, new BigDecimal("70"), BigDecimal.ZERO,
                            BigDecimal.ZERO, true),
                    completedSub(50L, 7L, new BigDecimal("60"), BigDecimal.ZERO,
                            BigDecimal.ZERO, false) // 不合格
            ));

            service.updateInspectorSummary(1L, 99L, "张三");

            ArgumentCaptor<InspectorSummary> cap = ArgumentCaptor.forClass(InspectorSummary.class);
            verify(inspectorSummaryRepository).save(cap.capture());
            InspectorSummary s = cap.getValue();
            assertThat(s.getTotalSubmissions()).isEqualTo(4);
            assertThat(s.getFlaggedSubmissions()).isEqualTo(1);
            // avg = (80+90+70+60)/4 = 75.00
            assertThat(s.getAvgScore()).isEqualByComparingTo("75.00");
            // compliance = (4-1)/4*100 = 75.0000
            assertThat(s.getComplianceRate()).isEqualByComparingTo("75.0000");
        }

        @Test
        @DisplayName("已存在的汇总被复用 (保留 createdAt)")
        void shouldReuseExistingSummary() {
            InspectorSummary existing = new InspectorSummary();
            existing.setId(500L);
            existing.setProjectId(1L);
            existing.setInspectorId(99L);
            existing.setCreatedAt(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
            when(inspectorSummaryRepository.findByInspectorAndPeriod(
                    eq(1L), eq(99L), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.of(existing));
            when(taskRepository.findByProjectIdAndTaskDate(eq(1L), any()))
                    .thenReturn(List.of());

            service.updateInspectorSummary(1L, 99L, "张三");

            ArgumentCaptor<InspectorSummary> cap = ArgumentCaptor.forClass(InspectorSummary.class);
            verify(inspectorSummaryRepository).save(cap.capture());
            assertThat(cap.getValue().getId()).isEqualTo(500L);
            assertThat(cap.getValue().getCreatedAt())
                    .isEqualTo(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
        }
    }

    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("rebuildCorrectiveSummary — 整改汇总")
    class RebuildCorrectiveSummaryTests {

        @Test
        @DisplayName("案例计数: total/open/closed 区分")
        void shouldCountCaseStatuses() {
            when(correctiveCaseRepository.findByProjectId(1L)).thenReturn(List.of(
                    caseInStatus(1L, 1L, CaseStatus.OPEN),
                    caseInStatus(2L, 1L, CaseStatus.OPEN),
                    caseInStatus(3L, 1L, CaseStatus.CLOSED),
                    caseInStatus(4L, 1L, CaseStatus.VERIFIED)
            ));
            when(correctiveSummaryRepository.findByProjectAndPeriod(
                    eq(1L), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.empty());

            service.rebuildCorrectiveSummary(1L);

            ArgumentCaptor<CorrectiveSummary> cap = ArgumentCaptor.forClass(CorrectiveSummary.class);
            verify(correctiveSummaryRepository).save(cap.capture());
            CorrectiveSummary s = cap.getValue();
            assertThat(s.getTotalCases()).isEqualTo(4);
            assertThat(s.getOpenCases()).isEqualTo(2);
            assertThat(s.getClosedCases()).isEqualTo(2);
            assertThat(s.getAvgResolutionDays()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("无案例: 全部计数为 0, 仍保存")
        void shouldSaveZeroWhenNoCases() {
            when(correctiveCaseRepository.findByProjectId(1L)).thenReturn(List.of());
            when(correctiveSummaryRepository.findByProjectAndPeriod(
                    eq(1L), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.empty());

            service.rebuildCorrectiveSummary(1L);

            ArgumentCaptor<CorrectiveSummary> cap = ArgumentCaptor.forClass(CorrectiveSummary.class);
            verify(correctiveSummaryRepository).save(cap.capture());
            assertThat(cap.getValue().getTotalCases()).isZero();
            assertThat(cap.getValue().getCreatedAt()).isNotNull();
        }
    }

    // ════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("事件监听器")
    class EventListenerTests {

        @Test
        @DisplayName("onTaskPublished: 任务存在则重建日汇总")
        void shouldRebuildOnTaskPublished() {
            when(taskRepository.findById(50L))
                    .thenReturn(Optional.of(publishedTask(50L, 1L)));
            when(taskRepository.findByProjectIdAndTaskDate(1L, D1)).thenReturn(List.of());

            service.onTaskPublished(new TaskPublishedEvent(50L, "TK-50", 1L));

            verify(dailySummaryRepository).deleteByProjectAndDate(1L, D1);
        }

        @Test
        @DisplayName("onTaskPublished: 任务不存在则短路, 不重建")
        void shouldShortCircuitOnTaskPublishedWhenTaskMissing() {
            when(taskRepository.findById(50L)).thenReturn(Optional.empty());

            service.onTaskPublished(new TaskPublishedEvent(50L, "TK-50", 1L));

            verify(dailySummaryRepository, never()).deleteByProjectAndDate(anyLong(), any());
        }

        @Test
        @DisplayName("onSubmissionCompleted: 重建日汇总 + 更新检查员汇总")
        void shouldRebuildAndUpdateInspectorOnSubmissionCompleted() {
            InspSubmission sub = completedSub(50L, 7L, new BigDecimal("80"),
                    BigDecimal.ZERO, BigDecimal.ZERO, true);
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(sub));
            InspTask t = taskInStatus(50L, 1L, TaskStatus.PUBLISHED, 99L);
            when(taskRepository.findById(50L)).thenReturn(Optional.of(t));
            when(taskRepository.findByProjectIdAndTaskDate(eq(1L), any()))
                    .thenReturn(List.of());
            when(inspectorSummaryRepository.findByInspectorAndPeriod(
                    eq(1L), eq(99L), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.empty());

            service.onSubmissionCompleted(new SubmissionCompletedEvent(
                    1L, 50L, "ORG", 7L, new BigDecimal("80")));

            verify(dailySummaryRepository).deleteByProjectAndDate(1L, D1);
            verify(inspectorSummaryRepository).save(any());
        }

        @Test
        @DisplayName("onSubmissionCompleted: 提交不存在则短路")
        void shouldShortCircuitWhenSubmissionMissing() {
            when(submissionRepository.findById(1L)).thenReturn(Optional.empty());

            service.onSubmissionCompleted(new SubmissionCompletedEvent(
                    1L, 50L, "ORG", 7L, null));

            verify(taskRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("onSubmissionCompleted: 任务不存在则短路")
        void shouldShortCircuitWhenTaskMissing() {
            InspSubmission sub = completedSub(50L, 7L, new BigDecimal("80"),
                    BigDecimal.ZERO, BigDecimal.ZERO, true);
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(sub));
            when(taskRepository.findById(50L)).thenReturn(Optional.empty());

            service.onSubmissionCompleted(new SubmissionCompletedEvent(
                    1L, 50L, "ORG", 7L, null));

            verify(dailySummaryRepository, never()).deleteByProjectAndDate(anyLong(), any());
        }

        @Test
        @DisplayName("onCorrectiveCaseCreated: 案例存在则重建整改汇总")
        void shouldRebuildOnCorrectiveCaseCreated() {
            when(correctiveCaseRepository.findById(7L))
                    .thenReturn(Optional.of(caseInStatus(7L, 1L, CaseStatus.OPEN)));
            when(correctiveCaseRepository.findByProjectId(1L)).thenReturn(List.of());
            when(correctiveSummaryRepository.findByProjectAndPeriod(
                    eq(1L), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.empty());

            service.onCorrectiveCaseCreated(new CorrectiveCaseCreatedEvent(
                    7L, "CC-7", 1L, 1L, "HIGH"));

            verify(correctiveSummaryRepository).save(any());
        }

        @Test
        @DisplayName("onCorrectiveCaseCreated: caseId 为空则短路")
        void shouldShortCircuitWhenCaseIdNull() {
            service.onCorrectiveCaseCreated(new CorrectiveCaseCreatedEvent(
                    null, "CC-X", 1L, 1L, "HIGH"));

            verify(correctiveCaseRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("onCaseClosed: 案例存在则重建整改汇总")
        void shouldRebuildOnCaseClosed() {
            when(correctiveCaseRepository.findById(7L))
                    .thenReturn(Optional.of(caseInStatus(7L, 1L, CaseStatus.CLOSED)));
            when(correctiveCaseRepository.findByProjectId(1L)).thenReturn(List.of());
            when(correctiveSummaryRepository.findByProjectAndPeriod(
                    eq(1L), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.empty());

            service.onCaseClosed(new CaseClosedEvent(7L, "CC-7", D1));

            verify(correctiveSummaryRepository).save(any());
        }

        @Test
        @DisplayName("onCaseClosed: 案例不存在则短路")
        void shouldShortCircuitOnCaseClosedWhenMissing() {
            when(correctiveCaseRepository.findById(7L)).thenReturn(Optional.empty());

            service.onCaseClosed(new CaseClosedEvent(7L, "CC-7", D1));

            verify(correctiveSummaryRepository, never()).save(any());
        }

        @Test
        @DisplayName("onEffectivenessFailed: 案例存在则重建整改汇总")
        void shouldRebuildOnEffectivenessFailed() {
            when(correctiveCaseRepository.findById(7L))
                    .thenReturn(Optional.of(caseInStatus(7L, 1L, CaseStatus.ESCALATED)));
            when(correctiveCaseRepository.findByProjectId(1L)).thenReturn(List.of());
            when(correctiveSummaryRepository.findByProjectAndPeriod(
                    eq(1L), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.empty());

            service.onEffectivenessFailed(new EffectivenessFailedEvent(7L, "CC-7", 2));

            verify(correctiveSummaryRepository).save(any());
        }

        @Test
        @DisplayName("onTaskCancelled: 有检查员则更新检查员汇总")
        void shouldUpdateInspectorOnTaskCancelled() {
            InspTask t = taskInStatus(50L, 1L, TaskStatus.CANCELLED, 99L);
            when(taskRepository.findById(50L)).thenReturn(Optional.of(t));
            when(taskRepository.findByProjectIdAndTaskDate(eq(1L), any()))
                    .thenReturn(List.of());
            when(inspectorSummaryRepository.findByInspectorAndPeriod(
                    eq(1L), eq(99L), eq(PeriodType.WEEKLY), any()))
                    .thenReturn(Optional.empty());

            service.onTaskCancelled(new TaskCancelledEvent(50L, "TK-50"));

            verify(inspectorSummaryRepository).save(any());
        }

        @Test
        @DisplayName("onTaskCancelled: 任务无检查员则不更新")
        void shouldNotUpdateWhenTaskHasNoInspector() {
            InspTask t = taskInStatus(50L, 1L, TaskStatus.CANCELLED, null);
            when(taskRepository.findById(50L)).thenReturn(Optional.of(t));

            service.onTaskCancelled(new TaskCancelledEvent(50L, "TK-50"));

            verify(inspectorSummaryRepository, never()).save(any());
        }

        @Test
        @DisplayName("onTaskCancelled: 任务不存在则短路")
        void shouldShortCircuitOnTaskCancelledWhenMissing() {
            when(taskRepository.findById(50L)).thenReturn(Optional.empty());

            service.onTaskCancelled(new TaskCancelledEvent(50L, "TK-50"));

            verify(inspectorSummaryRepository, never()).save(any());
        }
    }
}
