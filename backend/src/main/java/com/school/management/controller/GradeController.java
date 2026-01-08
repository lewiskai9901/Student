package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.dto.GradeDTO;
import com.school.management.dto.request.GradeCreateRequest;
import com.school.management.dto.request.GradeUpdateRequest;
import com.school.management.dto.vo.GradeStatisticsVO;
import com.school.management.service.GradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 年级管理控制器 (V1 - 已弃用)
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 * @deprecated 使用 V2 API {@link com.school.management.interfaces.rest.organization.GradeController} 替代
 *             V2 API 路径: /api/v2/grades
 */
@Deprecated(since = "2.0.0", forRemoval = false)
@Slf4j
@RestController
@RequestMapping("/quantification/grades")
@RequiredArgsConstructor
@Tag(name = "年级管理 (已弃用)", description = "年级管理相关接口 - 请使用 /api/v2/grades")
public class GradeController {

    private final GradeService gradeService;

    /**
     * 创建年级
     */
    @PostMapping
    @Operation(summary = "创建年级", description = "创建新的年级")
    @PreAuthorize("hasAuthority('quantification:grade:add')")
    public Result<GradeDTO> createGrade(@Validated @RequestBody GradeCreateRequest request) {
        log.info("创建年级: {}", request);
        GradeDTO gradeDTO = gradeService.createGrade(request);
        return Result.success(gradeDTO);
    }

    /**
     * 更新年级
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新年级", description = "更新年级信息")
    @PreAuthorize("hasAuthority('quantification:grade:edit')")
    public Result<GradeDTO> updateGrade(
            @Parameter(description = "年级ID") @PathVariable Long id,
            @Validated @RequestBody GradeUpdateRequest request) {
        log.info("更新年级 ID={}: {}", id, request);
        request.setId(id);
        GradeDTO gradeDTO = gradeService.updateGrade(request);
        return Result.success(gradeDTO);
    }

    /**
     * 删除年级
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除年级", description = "逻辑删除年级")
    @PreAuthorize("hasAuthority('quantification:grade:delete')")
    public Result<Boolean> deleteGrade(@Parameter(description = "年级ID") @PathVariable Long id) {
        log.info("删除年级: {}", id);
        boolean success = gradeService.deleteGrade(id);
        return Result.success(success);
    }

    /**
     * 分页查询年级列表
     */
    @GetMapping
    @Operation(summary = "分页查询年级", description = "分页查询年级列表")
    @PreAuthorize("hasAuthority('quantification:grade:view')")
    public Result<IPage<GradeDTO>> listGrades(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "入学年份") @RequestParam(required = false) Integer enrollmentYear,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {

        log.info("分页查询年级: pageNum={}, pageSize={}, enrollmentYear={}, status={}, keyword={}",
                pageNum, pageSize, enrollmentYear, status, keyword);

        IPage<GradeDTO> page = gradeService.listGrades(pageNum, pageSize,
                enrollmentYear, status, keyword);
        return Result.success(page);
    }

    /**
     * 查询所有年级(不分页)
     */
    @GetMapping("/all")
    @Operation(summary = "查询所有年级", description = "查询所有年级列表(不分页)")
    @PreAuthorize("hasAuthority('quantification:grade:view')")
    public Result<List<GradeDTO>> getAllGrades() {
        log.info("查询所有年级");
        IPage<GradeDTO> page = gradeService.listGrades(1, 10000, null, 1, null);
        return Result.success(page.getRecords());
    }

    /**
     * 根据ID查询年级详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询年级详情", description = "根据ID查询年级详情")
    @PreAuthorize("hasAuthority('quantification:grade:view')")
    public Result<GradeDTO> getGradeById(@Parameter(description = "年级ID") @PathVariable Long id) {
        log.info("查询年级详情: {}", id);
        GradeDTO gradeDTO = gradeService.getGradeById(id);
        return Result.success(gradeDTO);
    }

    /**
     * 同步年级统计信息
     */
    @PostMapping("/{id}/sync-statistics")
    @Operation(summary = "同步年级统计", description = "同步年级的班级和学生统计信息")
    @PreAuthorize("hasAuthority('quantification:grade:edit')")
    public Result<Boolean> syncGradeStatistics(@Parameter(description = "年级ID") @PathVariable Long id) {
        log.info("同步年级统计: {}", id);
        boolean success = gradeService.syncGradeStatistics(id);
        return Result.success(success);
    }

    /**
     * 获取年级统计信息
     */
    @GetMapping("/{id}/statistics")
    @Operation(summary = "获取年级统计", description = "获取年级的统计信息")
    @PreAuthorize("hasAuthority('quantification:grade:view')")
    public Result<GradeStatisticsVO> getGradeStatistics(@Parameter(description = "年级ID") @PathVariable Long id) {
        log.info("获取年级统计: {}", id);
        GradeStatisticsVO statistics = gradeService.getGradeStatistics(id);
        return Result.success(statistics);
    }

    /**
     * 更新年级主任
     */
    @PutMapping("/{id}/director")
    @Operation(summary = "更新年级主任", description = "更新年级的主任")
    @PreAuthorize("hasAuthority('quantification:grade:edit')")
    public Result<Boolean> updateGradeDirector(
            @Parameter(description = "年级ID") @PathVariable Long id,
            @Parameter(description = "主任ID") @RequestParam Long directorId) {
        log.info("更新年级主任: gradeId={}, directorId={}", id, directorId);
        boolean success = gradeService.updateGradeDirector(id, directorId);
        return Result.success(success);
    }

    /**
     * 批量更新年级状态
     */
    @PutMapping("/batch-status")
    @Operation(summary = "批量更新状态", description = "批量更新年级状态")
    @PreAuthorize("hasAuthority('quantification:grade:edit')")
    public Result<Boolean> batchUpdateStatus(
            @Parameter(description = "年级ID列表") @RequestParam List<Long> gradeIds,
            @Parameter(description = "状态") @RequestParam Integer status) {
        log.info("批量更新年级状态: gradeIds={}, status={}", gradeIds, status);
        boolean success = gradeService.batchUpdateStatus(gradeIds, status);
        return Result.success(success);
    }

    /**
     * 根据年级主任查询年级
     */
    @GetMapping("/director/{directorId}")
    @Operation(summary = "主任年级列表", description = "查询年级主任管理的所有年级")
    @PreAuthorize("hasAuthority('quantification:grade:view')")
    public Result<List<GradeDTO>> listGradesByDirector(
            @Parameter(description = "主任ID") @PathVariable Long directorId) {
        log.info("查询主任年级: {}", directorId);
        List<GradeDTO> grades = gradeService.listGradesByDirector(directorId);
        return Result.success(grades);
    }

    /**
     * 检查年级是否可以删除
     */
    @GetMapping("/{id}/can-delete")
    @Operation(summary = "检查可删除性", description = "检查年级是否可以删除")
    @PreAuthorize("hasAuthority('quantification:grade:view')")
    public Result<Boolean> checkCanDelete(@Parameter(description = "年级ID") @PathVariable Long id) {
        log.info("检查年级可删除性: {}", id);
        boolean canDelete = gradeService.checkCanDelete(id);
        return Result.success(canDelete);
    }
}
