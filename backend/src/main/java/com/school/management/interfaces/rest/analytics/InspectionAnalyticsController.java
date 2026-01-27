package com.school.management.interfaces.rest.analytics;

import com.school.management.application.analytics.InspectionAnalyticsService;
import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v2/inspection/analytics")
@Tag(name = "Inspection Analytics V4", description = "量化检查数据分析接口")
@RequiredArgsConstructor
public class InspectionAnalyticsController {

    private final InspectionAnalyticsService analyticsService;

    @GetMapping("/class-ranking")
    @Operation(summary = "获取班级排名趋势")
    @PreAuthorize("hasAuthority('analytics:view')")
    public Result<AnalyticsResponse> getClassRanking(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(AnalyticsResponse.of("CLASS_RANKING",
            analyticsService.getClassRankingTrend(startDate, endDate)));
    }

    @GetMapping("/violation-distribution")
    @Operation(summary = "获取违规分布")
    @PreAuthorize("hasAuthority('analytics:view')")
    public Result<AnalyticsResponse> getViolationDistribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(AnalyticsResponse.of("VIOLATION_DISTRIBUTION",
            analyticsService.getViolationDistribution(startDate, endDate)));
    }

    @GetMapping("/inspector-workload")
    @Operation(summary = "获取检查员工作量")
    @PreAuthorize("hasAuthority('analytics:view')")
    public Result<AnalyticsResponse> getInspectorWorkload(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(AnalyticsResponse.of("INSPECTOR_WORKLOAD",
            analyticsService.getInspectorWorkload(startDate, endDate)));
    }

    @GetMapping("/department-comparison")
    @Operation(summary = "获取系部对比")
    @PreAuthorize("hasAuthority('analytics:view')")
    public Result<AnalyticsResponse> getDepartmentComparison(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(AnalyticsResponse.of("DEPARTMENT_COMPARISON",
            analyticsService.getDepartmentComparison(startDate, endDate)));
    }
}
