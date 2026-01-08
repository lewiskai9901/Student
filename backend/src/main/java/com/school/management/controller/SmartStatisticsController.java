package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.dto.statistics.smart.*;
import com.school.management.service.SmartStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 智能统计分析控制器
 * 提供动态适应、多维度分析的统计API
 */
@Slf4j
@RestController
@RequestMapping("/check-plans/{planId}/smart-statistics")
@RequiredArgsConstructor
@Tag(name = "智能统计分析", description = "检查计划智能统计分析相关接口")
public class SmartStatisticsController {

    private final SmartStatisticsService smartStatisticsService;

    /**
     * 获取智能统计概览
     */
    @GetMapping("/overview")
    @Operation(summary = "获取智能统计概览", description = "获取检查计划的智能统计概览，包含覆盖率、趋势、警告等信息")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<SmartStatisticsOverviewVO> getSmartOverview(
            @Parameter(description = "检查计划ID") @PathVariable Long planId,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "检查记录ID列表") @RequestParam(required = false) List<Long> recordIds) {

        SmartStatisticsFilters filters = SmartStatisticsFilters.builder()
                .planId(planId)
                .startDate(startDate)
                .endDate(endDate)
                .recordIds(recordIds)
                .build();

        SmartStatisticsOverviewVO overview = smartStatisticsService.getSmartOverview(filters);
        return Result.success(overview);
    }

    /**
     * 获取智能班级排名
     */
    @GetMapping("/ranking")
    @Operation(summary = "获取智能班级排名", description = "获取班级排名，支持多维度排名、缺检处理、轮次归一化")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<SmartRankingResultVO> getSmartRanking(
            @Parameter(description = "检查计划ID") @PathVariable Long planId,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "对比模式: total/average/normalized/weighted") @RequestParam(defaultValue = "average") String compareMode,
            @Parameter(description = "是否包含部分检查的班级") @RequestParam(defaultValue = "false") Boolean includePartial,
            @Parameter(description = "班级ID列表") @RequestParam(required = false) List<Long> classIds,
            @Parameter(description = "年级ID列表") @RequestParam(required = false) List<Long> gradeIds,
            @Parameter(description = "部门ID列表") @RequestParam(required = false) List<Long> departmentIds,
            @Parameter(description = "排序字段") @RequestParam(required = false) String sortBy,
            @Parameter(description = "排序方向: asc/desc") @RequestParam(defaultValue = "asc") String sortOrder,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {

        SmartStatisticsFilters filters = SmartStatisticsFilters.builder()
                .planId(planId)
                .startDate(startDate)
                .endDate(endDate)
                .compareMode(compareMode)
                .includePartial(includePartial)
                .classIds(classIds)
                .gradeIds(gradeIds)
                .departmentIds(departmentIds)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();

        SmartRankingResultVO result = smartStatisticsService.getSmartRanking(filters);
        return Result.success(result);
    }

    /**
     * 获取动态类别统计
     */
    @GetMapping("/categories")
    @Operation(summary = "获取动态类别统计", description = "自动识别并统计检查涉及的所有类别")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<DynamicCategoryStatsVO> getDynamicCategoryStats(
            @Parameter(description = "检查计划ID") @PathVariable Long planId,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "类别ID列表") @RequestParam(required = false) List<Long> categoryIds) {

        SmartStatisticsFilters filters = SmartStatisticsFilters.builder()
                .planId(planId)
                .startDate(startDate)
                .endDate(endDate)
                .categoryIds(categoryIds)
                .build();

        DynamicCategoryStatsVO result = smartStatisticsService.getDynamicCategoryStats(filters);
        return Result.success(result);
    }

    /**
     * 获取轮次分析
     */
    @GetMapping("/rounds")
    @Operation(summary = "获取轮次分析", description = "分析多轮检查之间的变化情况")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<RoundAnalysisVO> getRoundAnalysis(
            @Parameter(description = "检查计划ID") @PathVariable Long planId,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        SmartStatisticsFilters filters = SmartStatisticsFilters.builder()
                .planId(planId)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        RoundAnalysisVO result = smartStatisticsService.getRoundAnalysis(filters);
        return Result.success(result);
    }

    /**
     * 获取班级追踪数据
     */
    @GetMapping("/class-tracking/{classId}")
    @Operation(summary = "获取班级追踪数据", description = "追踪单个班级在检查计划下的历史表现")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<ClassTrackingVO> getClassTracking(
            @Parameter(description = "检查计划ID") @PathVariable Long planId,
            @Parameter(description = "班级ID") @PathVariable Long classId) {

        ClassTrackingVO result = smartStatisticsService.getClassTracking(planId, classId);
        return Result.success(result);
    }

    /**
     * 获取检查覆盖率
     */
    @GetMapping("/coverage")
    @Operation(summary = "获取检查覆盖率", description = "获取检查计划的班级覆盖情况")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<CheckCoverageVO> getCheckCoverage(
            @Parameter(description = "检查计划ID") @PathVariable Long planId,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        SmartStatisticsFilters filters = SmartStatisticsFilters.builder()
                .planId(planId)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        CheckCoverageVO result = smartStatisticsService.getCheckCoverage(planId, filters);
        return Result.success(result);
    }

    /**
     * 获取统计洞察
     */
    @GetMapping("/insights")
    @Operation(summary = "获取统计洞察", description = "自动分析数据并生成文字洞察")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<List<String>> getInsights(
            @Parameter(description = "检查计划ID") @PathVariable Long planId,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        SmartStatisticsFilters filters = SmartStatisticsFilters.builder()
                .planId(planId)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        List<String> insights = smartStatisticsService.generateInsights(filters);
        return Result.success(insights);
    }
}
