package com.school.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.Result;
import com.school.management.dto.analysis.AnalysisConfigDTO;
import com.school.management.dto.analysis.AnalysisReportDTO;
import com.school.management.dto.request.CheckPlanCreateRequest;
import com.school.management.dto.request.CheckPlanQueryRequest;
import com.school.management.dto.request.CheckPlanUpdateRequest;
import com.school.management.dto.response.CheckPlanListVO;
import com.school.management.dto.response.CheckPlanStatisticsVO;
import com.school.management.dto.response.CheckPlanVO;
import com.school.management.entity.CheckPlan;
import com.school.management.service.AnalysisConfigService;
import com.school.management.service.CheckPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 检查计划Controller
 *
 * @author system
 * @since 3.0.0
 */
@Slf4j
@RestController
@RequestMapping("/check-plans")
@RequiredArgsConstructor
@Tag(name = "检查计划管理", description = "检查计划的增删改查及状态管理")
public class CheckPlanController {

    private final CheckPlanService checkPlanService;
    private final AnalysisConfigService analysisConfigService;

    @PostMapping
    @Operation(summary = "创建检查计划")
    @PreAuthorize("hasAuthority('quantification:plan:add')")
    public Result<CheckPlan> createPlan(@Validated @RequestBody CheckPlanCreateRequest request) {
        CheckPlan plan = checkPlanService.createPlan(request);
        return Result.success(plan);
    }

