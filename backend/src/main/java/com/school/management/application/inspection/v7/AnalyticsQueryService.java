package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.analytics.*;
import com.school.management.domain.inspection.model.v7.corrective.CorrectiveCase;
import com.school.management.domain.inspection.repository.v7.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * V7 分析查询服务 (CQRS 读侧)
 * 纯读操作，从汇总表查询
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class AnalyticsQueryService {

    private final DailySummaryRepository dailySummaryRepository;
    private final PeriodSummaryRepository periodSummaryRepository;
    private final CorrectiveCaseRepository correctiveCaseRepository;
    private final InspectorSummaryRepository inspectorSummaryRepository;
    private final ItemFrequencySummaryRepository itemFrequencySummaryRepository;
    private final CorrectiveSummaryRepository correctiveSummaryRepository;

    // ========== Daily ==========

    @Transactional(readOnly = true)
    public List<DailySummary> getDailyRanking(Long projectId, LocalDate date) {
        return dailySummaryRepository.findRanking(projectId, date);
    }

    @Transactional(readOnly = true)
    public List<DailySummary> getDailySummary(Long projectId, LocalDate date) {
        return dailySummaryRepository.findByProjectAndDate(projectId, date);
    }

    // ========== Period ==========

    @Transactional(readOnly = true)
    public List<PeriodSummary> getPeriodSummary(Long projectId, PeriodType periodType, LocalDate periodStart) {
        return periodSummaryRepository.findByProjectAndPeriod(projectId, periodType, periodStart);
    }

    @Transactional(readOnly = true)
    public List<PeriodSummary> getPeriodRanking(Long projectId, PeriodType periodType, LocalDate periodStart) {
        return periodSummaryRepository.findRanking(projectId, periodType, periodStart);
    }

    // ========== Trend ==========

    @Transactional(readOnly = true)
    public List<DailySummary> getTrend(Long projectId, String targetType, Long targetId,
                                        LocalDate startDate, LocalDate endDate) {
        return dailySummaryRepository.findByTarget(targetType, targetId, startDate, endDate);
    }

    // ========== Comparison ==========

    @Transactional(readOnly = true)
    public List<DailySummary> getComparison(Long projectId, LocalDate date) {
        return dailySummaryRepository.findByProjectAndDate(projectId, date);
    }

    // ========== Dimension Breakdown ==========

    @Transactional(readOnly = true)
    public List<DailySummary> getDimensionBreakdown(Long projectId, LocalDate startDate, LocalDate endDate) {
        return dailySummaryRepository.findByProjectAndDateRange(projectId, startDate, endDate);
    }

    // ========== Inspector Performance ==========

    @Transactional(readOnly = true)
    public List<PeriodSummary> getInspectorPerformance(Long projectId, PeriodType periodType, LocalDate periodStart) {
        return periodSummaryRepository.findByProjectAndPeriod(projectId, periodType, periodStart);
    }

    // ========== Corrective Summary (legacy, from live data) ==========

    @Transactional(readOnly = true)
    public Map<String, Object> getCorrectiveSummaryLive(Long projectId) {
        List<CorrectiveCase> cases = correctiveCaseRepository.findByProjectId(projectId);
        Map<String, Object> result = new HashMap<>();
        result.put("total", cases.size());
        result.put("open", cases.stream().filter(c -> "OPEN".equals(c.getStatus().name())).count());
        result.put("assigned", cases.stream().filter(c -> "ASSIGNED".equals(c.getStatus().name())).count());
        result.put("inProgress", cases.stream().filter(c -> "IN_PROGRESS".equals(c.getStatus().name())).count());
        result.put("submitted", cases.stream().filter(c -> "SUBMITTED".equals(c.getStatus().name())).count());
        result.put("verified", cases.stream().filter(c -> "VERIFIED".equals(c.getStatus().name())).count());
        result.put("rejected", cases.stream().filter(c -> "REJECTED".equals(c.getStatus().name())).count());
        result.put("closed", cases.stream().filter(c -> "CLOSED".equals(c.getStatus().name())).count());
        result.put("escalated", cases.stream().filter(c -> "ESCALATED".equals(c.getStatus().name())).count());

        long overdue = cases.stream().filter(CorrectiveCase::isOverdue).count();
        result.put("overdue", overdue);

        // Priority breakdown
        result.put("critical", cases.stream().filter(c -> "CRITICAL".equals(c.getPriority().name())).count());
        result.put("high", cases.stream().filter(c -> "HIGH".equals(c.getPriority().name())).count());

        return result;
    }

    // ========== Inspector Performance (from read model) ==========

    @Transactional(readOnly = true)
    public List<InspectorSummary> getInspectorSummaries(Long projectId, PeriodType periodType, LocalDate periodStart) {
        return inspectorSummaryRepository.findByProjectAndPeriod(projectId, periodType, periodStart);
    }

    @Transactional(readOnly = true)
    public Optional<InspectorSummary> getInspectorSummary(Long projectId, Long inspectorId,
                                                            PeriodType periodType, LocalDate periodStart) {
        return inspectorSummaryRepository.findByInspectorAndPeriod(projectId, inspectorId, periodType, periodStart);
    }

    // ========== Item Frequency / Pareto ==========

    @Transactional(readOnly = true)
    public List<ItemFrequencySummary> getItemFrequencies(Long projectId, PeriodType periodType, LocalDate periodStart) {
        return itemFrequencySummaryRepository.findByProjectAndPeriod(projectId, periodType, periodStart);
    }

    @Transactional(readOnly = true)
    public List<ItemFrequencySummary> getParetoTopN(Long projectId, PeriodType periodType,
                                                      LocalDate periodStart, int limit) {
        return itemFrequencySummaryRepository.findTopNByDeduction(projectId, periodType, periodStart, limit);
    }

    // ========== Corrective Summary (from read model) ==========

    @Transactional(readOnly = true)
    public Optional<CorrectiveSummary> getCorrectiveSummary(Long projectId, PeriodType periodType, LocalDate periodStart) {
        return correctiveSummaryRepository.findByProjectAndPeriod(projectId, periodType, periodStart);
    }

    @Transactional(readOnly = true)
    public List<CorrectiveSummary> getCorrectiveSummaries(Long projectId) {
        return correctiveSummaryRepository.findByProject(projectId);
    }

    // ========== Heatmap ==========

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHeatmapData(Long projectId, LocalDate dateFrom, LocalDate dateTo, String targetType) {
        LocalDate from = dateFrom != null ? dateFrom : LocalDate.now().minusDays(30);
        LocalDate to = dateTo != null ? dateTo : LocalDate.now();
        List<DailySummary> summaries = dailySummaryRepository.findByProjectAndDateRange(projectId, from, to);

        return summaries.stream()
                .filter(s -> targetType == null || targetType.isBlank() || targetType.equals(s.getTargetType()))
                .map(s -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("targetId", s.getTargetId());
                    item.put("targetName", s.getTargetName());
                    item.put("date", s.getSummaryDate());
                    item.put("score", s.getAvgScore());
                    return item;
                })
                .collect(Collectors.toList());
    }

    // ========== Issue Flow (Sankey) ==========

    @Transactional(readOnly = true)
    public Map<String, Object> getIssueFlowData(Long projectId, LocalDate dateFrom, LocalDate dateTo) {
        List<CorrectiveCase> cases = correctiveCaseRepository.findByProjectId(projectId);

        // Build sankey nodes: status nodes
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<String> statusNames = List.of("OPEN", "ASSIGNED", "IN_PROGRESS", "SUBMITTED", "VERIFIED", "REJECTED", "CLOSED", "ESCALATED");
        for (String status : statusNames) {
            Map<String, Object> node = new HashMap<>();
            node.put("name", status);
            nodes.add(node);
        }

        // Build sankey links: status transition counts
        Map<String, Long> transitionCounts = new HashMap<>();
        transitionCounts.put("OPEN->ASSIGNED", cases.stream().filter(c -> !"OPEN".equals(c.getStatus().name())).count());
        transitionCounts.put("ASSIGNED->IN_PROGRESS", cases.stream().filter(c -> {
            String s = c.getStatus().name();
            return "IN_PROGRESS".equals(s) || "SUBMITTED".equals(s) || "VERIFIED".equals(s) || "CLOSED".equals(s);
        }).count());
        transitionCounts.put("IN_PROGRESS->SUBMITTED", cases.stream().filter(c -> {
            String s = c.getStatus().name();
            return "SUBMITTED".equals(s) || "VERIFIED".equals(s) || "CLOSED".equals(s);
        }).count());
        transitionCounts.put("SUBMITTED->VERIFIED", cases.stream().filter(c -> "VERIFIED".equals(c.getStatus().name()) || "CLOSED".equals(c.getStatus().name())).count());
        transitionCounts.put("SUBMITTED->REJECTED", cases.stream().filter(c -> "REJECTED".equals(c.getStatus().name())).count());
        transitionCounts.put("VERIFIED->CLOSED", cases.stream().filter(c -> "CLOSED".equals(c.getStatus().name())).count());
        transitionCounts.put("OPEN->ESCALATED", cases.stream().filter(c -> "ESCALATED".equals(c.getStatus().name())).count());

        List<Map<String, Object>> links = new ArrayList<>();
        for (Map.Entry<String, Long> entry : transitionCounts.entrySet()) {
            if (entry.getValue() > 0) {
                String[] parts = entry.getKey().split("->");
                Map<String, Object> link = new HashMap<>();
                link.put("source", parts[0]);
                link.put("target", parts[1]);
                link.put("value", entry.getValue());
                links.add(link);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("links", links);
        return result;
    }

    // ========== Timing Stats ==========

    @Transactional(readOnly = true)
    public Map<String, Object> getTimingStats(Long projectId, LocalDate dateFrom, LocalDate dateTo) {
        List<CorrectiveCase> cases = correctiveCaseRepository.findByProjectId(projectId);

        long totalCases = cases.size();
        long closedCases = cases.stream().filter(c -> "CLOSED".equals(c.getStatus().name())).count();
        long overdueCases = cases.stream().filter(CorrectiveCase::isOverdue).count();

        // Average resolution time (in hours) for closed cases
        double avgResolutionHours = cases.stream()
                .filter(c -> "CLOSED".equals(c.getStatus().name()) && c.getCreatedAt() != null && c.getVerifiedAt() != null)
                .mapToLong(c -> java.time.Duration.between(c.getCreatedAt(), c.getVerifiedAt()).toHours())
                .average()
                .orElse(0.0);

        Map<String, Object> result = new HashMap<>();
        result.put("totalCases", totalCases);
        result.put("closedCases", closedCases);
        result.put("overdueCases", overdueCases);
        result.put("avgResolutionHours", Math.round(avgResolutionHours * 100.0) / 100.0);
        result.put("closureRate", totalCases > 0 ? Math.round(closedCases * 10000.0 / totalCases) / 100.0 : 0.0);
        result.put("overdueRate", totalCases > 0 ? Math.round(overdueCases * 10000.0 / totalCases) / 100.0 : 0.0);
        return result;
    }
}
