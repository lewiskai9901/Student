package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.analytics.CorrectiveSummary;
import com.school.management.domain.inspection.model.analytics.DailySummary;
import com.school.management.domain.inspection.model.analytics.InspectorSummary;
import com.school.management.domain.inspection.model.analytics.ItemFrequencySummary;
import com.school.management.domain.inspection.model.analytics.PeriodSummary;
import com.school.management.domain.inspection.model.analytics.PeriodType;
import com.school.management.domain.inspection.model.corrective.CasePriority;
import com.school.management.domain.inspection.model.corrective.CaseStatus;
import com.school.management.domain.inspection.model.corrective.CorrectiveCase;
import com.school.management.domain.inspection.repository.CorrectiveCaseRepository;
import com.school.management.domain.inspection.repository.CorrectiveSummaryRepository;
import com.school.management.domain.inspection.repository.DailySummaryRepository;
import com.school.management.domain.inspection.repository.InspectorSummaryRepository;
import com.school.management.domain.inspection.repository.ItemFrequencySummaryRepository;
import com.school.management.domain.inspection.repository.PeriodSummaryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AnalyticsQueryService CQRS 读侧查询服务单测.
 *
 * 用 Mockito 隔离 6 个汇总 / 整改仓储, 验证:
 *   - 简单转发方法 (daily/period/trend/comparison/dimension/inspector) 的参数传递与结果回传
 *   - getCorrectiveSummaryLive 单遍计数聚合 (状态/优先级/升级/逾期)
 *   - getHeatmapData 默认日期兜底 + targetType 过滤 + map 组装
 *   - getIssueFlowData sankey 节点/链路构建 + 0 值过滤
 *   - getTimingStats 平均解决时长 / 关闭率 / 逾期率 (含除零兜底)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsQueryService CQRS 读侧查询服务")
class AnalyticsQueryServiceTest {

    @Mock DailySummaryRepository dailySummaryRepository;
    @Mock PeriodSummaryRepository periodSummaryRepository;
    @Mock CorrectiveCaseRepository correctiveCaseRepository;
    @Mock InspectorSummaryRepository inspectorSummaryRepository;
    @Mock ItemFrequencySummaryRepository itemFrequencySummaryRepository;
    @Mock CorrectiveSummaryRepository correctiveSummaryRepository;

    @InjectMocks AnalyticsQueryService service;

    // ---------- helpers ----------

    private DailySummary daily(String targetType, Long targetId, String targetName,
                               LocalDate date, BigDecimal avgScore) {
        DailySummary d = new DailySummary();
        d.setTargetType(targetType);
        d.setTargetId(targetId);
        d.setTargetName(targetName);
        d.setSummaryDate(date);
        d.setAvgScore(avgScore);
        return d;
    }

    private CorrectiveCase caseOf(CaseStatus status, CasePriority priority,
                                  Integer escalationLevel, LocalDateTime deadline) {
        return CorrectiveCase.builder()
                .id(1L).caseCode("C-1")
                .status(status)
                .priority(priority)
                .escalationLevel(escalationLevel)
                .deadline(deadline)
                .build();
    }

    // ============================================================
    @Nested
    @DisplayName("Daily / Period / Trend / Comparison / Dimension 转发查询")
    class SimpleForwardingTests {

        @Test
        @DisplayName("getDailyRanking 转发到 dailySummaryRepository.findRanking")
        void shouldForwardDailyRanking() {
            LocalDate date = LocalDate.of(2026, 5, 1);
            List<DailySummary> expected = List.of(daily("ORG", 1L, "A", date, BigDecimal.TEN));
            when(dailySummaryRepository.findRanking(7L, date)).thenReturn(expected);

            List<DailySummary> result = service.getDailyRanking(7L, date);

            assertThat(result).isSameAs(expected);
            verify(dailySummaryRepository).findRanking(7L, date);
        }

        @Test
        @DisplayName("getDailySummary 转发到 findByProjectAndDate")
        void shouldForwardDailySummary() {
            LocalDate date = LocalDate.of(2026, 5, 2);
            List<DailySummary> expected = List.of(daily("ORG", 2L, "B", date, BigDecimal.ONE));
            when(dailySummaryRepository.findByProjectAndDate(7L, date)).thenReturn(expected);

            assertThat(service.getDailySummary(7L, date)).isSameAs(expected);
        }

