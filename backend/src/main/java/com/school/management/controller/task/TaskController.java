package com.school.management.controller.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.dto.task.*;
import com.school.management.util.SecurityUtils;
import com.school.management.service.task.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 任务管理控制器
 */
@Tag(name = "任务管理", description = "任务的创建、分配、提交、审批等操作")
@RestController
@RequestMapping("/task/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "分页查询任务")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('task:view', 'task:manage')")
    public Result<IPage<TaskDTO>> pageQuery(TaskQueryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        // 这里可以根据用户角色获取部门ID
        Long departmentId = null; // 可以从SecurityUtils获取
        IPage<TaskDTO> page = taskService.pageQuery(request, userId, departmentId);
        return Result.success(page);
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('task:view', 'task:manage')")
    public Result<TaskDTO> getDetail(@PathVariable Long id) {
        TaskDTO task = taskService.getDetail(id);
        return Result.success(task);
    }

    @Operation(summary = "创建任务")
    @PostMapping
    @PreAuthorize("hasAuthority('task:create')")
    public Result<TaskDTO> create(@Valid @RequestBody TaskCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentUsername();
        TaskDTO task = taskService.create(request, userId, userName);
        return Result.success(task);
    }

    @Operation(summary = "接收任务")
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('task:execute')")
    public Result<TaskDTO> acceptTask(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentUsername();
        TaskDTO task = taskService.acceptTask(id, userId, userName);
        return Result.success(task);
    }

    @Operation(summary = "提交任务")
    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('task:execute')")
    public Result<TaskDTO> submitTask(@Valid @RequestBody TaskSubmitRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentUsername();
        TaskDTO task = taskService.submitTask(request, userId, userName);
        return Result.success(task);
    }

    @Operation(summary = "审批任务")
    @PostMapping("/approve")
    @PreAuthorize("hasAuthority('task:approve')")
    public Result<TaskDTO> approveTask(@Valid @RequestBody TaskApproveRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentUsername();
        TaskDTO task = taskService.approveTask(request, userId, userName);
        return Result.success(task);
    }

    @Operation(summary = "取消任务")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('task:manage')")
    public Result<Boolean> cancelTask(@PathVariable Long id, @RequestParam(required = false) String reason) {
        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentUsername();
        boolean success = taskService.cancelTask(id, reason, userId, userName);
        return Result.success(success);
    }

    @Operation(summary = "获取我的任务")
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('task:execute')")
    public Result<IPage<TaskDTO>> getMyTasks(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<TaskDTO> page = taskService.getMyTasks(pageNum, pageSize, status, userId);
        return Result.success(page);
    }

    @Operation(summary = "获取待我审批的任务")
    @GetMapping("/pending-approval")
    @PreAuthorize("hasAuthority('task:approve')")
    public Result<IPage<TaskDTO>> getPendingApprovalTasks(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<TaskDTO> page = taskService.getPendingApprovalTasks(pageNum, pageSize, userId);
        return Result.success(page);
    }

    @Operation(summary = "获取任务统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyAuthority('task:view', 'task:manage')")
    public Result<TaskStatisticsDTO> getStatistics() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long departmentId = null; // 可以从SecurityUtils获取
        TaskStatisticsDTO statistics = taskService.getStatistics(userId, departmentId);
        return Result.success(statistics);
    }

    @Operation(summary = "获取任务流程进度")
    @GetMapping("/{id}/progress")
    @PreAuthorize("hasAnyAuthority('task:view', 'task:execute', 'task:approve', 'task:manage')")
    public Result<java.util.List<TaskProgressNodeDTO>> getTaskProgress(@PathVariable Long id) {
        java.util.List<TaskProgressNodeDTO> progress = taskService.getTaskProgress(id);
        return Result.success(progress);
    }

    @Operation(summary = "获取任务详情（含卡片式执行人数据）")
    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAnyAuthority('task:view', 'task:execute', 'task:approve', 'task:manage')")
    public Result<TaskDetailDTO> getTaskDetail(@PathVariable Long id) {
        TaskDetailDTO detail = taskService.getTaskDetail(id);
        return Result.success(detail);
    }
}