    @PutMapping
    @Operation(summary = "更新检查计划")
    @PreAuthorize("hasAuthority('quantification:plan:edit')")
    public Result<CheckPlan> updatePlan(@Validated @RequestBody CheckPlanUpdateRequest request) {
        CheckPlan plan = checkPlanService.updatePlan(request);
        return Result.success(plan);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除检查计划")
    @PreAuthorize("hasAuthority('quantification:plan:delete')")
    public Result<Void> deletePlan(@PathVariable Long id) {
        checkPlanService.deletePlan(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取计划详情")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public Result<CheckPlanVO> getPlanDetail(@PathVariable Long id) {
        CheckPlanVO vo = checkPlanService.getPlanDetail(id);
        return Result.success(vo);
    }

    @GetMapping
    @Operation(summary = "分页查询计划列表")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public Result<Page<CheckPlanListVO>> getPlanPage(CheckPlanQueryRequest request) {
        Page<CheckPlanListVO> page = checkPlanService.getPlanPage(request);
        return Result.success(page);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "开始计划")
    @PreAuthorize("hasAuthority('quantification:plan:manage')")
    public Result<Void> startPlan(@PathVariable Long id) {
        checkPlanService.startPlan(id);
        return Result.success();
    }

    @PostMapping("/{id}/finish")
    @Operation(summary = "结束计划")
    @PreAuthorize("hasAuthority('quantification:plan:manage')")
    public Result<Void> finishPlan(@PathVariable Long id) {
        checkPlanService.finishPlan(id);
        return Result.success();
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "归档计划")
    @PreAuthorize("hasAuthority('quantification:plan:manage')")
    public Result<Void> archivePlan(@PathVariable Long id) {
        checkPlanService.archivePlan(id);
        return Result.success();
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取计划统计数据")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public Result<CheckPlanStatisticsVO> getPlanStatistics() {
        CheckPlanStatisticsVO vo = checkPlanService.getPlanStatistics();
        return Result.success(vo);
    }

    @GetMapping("/{id}/template-snapshot")
    @Operation(summary = "获取计划的模板快照")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public Result<String> getTemplateSnapshot(@PathVariable Long id) {
        CheckPlanVO vo = checkPlanService.getPlanDetail(id);
        return Result.success(vo.getTemplateSnapshot());
    }

    @GetMapping("/{id}/target-classes")
    @Operation(summary = "获取计划的目标班级列表")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public Result<List<?>> getTargetClasses(@PathVariable Long id) {
        return Result.success(checkPlanService.getTargetClasses(id));
    }

    @GetMapping("/{id}/target-scope")
    @Operation(summary = "获取计划的目标范围配置")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public Result<Object> getTargetScope(@PathVariable Long id) {
        CheckPlanVO vo = checkPlanService.getPlanDetail(id);
        return Result.success(new TargetScopeInfo(vo.getTargetScopeType(), vo.getTargetScopeConfig()));
    }

    private record TargetScopeInfo(String scopeType, String scopeConfig) {}

    // ==================== 统计分析配置接口 ====================

    @GetMapping("/{planId}/analysis/configs")
    @Operation(summary = "获取计划的分析配置列表")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public Result<List<AnalysisConfigDTO>> getAnalysisConfigs(
            @Parameter(description = "计划ID") @PathVariable Long planId) {
        return Result.success(analysisConfigService.getConfigsByPlanId(planId));
    }

    @GetMapping("/{planId}/analysis/configs/{configId}")
    @Operation(summary = "获取分析配置详情")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public Result<AnalysisConfigDTO> getAnalysisConfigDetail(
            @Parameter(description = "计划ID") @PathVariable Long planId,
            @Parameter(description = "配置ID") @PathVariable Long configId) {
        return Result.success(analysisConfigService.getConfigById(configId));
    }

    @PostMapping("/{planId}/analysis/configs")
    @Operation(summary = "创建分析配置")
    @PreAuthorize("hasAuthority('quantification:plan:edit')")
    public Result<Long> createAnalysisConfig(
            @Parameter(description = "计划ID") @PathVariable Long planId,
            @Valid @RequestBody AnalysisConfigDTO dto) {
        dto.setPlanId(planId);
        Long id = analysisConfigService.createConfig(dto);
        return Result.success(id);
    }

    @PutMapping("/{planId}/analysis/configs/{configId}")
    @Operation(summary = "更新分析配置")
    @PreAuthorize("hasAuthority('quantification:plan:edit')")
    public Result<Void> updateAnalysisConfig(
            @Parameter(description = "计划ID") @PathVariable Long planId,
            @Parameter(description = "配置ID") @PathVariable Long configId,
            @Valid @RequestBody AnalysisConfigDTO dto) {
        dto.setId(configId);
        dto.setPlanId(planId);
        analysisConfigService.updateConfig(dto);
        return Result.success();
    }

    @DeleteMapping("/{planId}/analysis/configs/{configId}")
    @Operation(summary = "删除分析配置")
    @PreAuthorize("hasAuthority('quantification:plan:edit')")
    public Result<Void> deleteAnalysisConfig(
            @Parameter(description = "计划ID") @PathVariable Long planId,
            @Parameter(description = "配置ID") @PathVariable Long configId) {
        analysisConfigService.deleteConfig(configId);
        return Result.success();
    }

    @GetMapping("/{planId}/analysis/report/{configId}")
    @Operation(summary = "生成分析报告")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public Result<AnalysisReportDTO> generateAnalysisReport(
            @Parameter(description = "计划ID") @PathVariable Long planId,
            @Parameter(description = "配置ID") @PathVariable Long configId) {
        return Result.success(analysisConfigService.generateReport(configId));
    }

    @GetMapping("/{planId}/analysis/report/{configId}/export/excel")
    @Operation(summary = "导出Excel分析报告")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public void exportAnalysisExcel(
            @Parameter(description = "计划ID") @PathVariable Long planId,
            @Parameter(description = "配置ID") @PathVariable Long configId,
            HttpServletResponse response) throws IOException {
        AnalysisConfigDTO config = analysisConfigService.getConfigById(configId);
        byte[] data = analysisConfigService.exportToExcel(configId);

        String fileName = URLEncoder.encode(config.getName() + "_分析报告.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        response.getOutputStream().write(data);
    }

    @GetMapping("/{planId}/analysis/report/{configId}/export/pdf")
    @Operation(summary = "导出PDF分析报告")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public void exportAnalysisPdf(
            @Parameter(description = "计划ID") @PathVariable Long planId,
            @Parameter(description = "配置ID") @PathVariable Long configId,
            HttpServletResponse response) throws IOException {
        AnalysisConfigDTO config = analysisConfigService.getConfigById(configId);
        byte[] data = analysisConfigService.exportToPdf(configId);

        String fileName = URLEncoder.encode(config.getName() + "_分析报告.pdf", StandardCharsets.UTF_8);
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        response.getOutputStream().write(data);
    }

    @GetMapping("/analysis/options/time-ranges")
    @Operation(summary = "获取时间范围选项")
    public Result<Object> getAnalysisTimeRanges() {
        return Result.success(new Object[]{
                new TimeRangeOption("PLAN_FULL", "整个计划期间"),
                new TimeRangeOption("LAST_7_DAYS", "最近7天"),
                new TimeRangeOption("LAST_30_DAYS", "最近30天"),
                new TimeRangeOption("CUSTOM", "自定义范围")
        });
    }

    @GetMapping("/analysis/options/report-sections")
    @Operation(summary = "获取报告模块选项")
    public Result<Object> getAnalysisReportSections() {
        return Result.success(new Object[]{
                new ReportSectionOption("CLASS_RANKING", "班级排名", "横向柱状图"),
                new ReportSectionOption("DISTRIBUTION", "类别分布", "饼图"),
                new ReportSectionOption("FREQUENCY_STATS", "扣分项频次", "柱状图"),
                new ReportSectionOption("TREND_ANALYSIS", "趋势分析", "折线图")
        });
    }

    // ==================== 内部类 ====================
    private record TimeRangeOption(String code, String name) {}
    private record ReportSectionOption(String code, String name, String chartType) {}
}