        @Test
        @DisplayName("getPeriodSummary 转发到 periodSummaryRepository.findByProjectAndPeriod")
        void shouldForwardPeriodSummary() {
            LocalDate start = LocalDate.of(2026, 5, 1);
            List<PeriodSummary> expected = List.of(new PeriodSummary());
            when(periodSummaryRepository.findByProjectAndPeriod(7L, PeriodType.WEEKLY, start))
                    .thenReturn(expected);

            assertThat(service.getPeriodSummary(7L, PeriodType.WEEKLY, start)).isSameAs(expected);
        }

        @Test
        @DisplayName("getPeriodRanking 转发到 periodSummaryRepository.findRanking")
        void shouldForwardPeriodRanking() {
            LocalDate start = LocalDate.of(2026, 5, 1);
            List<PeriodSummary> expected = List.of(new PeriodSummary());
            when(periodSummaryRepository.findRanking(7L, PeriodType.MONTHLY, start))
                    .thenReturn(expected);

            assertThat(service.getPeriodRanking(7L, PeriodType.MONTHLY, start)).isSameAs(expected);
        }

        @Test
        @DisplayName("getTrend 转发到 dailySummaryRepository.findByTarget — projectId 不参与查询")
        void shouldForwardTrend() {
            LocalDate from = LocalDate.of(2026, 5, 1);
            LocalDate to = LocalDate.of(2026, 5, 31);
            List<DailySummary> expected = List.of(daily("ORG", 9L, "X", from, BigDecimal.ZERO));
            when(dailySummaryRepository.findByTarget("ORG", 9L, from, to)).thenReturn(expected);

            assertThat(service.getTrend(7L, "ORG", 9L, from, to)).isSameAs(expected);
            verify(dailySummaryRepository).findByTarget("ORG", 9L, from, to);
        }

        @Test
        @DisplayName("getComparison 转发到 findByProjectAndDate")
        void shouldForwardComparison() {
            LocalDate date = LocalDate.of(2026, 5, 5);
            List<DailySummary> expected = List.of(daily("ORG", 3L, "C", date, BigDecimal.TEN));
            when(dailySummaryRepository.findByProjectAndDate(7L, date)).thenReturn(expected);

            assertThat(service.getComparison(7L, date)).isSameAs(expected);
        }

        @Test
        @DisplayName("getDimensionBreakdown 转发到 findByProjectAndDateRange")
        void shouldForwardDimensionBreakdown() {
            LocalDate from = LocalDate.of(2026, 5, 1);
            LocalDate to = LocalDate.of(2026, 5, 31);
            List<DailySummary> expected = List.of(daily("ORG", 4L, "D", from, BigDecimal.ONE));
            when(dailySummaryRepository.findByProjectAndDateRange(7L, from, to)).thenReturn(expected);

            assertThat(service.getDimensionBreakdown(7L, from, to)).isSameAs(expected);
        }

