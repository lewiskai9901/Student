package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.AnalyticsProjectionService;
import com.school.management.application.inspection.v7.AnalyticsQueryService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.analytics.*;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v7/insp/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsQueryService queryService;
    private final AnalyticsProjectionService projectionService;

    // ========== Daily ==========

    @GetMapping("/daily-ranking")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<List<DailySummary>> getDailyRanking(
            @RequestParam Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(queryService.getDailyRanking(projectId, date));
    }

    @GetMapping("/daily-summary")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<List<DailySummary>> getDailySummary(
            @RequestParam Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(queryService.getDailySummary(projectId, date));
    }

    // ========== Period ==========

    @GetMapping("/period-summary")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<List<PeriodSummary>> getPeriodSummary(
            @RequestParam Long projectId,
            @RequestParam PeriodType periodType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart) {
        return Result.success(queryService.getPeriodSummary(projectId, periodType, periodStart));
    }

    // ========== Trend ==========

    @GetMapping("/trend")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<List<DailySummary>> getTrend(
            @RequestParam Long projectId,
            @RequestParam String targetType,
            @RequestParam Long targetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(queryService.getTrend(projectId, targetType, targetId, startDate, endDate));
    }

    // ========== Comparison ==========

    @GetMapping("/comparison")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<List<DailySummary>> getComparison(
            @RequestParam Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(queryService.getComparison(projectId, date));
    }

    // ========== Dimension Breakdown ==========

    @GetMapping("/dimension-breakdown")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<List<DailySummary>> getDimensionBreakdown(
            @RequestParam Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(queryService.getDimensionBreakdown(projectId, startDate, endDate));
    }

    // ========== Inspector Performance ==========

    @GetMapping("/inspector-performance")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<List<PeriodSummary>> getInspectorPerformance(
            @RequestParam Long projectId,
            @RequestParam PeriodType periodType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart) {
        return Result.success(queryService.getInspectorPerformance(projectId, periodType, periodStart));
    }

    // ========== Corrective Summary (live) ==========

    @GetMapping("/corrective-summary-live")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<Map<String, Object>> getCorrectiveSummaryLive(@RequestParam Long projectId) {
        return Result.success(queryService.getCorrectiveSummaryLive(projectId));
    }

    // ========== Inspector Performance (read model) ==========

    @GetMapping("/inspector-summaries")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<List<InspectorSummary>> getInspectorSummaries(
            @RequestParam Long projectId,
            @RequestParam PeriodType periodType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart) {
        return Result.success(queryService.getInspectorSummaries(projectId, periodType, periodStart));
    }

    // ========== Item Frequency / Pareto ==========

    @GetMapping("/item-frequency")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<List<ItemFrequencySummary>> getItemFrequencies(
            @RequestParam Long projectId,
            @RequestParam PeriodType periodType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart) {
        return Result.success(queryService.getItemFrequencies(projectId, periodType, periodStart));
    }

    @GetMapping("/pareto")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<List<ItemFrequencySummary>> getPareto(
            @RequestParam Long projectId,
            @RequestParam PeriodType periodType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(queryService.getParetoTopN(projectId, periodType, periodStart, limit));
    }

    // ========== Corrective Summary (read model) ==========

    @GetMapping("/corrective-summary")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<CorrectiveSummary> getCorrectiveSummary(
            @RequestParam Long projectId,
            @RequestParam PeriodType periodType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart) {
        return Result.success(queryService.getCorrectiveSummary(projectId, periodType, periodStart).orElse(null));
    }

    @GetMapping("/corrective-summaries")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<List<CorrectiveSummary>> getCorrectiveSummaries(@RequestParam Long projectId) {
        return Result.success(queryService.getCorrectiveSummaries(projectId));
    }

    // ========== Rebuild (Admin) ==========

    @PostMapping("/rebuild-daily")
    @CasbinAccess(resource = "insp:analytics", action = "manage")
    public Result<Void> rebuildDaily(@RequestBody RebuildDailyRequest request) {
        projectionService.rebuildDailySummary(request.getProjectId(), request.getDate());
        return Result.success(null);
    }

    @PostMapping("/rebuild-period")
    @CasbinAccess(resource = "insp:analytics", action = "manage")
    public Result<Void> rebuildPeriod(@RequestBody RebuildPeriodRequest request) {
        projectionService.rebuildPeriodSummary(request.getProjectId(),
                request.getPeriodType(), request.getPeriodStart());
        return Result.success(null);
    }

    // ========== Heatmap ==========

    @GetMapping("/heatmap")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<List<Map<String, Object>>> getHeatmap(
            @RequestParam Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String targetType) {
        return Result.success(queryService.getHeatmapData(projectId, dateFrom, dateTo, targetType));
    }

    // ========== Issue Flow (Sankey) ==========

    @GetMapping("/issue-flow")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<Map<String, Object>> getIssueFlow(
            @RequestParam Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return Result.success(queryService.getIssueFlowData(projectId, dateFrom, dateTo));
    }

    // ========== Timing Stats ==========

    @GetMapping("/timing")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<Map<String, Object>> getTimingStats(
            @RequestParam Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return Result.success(queryService.getTimingStats(projectId, dateFrom, dateTo));
    }

    // ========== Request DTOs ==========

    @lombok.Data
    public static class RebuildDailyRequest {
        private Long projectId;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate date;
    }

    @lombok.Data
    public static class RebuildPeriodRequest {
        private Long projectId;
        private PeriodType periodType;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate periodStart;
    }
}
