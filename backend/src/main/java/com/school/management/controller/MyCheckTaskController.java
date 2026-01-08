package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.entity.CheckTaskAssignment;
import com.school.management.service.CheckPlanInspectorService;
import com.school.management.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 我的检查任务Controller（打分人员视角）
 */
@RestController
@RequestMapping("/my-check-tasks")
@RequiredArgsConstructor
@Tag(name = "我的检查任务", description = "打分人员查看和执行分配的检查任务")
@PreAuthorize("isAuthenticated()")
public class MyCheckTaskController {

    private final CheckPlanInspectorService inspectorService;

    @GetMapping
    @Operation(summary = "获取我的检查任务列表")
    public Result<IPage<CheckTaskAssignment>> getMyTasks(
            @Parameter(description = "任务状态：0待处理 1进行中 2已完成") @RequestParam(required = false) Integer status,
            @Parameter(description = "计划ID") @RequestParam(required = false) Long planId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(inspectorService.getMyTasks(userId, status, planId, pageNum, pageSize));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "获取任务详情")
    public Result<CheckTaskAssignment> getTaskDetail(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(inspectorService.getTaskDetail(taskId, userId));
    }

    @PutMapping("/{taskId}/start")
    @Operation(summary = "开始执行任务")
    public Result<Void> startTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        Long userId = SecurityUtils.getCurrentUserId();
        inspectorService.startTask(taskId, userId);
        return Result.success();
    }

    @PutMapping("/{taskId}/complete")
    @Operation(summary = "完成任务")
    public Result<Void> completeTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        Long userId = SecurityUtils.getCurrentUserId();
        inspectorService.completeTask(taskId, userId);
        return Result.success();
    }

    @GetMapping("/pending-count")
    @Operation(summary = "获取待处理任务数量")
    public Result<Integer> getPendingCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(inspectorService.countPendingTasks(userId));
    }
}
