package com.school.management.controller;

import com.school.management.common.ApiResponse;
import com.school.management.dto.statistics.*;
import com.school.management.service.CheckPlanStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 检查计划统计控制器
 */
@Tag(name = "检查计划统计", description = "检查计划统计分析相关接口")
@RestController
@RequestMapping("/check-plans/{planId}/statistics")
@RequiredArgsConstructor
public class CheckPlanStatisticsController {

    private final CheckPlanStatisticsService statisticsService;

    @Operation(summary = "获取统计概览")
    @GetMapping("/overview")
    @PreAuthorize("hasAnyAuthority('quantification:plan:view', 'quantification:statistics:view')")
    public ApiResponse<PlanStatisticsOverviewVO> getOverview(
            @Parameter(description = "检查计划ID") @PathVariable Long planId,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "班级ID列表") @RequestParam(required = false) List<Long> classIds,
            @Parameter(description = "类别ID列表") @RequestParam(required = false) List<Long> categoryIds
    ) {
        StatisticsFilters filters = StatisticsFilters.builder()
                .planId(planId)
                .startDate(startDate)
                .endDate(endDate)
                .classIds(classIds)
                .categoryIds(categoryIds)
                .build();

        return ApiResponse.success(statisticsService.getOverview(filters));
    }

    @Operation(summary = "获取班级排名")
    @GetMapping("/class-ranking")
    @PreAuthorize("hasAnyAuthority('quantification:plan:view', 'quantification:statistics:view')")
    public ApiResponse<List<ClassRankingVO>> getClassRanking(
            @Parameter(description = "检查计划ID") @PathVariable Long planId,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "班级ID列表") @RequestParam(required = false) List<Long> classIds,
            @Parameter(description = "年级ID列表") @RequestParam(required = false) List<Long> gradeIds,
            @Parameter(description = "是否使用加权分") @RequestParam(required = false, defaultValue = "false") Boolean useWeightedScore,
            @Parameter(description = "排序字段: totalScore/avgScore") @RequestParam(required = false, defaultValue = "totalScore") String sortBy,
            @Parameter(description = "排序方向: asc/desc") @RequestParam(required = false, defaultValue = "asc") String sortOrder,
            @Parameter(description = "返回数量限制") @RequestParam(required = false) Integer topN
    ) {
        StatisticsFilters filters = StatisticsFilters.builder()
                .planId(planId)
                .startDate(startDate)
                .endDate(endDate)
                .classIds(classIds)
                .gradeIds(gradeIds)
                .useWeightedScore(useWeightedScore)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .topN(topN)
                .build();

        return ApiResponse.success(statisticsService.getClassRanking(filters));
    }

    @Operation(summary = "获取类别统计")
    @GetMapping("/category")
    @PreAuthorize("hasAnyAuthority('quantification:plan:view', 'quantification:statistics:view')")
    public ApiResponse<List<CategoryStatisticsVO>> getCategoryStatistics(
            @Parameter(description = "检查计划ID") @PathVariable Long planId,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "班级ID列表") @RequestParam(required = false) List<Long> classIds,
            @Parameter(description = "类别ID列表") @RequestParam(required = false) List<Long> categoryIds
    ) {
        StatisticsFilters filters = StatisticsFilters.builder()
                .planId(planId)
                .startDate(startDate)
                .endDate(endDate)
                .classIds(classIds)
                .categoryIds(categoryIds)
                .build();

        return ApiResponse.success(statisticsService.getCategoryStatistics(filters));
    }

    @Operation(summary = "获取扣分项统计")
    @GetMapping("/items")
    @PreAuthorize("hasAnyAuthority('quantification:plan:view', 'quantification:statistics:view')")
    public ApiResponse<List<ItemStatisticsVO>> getItemStatistics(
            @Parameter(description = "检查计划ID") @PathVariable Long planId,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "班级ID列表") @RequestParam(required = false) List<Long> classIds,
            @Parameter(description = "类别ID列表") @RequestParam(required = false) List<Long> categoryIds,
            @Parameter(description = "扣分项ID列表") @RequestParam(required = false) List<Long> itemIds,
            @Parameter(description = "返回数量限制") @RequestParam(required = false) Integer topN
    ) {
        StatisticsFilters filters = StatisticsFilters.builder()
                .planId(planId)
                .startDate(startDate)
                .endDate(endDate)
                .classIds(classIds)
                .categoryIds(categoryIds)
                .itemIds(itemIds)
                .topN(topN)
                .build();

        return ApiResponse.success(statisticsService.getItemStatistics(filters));
    }

    @Operation(summary = "获取趋势数据")
    @GetMapping("/trend")
    @PreAuthorize("hasAnyAuthority('quantification:plan:view', 'quantification:statistics:view')")
    public ApiResponse<TrendDataVO> getTrendData(
            @Parameter(description = "检查计划ID") @PathVariable Long planId,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "班级ID列表") @RequestParam(required = false) List<Long> classIds,
            @Parameter(description = "统计粒度: day/week/month") @RequestParam(required = false, defaultValue = "day") String granularity
    ) {
        StatisticsFilters filters = StatisticsFilters.builder()
                .planId(planId)
                .startDate(startDate)
                .endDate(endDate)
                .classIds(classIds)
                .trendGranularity(granularity)
                .build();

        return ApiResponse.success(statisticsService.getTrendData(filters));
    }

    @Operation(summary = "导出统计报表")
    @PostMapping("/export")
    @PreAuthorize("hasAnyAuthority('quantification:plan:view', 'quantification:statistics:export')")
    public ApiResponse<String> exportStatistics(
            @Parameter(description = "检查计划ID") @PathVariable Long planId,
            @RequestBody StatisticsFilters filters
    ) {
        filters.setPlanId(planId);
        String filePath = statisticsService.exportStatistics(filters);
        return ApiResponse.success(filePath);
    }
}
