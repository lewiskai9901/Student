package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.dto.quickentry.*;
import com.school.management.service.QuickEntryService;
import com.school.management.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 快捷录入控制器
 * 用于按人次扣分的快速录入功能
 *
 * @author system
 * @since 1.0.7
 */
@Slf4j
@RestController
@RequestMapping("/quantification/daily-checks/{checkId}/quick-entry")
@RequiredArgsConstructor
@Tag(name = "快捷录入", description = "按人次扣分的快速录入接口")
public class QuickEntryController {

    private final QuickEntryService quickEntryService;

    /**
     * 获取可用的按人次扣分项列表
     */
    @GetMapping("/deduction-items")
    @PreAuthorize("hasAuthority('quantification:check:score')")
    @Operation(summary = "获取可用扣分项", description = "获取当前检查中可用于快捷录入的按人次扣分项")
    public Result<List<QuickEntryDeductionItemDTO>> getDeductionItems(
            @Parameter(description = "检查ID") @PathVariable Long checkId) {
        List<QuickEntryDeductionItemDTO> items = quickEntryService.getAvailableDeductionItems(checkId);
        return Result.success(items);
    }

    /**
     * 搜索学生
     */
    @GetMapping("/search-students")
    @PreAuthorize("hasAuthority('quantification:check:score')")
    @Operation(summary = "搜索学生", description = "在检查目标范围内搜索学生")
    public Result<List<QuickEntryStudentDTO>> searchStudents(
            @Parameter(description = "检查ID") @PathVariable Long checkId,
            @Parameter(description = "搜索关键词(姓名或学号)") @RequestParam String keyword,
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "20") Integer limit) {
        List<QuickEntryStudentDTO> students = quickEntryService.searchStudents(checkId, keyword, limit);
        return Result.success(students);
    }

    /**
     * 检查是否存在重复记录
     */
    @PostMapping("/check-duplicate")
    @PreAuthorize("hasAuthority('quantification:check:score')")
    @Operation(summary = "检查重复", description = "检查指定学生在指定扣分项是否已有记录")
    public Result<QuickEntryCheckDuplicateResponse> checkDuplicate(
            @Parameter(description = "检查ID") @PathVariable Long checkId,
            @Valid @RequestBody QuickEntryCheckDuplicateRequest request) {
        QuickEntryCheckDuplicateResponse response = quickEntryService.checkDuplicate(checkId, request);
        return Result.success(response);
    }

    /**
     * 提交快捷录入
     */
    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('quantification:check:score')")
    @Operation(summary = "提交录入", description = "提交一条快捷录入记录")
    public Result<QuickEntryRecordDTO> submitEntry(
            @Parameter(description = "检查ID") @PathVariable Long checkId,
            @Valid @RequestBody QuickEntrySubmitRequest request) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        QuickEntryRecordDTO record = quickEntryService.submitEntry(checkId, request, operatorId);
        return Result.success(record);
    }

    /**
     * 撤销录入
     */
    @DeleteMapping("/records/{recordId}")
    @PreAuthorize("hasAuthority('quantification:check:score')")
    @Operation(summary = "撤销录入", description = "撤销一条当天录入的记录")
    public Result<Void> revokeEntry(
            @Parameter(description = "检查ID") @PathVariable Long checkId,
            @Parameter(description = "记录ID") @PathVariable Long recordId) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        quickEntryService.revokeEntry(checkId, recordId, operatorId);
        return Result.success();
    }

    /**
     * 获取本次检查的快捷录入记录列表
     */
    @GetMapping("/records")
    @PreAuthorize("hasAuthority('quantification:check:score')")
    @Operation(summary = "获取录入记录", description = "获取当前检查中我的快捷录入记录列表")
    public Result<List<QuickEntryRecordDTO>> getMyRecords(
            @Parameter(description = "检查ID") @PathVariable Long checkId,
            @Parameter(description = "是否仅显示自己的记录") @RequestParam(defaultValue = "true") Boolean onlyMine) {
        Long operatorId = onlyMine ? SecurityUtils.getCurrentUserId() : null;
        List<QuickEntryRecordDTO> records = quickEntryService.getMyEntryRecords(checkId, operatorId);
        return Result.success(records);
    }
}
