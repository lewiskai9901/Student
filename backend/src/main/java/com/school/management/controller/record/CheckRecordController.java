package com.school.management.controller.record;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.dto.record.CheckRecordDTO;
import com.school.management.dto.record.CheckRecordQueryDTO;
import com.school.management.dto.record.CheckRecordClassStatsDTO;
import com.school.management.dto.record.CheckRecordDeductionDTO;
import com.school.management.dto.record.WeightConfigDetailDTO;
import com.school.management.dto.record.WeightConfigTreeDTO;
import com.school.management.entity.record.CheckRecordNew;
import com.school.management.security.CustomUserDetails;
import com.school.management.service.record.CheckRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 检查记录控制器 (V1 - 已弃用)
 *
 * @author system
 * @since 2.0.0
 * @deprecated 使用 V2 API {@link com.school.management.interfaces.rest.inspection.InspectionRecordController} 替代
 *             V2 API 路径: /api/v2/inspection-records
 */
@Deprecated(since = "2.0.0", forRemoval = false)
@Slf4j
@RestController
@RequestMapping("/quantification/check-records")
@RequiredArgsConstructor
@Tag(name = "检查记录管理 (已弃用)", description = "检查记录相关接口 - 请使用 /api/v2/inspection-records")
public class CheckRecordController {

    private final CheckRecordService checkRecordService;

    @GetMapping
    @Operation(summary = "分页查询检查记录")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<IPage<CheckRecordDTO>> queryPage(CheckRecordQueryDTO query) {
        IPage<CheckRecordDTO> page = checkRecordService.queryPage(query);
        return Result.success(page);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询检查记录（兼容前端）")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<IPage<CheckRecordDTO>> queryPageList(CheckRecordQueryDTO query) {
        IPage<CheckRecordDTO> page = checkRecordService.queryPage(query);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取检查记录详情")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<CheckRecordDTO> getById(@PathVariable Long id) {
        CheckRecordDTO dto = checkRecordService.getById(id);
        return Result.success(dto);
    }

    @GetMapping("/{id}/detail")
    @Operation(summary = "获取检查记录完整详情（包含班级统计）")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<CheckRecordDTO> getDetailById(@PathVariable Long id) {
        CheckRecordDTO dto = checkRecordService.getDetailById(id);
        return Result.success(dto);
    }

    @GetMapping("/by-daily-check/{dailyCheckId}")
    @Operation(summary = "根据日常检查ID获取记录")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<CheckRecordDTO> getByDailyCheckId(@PathVariable Long dailyCheckId) {
        CheckRecordDTO dto = checkRecordService.getByDailyCheckId(dailyCheckId);
        return Result.success(dto);
    }