        @Test
        @DisplayName("getInspectorPerformance 转发到 periodSummaryRepository.findByProjectAndPeriod")
        void shouldForwardInspectorPerformance() {
            LocalDate start = LocalDate.of(2026, 5, 1);
            List<PeriodSummary> expected = List.of(new PeriodSummary());
            when(periodSummaryRepository.findByProjectAndPeriod(7L, PeriodType.QUARTERLY, start))
                    .thenReturn(expected);

            assertThat(service.getInspectorPerformance(7L, PeriodType.QUARTERLY, start))
                    .isSameAs(expected);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("Inspector / Item / Corrective 读模型转发查询")
    class ReadModelForwardingTests {

        @Test
        @DisplayName("getInspectorSummaries 转发到 inspectorSummaryRepository.findByProjectAndPeriod")
        void shouldForwardInspectorSummaries() {
            LocalDate start = LocalDate.of(2026, 5, 1);
            List<InspectorSummary> expected = List.of(new InspectorSummary());
            when(inspectorSummaryRepository.findByProjectAndPeriod(7L, PeriodType.WEEKLY, start))
                    .thenReturn(expected);

            assertThat(service.getInspectorSummaries(7L, PeriodType.WEEKLY, start)).isSameAs(expected);
        }

        @Test
        @DisplayName("getInspectorSummary 转发到 findByInspectorAndPeriod")
        void shouldForwardInspectorSummary() {
            LocalDate start = LocalDate.of(2026, 5, 1);
            Optional<InspectorSummary> expected = Optional.of(new InspectorSummary());
            when(inspectorSummaryRepository.findByInspectorAndPeriod(7L, 88L, PeriodType.WEEKLY, start))
                    .thenReturn(expected);

            assertThat(service.getInspectorSummary(7L, 88L, PeriodType.WEEKLY, start)).isSameAs(expected);
        }

        @Test
        @DisplayName("getInspectorSummary 未命中返回 Optional.empty")
        void shouldReturnEmptyInspectorSummary() {
            LocalDate start = LocalDate.of(2026, 5, 1);
            when(inspectorSummaryRepository.findByInspectorAndPeriod(7L, 88L, PeriodType.WEEKLY, start))
                    .thenReturn(Optional.empty());

            assertThat(service.getInspectorSummary(7L, 88L, PeriodType.WEEKLY, start)).isEmpty();
        }

        @Test
        @DisplayName("getItemFrequencies 转发到 itemFrequencySummaryRepository.findByProjectAndPeriod")
        void shouldForwardItemFrequencies() {
            LocalDate start = LocalDate.of(2026, 5, 1);
            List<ItemFrequencySummary> expected = List.of(new ItemFrequencySummary());
            when(itemFrequencySummaryRepository.findByProjectAndPeriod(7L, PeriodType.MONTHLY, start))
                    .thenReturn(expected);

            assertThat(service.getItemFrequencies(7L, PeriodType.MONTHLY, start)).isSameAs(expected);
        }

        @Test
        @DisplayName("getParetoTopN 转发到 findTopNByDeduction 并带 limit")
        void shouldForwardParetoTopN() {
            LocalDate start = LocalDate.of(2026, 5, 1);
            List<ItemFrequencySummary> expected = List.of(new ItemFrequencySummary());
            when(itemFrequencySummaryRepository.findTopNByDeduction(7L, PeriodType.MONTHLY, start, 5))
                    .thenReturn(expected);

            assertThat(service.getParetoTopN(7L, PeriodType.MONTHLY, start, 5)).isSameAs(expected);
            verify(itemFrequencySummaryRepository).findTopNByDeduction(7L, PeriodType.MONTHLY, start, 5);
        }

        @Test
        @DisplayName("getCorrectiveSummary 转发到 correctiveSummaryRepository.findByProjectAndPeriod")
        void shouldForwardCorrectiveSummary() {
            LocalDate start = LocalDate.of(2026, 5, 1);
            Optional<CorrectiveSummary> expected = Optional.of(new CorrectiveSummary());
            when(correctiveSummaryRepository.findByProjectAndPeriod(7L, PeriodType.YEARLY, start))
                    .thenReturn(expected);

            assertThat(service.getCorrectiveSummary(7L, PeriodType.YEARLY, start)).isSameAs(expected);
        }

        @Test
        @DisplayName("getCorrectiveSummaries 转发到 findByProject")
        void shouldForwardCorrectiveSummaries() {
            List<CorrectiveSummary> expected = List.of(new CorrectiveSummary());
            when(correctiveSummaryRepository.findByProject(7L)).thenReturn(expected);

            assertThat(service.getCorrectiveSummaries(7L)).isSameAs(expected);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getCorrectiveSummaryLive — 单遍计数聚合")
    class CorrectiveSummaryLiveTests {

        @Test
        @DisplayName("空案例列表: 所有计数为 0")
        void shouldReturnAllZeroForEmpty() {
            when(correctiveCaseRepository.findByProjectId(7L)).thenReturn(List.of());

            Map<String, Object> result = service.getCorrectiveSummaryLive(7L);

            assertThat(result.get("total")).isEqualTo(0);
            assertThat(result.get("open")).isEqualTo(0L);
            assertThat(result.get("closed")).isEqualTo(0L);
            assertThat(result.get("escalated")).isEqualTo(0L);
            assertThat(result.get("overdue")).isEqualTo(0L);
            assertThat(result.get("critical")).isEqualTo(0L);
            assertThat(result.get("high")).isEqualTo(0L);
        }

        @Test
        @DisplayName("混合状态/优先级: 正确分桶计数")
        void shouldCountStatusesAndPriorities() {
            LocalDateTime past = LocalDateTime.now().minusDays(2);
            List<CorrectiveCase> cases = List.of(
                    caseOf(CaseStatus.OPEN, CasePriority.CRITICAL, 0, null),
                    caseOf(CaseStatus.OPEN, CasePriority.HIGH, 1, null),       // escalated
                    caseOf(CaseStatus.ASSIGNED, CasePriority.HIGH, 0, past),   // overdue
                    caseOf(CaseStatus.IN_PROGRESS, CasePriority.MEDIUM, 0, null),
                    caseOf(CaseStatus.SUBMITTED, CasePriority.LOW, 0, null),
                    caseOf(CaseStatus.VERIFIED, CasePriority.LOW, 0, past),    // verified -> not overdue
                    caseOf(CaseStatus.REJECTED, CasePriority.LOW, 0, null),
                    caseOf(CaseStatus.CLOSED, CasePriority.LOW, 0, past)       // closed -> not overdue
            );
            when(correctiveCaseRepository.findByProjectId(7L)).thenReturn(cases);

            Map<String, Object> result = service.getCorrectiveSummaryLive(7L);

            assertThat(result.get("total")).isEqualTo(8);
            assertThat(result.get("open")).isEqualTo(2L);
            assertThat(result.get("assigned")).isEqualTo(1L);
            assertThat(result.get("inProgress")).isEqualTo(1L);
            assertThat(result.get("submitted")).isEqualTo(1L);
            assertThat(result.get("verified")).isEqualTo(1L);
            assertThat(result.get("rejected")).isEqualTo(1L);
            assertThat(result.get("closed")).isEqualTo(1L);
            assertThat(result.get("escalated")).isEqualTo(1L);
            assertThat(result.get("overdue")).isEqualTo(1L);
            assertThat(result.get("critical")).isEqualTo(1L);
            assertThat(result.get("high")).isEqualTo(2L);
        }

        @Test
        @DisplayName("status 为 null: 归入 UNKNOWN 桶, 不计入命名状态")
        void shouldBucketNullStatusAsUnknown() {
            // builder().status(null) 会默认成 OPEN — 真正的 null status 只能用 mock 造,
            // 才能命中 service 的 "UNKNOWN" 兜底分支.
            CorrectiveCase c = org.mockito.Mockito.mock(CorrectiveCase.class);
            when(correctiveCaseRepository.findByProjectId(7L)).thenReturn(List.of(c));

            Map<String, Object> result = service.getCorrectiveSummaryLive(7L);

            assertThat(result.get("total")).isEqualTo(1);
            assertThat(result.get("open")).isEqualTo(0L);
            assertThat(result.get("escalated")).isEqualTo(0L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getHeatmapData — 热力图数据组装")
    class HeatmapTests {

        @Test
        @DisplayName("dateFrom/dateTo 为 null 时使用近 30 天默认窗口")
        void shouldDefaultDateRangeWhenNull() {
            when(dailySummaryRepository.findByProjectAndDateRange(eq(7L), any(), any()))
                    .thenReturn(List.of());

            List<Map<String, Object>> result = service.getHeatmapData(7L, null, null, null);

            assertThat(result).isEmpty();
            verify(dailySummaryRepository).findByProjectAndDateRange(
                    eq(7L),
                    eq(LocalDate.now().minusDays(30)),
                    eq(LocalDate.now()));
        }

        @Test
        @DisplayName("显式日期窗口直接使用, 不兜底")
        void shouldUseExplicitDateRange() {
            LocalDate from = LocalDate.of(2026, 1, 1);
            LocalDate to = LocalDate.of(2026, 1, 31);
            when(dailySummaryRepository.findByProjectAndDateRange(7L, from, to)).thenReturn(List.of());

            service.getHeatmapData(7L, from, to, null);

            verify(dailySummaryRepository).findByProjectAndDateRange(7L, from, to);
        }

        @Test
        @DisplayName("targetType 为 null/空白: 不过滤, 全部映射成 map item")
        void shouldMapAllWhenNoTargetTypeFilter() {
            LocalDate d = LocalDate.of(2026, 5, 1);
            when(dailySummaryRepository.findByProjectAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(
                            daily("ORG", 1L, "甲", d, BigDecimal.valueOf(90)),
                            daily("PLACE", 2L, "乙", d, BigDecimal.valueOf(80))));

            List<Map<String, Object>> result = service.getHeatmapData(7L, null, null, "  ");

            assertThat(result).hasSize(2);
            Map<String, Object> first = result.get(0);
            assertThat(first.get("targetId")).isEqualTo(1L);
            assertThat(first.get("targetName")).isEqualTo("甲");
            assertThat(first.get("date")).isEqualTo(d);
            assertThat(first.get("score")).isEqualTo(BigDecimal.valueOf(90));
        }

        @Test
        @DisplayName("targetType 指定: 仅保留匹配的目标类型")
        void shouldFilterByTargetType() {
            LocalDate d = LocalDate.of(2026, 5, 1);
            when(dailySummaryRepository.findByProjectAndDateRange(any(), any(), any()))
                    .thenReturn(List.of(
                            daily("ORG", 1L, "甲", d, BigDecimal.valueOf(90)),
                            daily("PLACE", 2L, "乙", d, BigDecimal.valueOf(80))));

            List<Map<String, Object>> result = service.getHeatmapData(7L, null, null, "PLACE");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("targetId")).isEqualTo(2L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getIssueFlowData — Sankey 流向图")
    class IssueFlowTests {

        @Test
        @DisplayName("固定 8 个状态节点始终存在")
        void shouldAlwaysBuild8Nodes() {
            when(correctiveCaseRepository.findByProjectId(7L)).thenReturn(List.of());

            Map<String, Object> result = service.getIssueFlowData(7L, null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) result.get("nodes");
            assertThat(nodes).hasSize(8);
            assertThat(nodes).extracting(n -> n.get("name"))
                    .containsExactly("OPEN", "ASSIGNED", "IN_PROGRESS", "SUBMITTED",
                            "VERIFIED", "REJECTED", "CLOSED", "ESCALATED");
        }

        @Test
        @DisplayName("空案例: links 为空 (0 值转换被过滤)")
        void shouldHaveNoLinksForEmpty() {
            when(correctiveCaseRepository.findByProjectId(7L)).thenReturn(List.of());

            Map<String, Object> result = service.getIssueFlowData(7L, null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> links = (List<Map<String, Object>>) result.get("links");
            assertThat(links).isEmpty();
        }

        @Test
        @DisplayName("混合状态: 仅 value>0 的状态转换形成 link")
        void shouldBuildLinksForNonZeroTransitions() {
            List<CorrectiveCase> cases = List.of(
                    caseOf(CaseStatus.OPEN, CasePriority.LOW, 0, null),       // 无流转
                    caseOf(CaseStatus.CLOSED, CasePriority.LOW, 2, null),     // 全链路 + escalated
                    caseOf(CaseStatus.REJECTED, CasePriority.LOW, 0, null)    // OPEN->ASSIGNED + SUBMITTED->REJECTED
            );
            when(correctiveCaseRepository.findByProjectId(7L)).thenReturn(cases);

            Map<String, Object> result = service.getIssueFlowData(7L, null, null);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> links = (List<Map<String, Object>>) result.get("links");
            // 每条 link 都有正 value
            assertThat(links).isNotEmpty();
            assertThat(links).allSatisfy(l ->
                    assertThat((Long) l.get("value")).isGreaterThan(0L));
            // CLOSED 案例驱动 VERIFIED->CLOSED 链路, value=1
            Map<String, Object> verifiedToClosed = links.stream()
                    .filter(l -> "VERIFIED".equals(l.get("source")) && "CLOSED".equals(l.get("target")))
                    .findFirst().orElseThrow();
            assertThat(verifiedToClosed.get("value")).isEqualTo(1L);
            // OPEN->ASSIGNED: CLOSED + REJECTED 两个非 OPEN 案例
            Map<String, Object> openToAssigned = links.stream()
                    .filter(l -> "OPEN".equals(l.get("source")) && "ASSIGNED".equals(l.get("target")))
                    .findFirst().orElseThrow();
            assertThat(openToAssigned.get("value")).isEqualTo(2L);
            // OPEN->ESCALATED: 仅 escalationLevel>0 的 CLOSED 案例
            Map<String, Object> escalated = links.stream()
                    .filter(l -> "ESCALATED".equals(l.get("target")))
                    .findFirst().orElseThrow();
            assertThat(escalated.get("value")).isEqualTo(1L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getTimingStats — 时效统计")
    class TimingStatsTests {

        @Test
        @DisplayName("空案例: 计数与比率全为 0, 无除零异常")
        void shouldReturnZeroForEmpty() {
            when(correctiveCaseRepository.findByProjectId(7L)).thenReturn(List.of());

            Map<String, Object> result = service.getTimingStats(7L, null, null);

            assertThat(result.get("totalCases")).isEqualTo(0L);
            assertThat(result.get("closedCases")).isEqualTo(0L);
            assertThat(result.get("overdueCases")).isEqualTo(0L);
            assertThat(result.get("avgResolutionHours")).isEqualTo(0.0);
            assertThat(result.get("closureRate")).isEqualTo(0.0);
            assertThat(result.get("overdueRate")).isEqualTo(0.0);
        }

        @Test
        @DisplayName("含已关闭案例: 计算平均解决时长与关闭率")
        void shouldComputeAvgResolutionAndRates() {
            LocalDateTime created = LocalDateTime.of(2026, 5, 1, 0, 0);
            CorrectiveCase closed = CorrectiveCase.builder()
                    .id(1L).caseCode("C-1").status(CaseStatus.CLOSED)
                    .priority(CasePriority.LOW)
                    .createdAt(created)
                    .verifiedAt(created.plusHours(10))
                    .build();
            CorrectiveCase open = caseOf(CaseStatus.OPEN, CasePriority.LOW, 0, null);
            CorrectiveCase overdue = caseOf(CaseStatus.ASSIGNED, CasePriority.LOW, 0,
                    LocalDateTime.now().minusDays(1));
            when(correctiveCaseRepository.findByProjectId(7L))
                    .thenReturn(List.of(closed, open, overdue));

            Map<String, Object> result = service.getTimingStats(7L, null, null);

            assertThat(result.get("totalCases")).isEqualTo(3L);
            assertThat(result.get("closedCases")).isEqualTo(1L);
            assertThat(result.get("overdueCases")).isEqualTo(1L);
            assertThat(result.get("avgResolutionHours")).isEqualTo(10.0);
            // 1/3 closed -> 33.33%
            assertThat((Double) result.get("closureRate")).isEqualTo(33.33);
            // 1/3 overdue -> 33.33%
            assertThat((Double) result.get("overdueRate")).isEqualTo(33.33);
        }

        @Test
        @DisplayName("已关闭但缺少 verifiedAt: 不计入平均解决时长 -> 默认 0.0")
        void shouldIgnoreClosedWithoutVerifiedAt() {
            CorrectiveCase closedNoVerify = CorrectiveCase.builder()
                    .id(1L).caseCode("C-1").status(CaseStatus.CLOSED)
                    .priority(CasePriority.LOW)
                    .createdAt(LocalDateTime.of(2026, 5, 1, 0, 0))
                    .build();
            when(correctiveCaseRepository.findByProjectId(7L))
                    .thenReturn(List.of(closedNoVerify));

            Map<String, Object> result = service.getTimingStats(7L, null, null);

            assertThat(result.get("closedCases")).isEqualTo(1L);
            assertThat(result.get("avgResolutionHours")).isEqualTo(0.0);
            assertThat((Double) result.get("closureRate")).isEqualTo(100.0);
        }
    }
}
