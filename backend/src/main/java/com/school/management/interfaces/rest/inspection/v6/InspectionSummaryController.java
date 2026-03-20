package com.school.management.interfaces.rest.inspection.v6;

import com.school.management.application.inspection.v6.InspectionSummaryService;
import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * V6检查汇总统计控制器
 */
@RequiredArgsConstructor
@Tag(name = "V6检查汇总", description = "V6检查汇总统计接口")
@RestController
@RequestMapping("/v6/inspection-summaries")
public class InspectionSummaryController {

    private final InspectionSummaryService summaryService;

    // ========== 汇总生成 ==========

    @Operation(summary = "手动生成日汇总")
    @PostMapping("/projects/{projectId}/daily")
    @CasbinAccess(resource = "inspection:summary", action = "generate")
    public Result<Void> generateDailySummary(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        summaryService.generateDailySummary(projectId, date);
        return Result.success();
    }

    @Operation(summary = "手动生成周汇总")
    @PostMapping("/projects/{projectId}/weekly")
    @CasbinAccess(resource = "inspection:summary", action = "generate")
    public Result<Void> generateWeeklySummary(
            @PathVariable Long projectId,
            @RequestParam int year,
            @RequestParam int weekNumber) {

        summaryService.generateWeeklySummary(projectId, year, weekNumber);
        return Result.success();
    }

    @Operation(summary = "手动生成月汇总")
    @PostMapping("/projects/{projectId}/monthly")
    @CasbinAccess(resource = "inspection:summary", action = "generate")
    public Result<Void> generateMonthlySummary(
            @PathVariable Long projectId,
            @RequestParam int year,
            @RequestParam int month) {

        summaryService.generateMonthlySummary(projectId, year, month);
        return Result.success();
    }

    // ========== 排名查询 ==========

    @Operation(summary = "获取日排名")
    @GetMapping("/projects/{projectId}/daily-ranking")
    @CasbinAccess(resource = "inspection:summary", action = "view")
    public Result<List<Map<String, Object>>> getDailyRanking(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String targetType) {

        List<Map<String, Object>> ranking = summaryService.getDailyRanking(projectId, date, targetType);
        return Result.success(ranking);
    }

    @Operation(summary = "获取组织汇总")
    @GetMapping("/projects/{projectId}/org-summary")
    @CasbinAccess(resource = "inspection:summary", action = "view")
    public Result<List<Map<String, Object>>> getOrgSummary(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Map<String, Object>> summary = summaryService.getOrgSummary(projectId, startDate, endDate);
        return Result.success(summary);
    }

    @Operation(summary = "获取班级汇总")
    @GetMapping("/projects/{projectId}/class-summary")
    @CasbinAccess(resource = "inspection:summary", action = "view")
    public Result<List<Map<String, Object>>> getClassSummary(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Map<String, Object>> summary = summaryService.getClassSummary(projectId, startDate, endDate);
        return Result.success(summary);
    }

    @Operation(summary = "获取趋势数据")
    @GetMapping("/projects/{projectId}/trend")
    @CasbinAccess(resource = "inspection:summary", action = "view")
    public Result<List<Map<String, Object>>> getTrend(
            @PathVariable Long projectId,
            @RequestParam Long targetId,
            @RequestParam String targetType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Map<String, Object>> trend = summaryService.getTrend(projectId, targetId, targetType, startDate, endDate);
        return Result.success(trend);
    }
}