    @PostMapping("/generate/{dailyCheckId}")
    @Operation(summary = "从日常检查生成检查记录")
    @PreAuthorize("hasAuthority('quantification:record:create')")
    public Result<CheckRecordDTO> generateFromDailyCheck(
            @PathVariable Long dailyCheckId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CheckRecordNew record = checkRecordService.generateFromDailyCheck(
                dailyCheckId,
                userDetails.getUserId(),
                userDetails.getRealName()
        );
        CheckRecordDTO dto = checkRecordService.getById(record.getId());
        return Result.success("检查记录生成成功", dto);
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "归档检查记录")
    @PreAuthorize("hasAuthority('quantification:record:archive')")
    public Result<Void> archive(@PathVariable Long id) {
        checkRecordService.archive(id);
        return Result.success("检查记录已归档", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除检查记录")
    @PreAuthorize("hasAuthority('quantification:record:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        checkRecordService.delete(id);
        return Result.success("检查记录已删除", null);
    }

    @PostMapping("/{id}/recalculate")
    @Operation(summary = "重新计算检查记录统计")
    @PreAuthorize("hasAuthority('quantification:record:edit')")
    public Result<Void> recalculate(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "手动重算") String reason) {
        checkRecordService.recalculate(id, reason);
        return Result.success("重新计算完成", null);
    }

    // ==================== 班级统计相关 ====================

    @GetMapping("/{recordId}/class-stats")
    @Operation(summary = "获取班级统计列表")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<List<CheckRecordClassStatsDTO>> getClassStatsList(@PathVariable Long recordId) {
        List<CheckRecordClassStatsDTO> list = checkRecordService.getClassStatsList(recordId);
        return Result.success(list);
    }

    @GetMapping("/class-stats/{classStatId}")
    @Operation(summary = "获取班级统计详情")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<CheckRecordClassStatsDTO> getClassStatsDetail(@PathVariable Long classStatId) {
        CheckRecordClassStatsDTO dto = checkRecordService.getClassStatsDetail(classStatId);
        return Result.success(dto);
    }

    @GetMapping("/{recordId}/class-stats/by-department/{departmentId}")
    @Operation(summary = "按院系获取班级统计")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<List<CheckRecordClassStatsDTO>> getClassStatsByDepartment(
            @PathVariable Long recordId,
            @PathVariable Long departmentId) {
        List<CheckRecordClassStatsDTO> list = checkRecordService.getClassStatsByDepartment(recordId, departmentId);
        return Result.success(list);
    }

    @GetMapping("/{recordId}/class-stats/by-grade/{gradeId}")
    @Operation(summary = "按年级获取班级统计")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<List<CheckRecordClassStatsDTO>> getClassStatsByGrade(
            @PathVariable Long recordId,
            @PathVariable Long gradeId) {
        List<CheckRecordClassStatsDTO> list = checkRecordService.getClassStatsByGrade(recordId, gradeId);
        return Result.success(list);
    }

    // ==================== 扣分明细相关 ====================

    @GetMapping("/{recordId}/deductions")
    @Operation(summary = "获取扣分明细列表")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<List<CheckRecordDeductionDTO>> getDeductionList(
            @PathVariable Long recordId,
            @RequestParam(required = false) @Parameter(description = "班级ID，可选") Long classId) {
        List<CheckRecordDeductionDTO> list = checkRecordService.getDeductionList(recordId, classId);
        return Result.success(list);
    }

    @GetMapping("/deductions/{deductionId}")
    @Operation(summary = "获取扣分明细详情")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<CheckRecordDeductionDTO> getDeductionById(@PathVariable Long deductionId) {
        CheckRecordDeductionDTO dto = checkRecordService.getDeductionById(deductionId);
        return Result.success(dto);
    }

    // ==================== 排名视图相关 ====================

    @GetMapping("/{recordId}/ranking")
    @Operation(summary = "获取班级排名列表（含加权分数）")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<List<CheckRecordClassStatsDTO>> getClassRanking(
            @PathVariable Long recordId,
            @RequestParam(required = false, defaultValue = "weighted") @Parameter(description = "排序方式：weighted=加权分数, original=原始分数") String sortBy,
            @RequestParam(required = false) @Parameter(description = "院系ID筛选") Long departmentId,
            @RequestParam(required = false) @Parameter(description = "年级筛选") Integer gradeLevel) {
        List<CheckRecordClassStatsDTO> list = checkRecordService.getClassRankingWithWeight(recordId, sortBy, departmentId, gradeLevel);
        return Result.success(list);
    }

    // ==================== 加权配置相关 ====================

    @GetMapping("/{recordId}/weight-config")
    @Operation(summary = "获取检查记录的加权配置详情")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<WeightConfigDetailDTO> getWeightConfigDetail(@PathVariable Long recordId) {
        WeightConfigDetailDTO detail = checkRecordService.getWeightConfigDetail(recordId);
        return Result.success(detail);
    }

    @GetMapping("/{recordId}/weight-config-tree")
    @Operation(summary = "获取检查记录的加权配置树形结构（含3级继承关系）")
    @PreAuthorize("hasAuthority('quantification:record:view')")
    public Result<WeightConfigTreeDTO> getWeightConfigTree(@PathVariable Long recordId) {
        WeightConfigTreeDTO tree = checkRecordService.getWeightConfigTree(recordId);
        return Result.success(tree);
    }
}
