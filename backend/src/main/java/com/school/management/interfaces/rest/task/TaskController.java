package com.school.management.interfaces.rest.task;

import com.school.management.application.task.TaskApplicationService;
import com.school.management.application.task.TaskQueryService;
import com.school.management.application.task.command.*;
import com.school.management.application.task.query.TaskDTO;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.interfaces.rest.task.dto.*;
import com.school.management.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务管理控制器 (V2 - DDD架构)
 */
@Tag(name = "任务管理 V2", description = "基于DDD架构的任务管理API")
@RestController("taskControllerV2")
@RequestMapping("/v2/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskApplicationService taskApplicationService;
    private final TaskQueryService taskQueryService;

    @Operation(summary = "创建任务")
    @PostMapping
    @PreAuthorize("hasAuthority('task:create')")
    public Result<Long> createTask(@Valid @RequestBody CreateTaskRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentUsername();

        CreateTaskCommand command = CreateTaskCommand.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .assignerId(userId)
                .assignerName(userName)
                .departmentId(request.getDepartmentId())
                .dueDate(request.getDueDate())
                .targetIds(request.getTargetIds())
                .assignType(request.getAssignType())
                .workflowTemplateId(request.getWorkflowTemplateId())
                .attachmentIds(request.getAttachmentIds())
                .build();

        Long taskId = taskApplicationService.createTask(command);
        return Result.success(taskId);
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('task:view', 'task:manage')")
    public Result<TaskDTO> getTask(@PathVariable Long id) {
        TaskDTO task = taskApplicationService.getById(id);
        return Result.success(task);
    }

    @Operation(summary = "分页查询任务")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('task:view', 'task:manage')")
    public Result<PageResult<TaskDTO>> listTasks(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Long userId = SecurityUtils.getCurrentUserId();
        PageResult<TaskDTO> result = taskApplicationService.findByPage(
                keyword, userId, departmentId, status, priority, overdue, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "接收任务")
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('task:execute')")
    public Result<Void> acceptTask(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        taskApplicationService.acceptTask(id, userId);
        return Result.success();
    }

    @Operation(summary = "提交任务")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('task:execute')")
    public Result<Void> submitTask(@PathVariable Long id, @Valid @RequestBody SubmitTaskRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentUsername();

        SubmitTaskCommand command = SubmitTaskCommand.builder()
                .taskId(id)
                .submitterId(userId)
                .submitterName(userName)
                .content(request.getContent())
                .attachmentIds(request.getAttachmentIds())
                .build();

        taskApplicationService.submitTask(command);
        return Result.success();
    }

    @Operation(summary = "审批任务")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('task:approve')")
    public Result<Void> approveTask(@PathVariable Long id, @Valid @RequestBody ApproveTaskRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentUsername();

        ApproveTaskCommand command = ApproveTaskCommand.builder()
                .taskId(id)
                .approverId(userId)
                .approverName(userName)
                .approved(request.getApproved())
                .comment(request.getComment())
                .build();

        taskApplicationService.approveTask(command);
        return Result.success();
    }

    @Operation(summary = "删除任务")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('task:manage')")
    public Result<Void> deleteTask(@PathVariable Long id) {
        taskApplicationService.deleteTask(id);
        return Result.success();
    }

    @Operation(summary = "获取我的待办任务")
    @GetMapping("/my/pending")
    @PreAuthorize("hasAuthority('task:execute')")
    public Result<List<TaskDTO>> getMyPendingTasks() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<TaskDTO> tasks = taskQueryService.findPendingTasksForUser(userId);
        return Result.success(tasks);
    }

    @Operation(summary = "获取我的进行中任务")
    @GetMapping("/my/in-progress")
    @PreAuthorize("hasAuthority('task:execute')")
    public Result<List<TaskDTO>> getMyInProgressTasks() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<TaskDTO> tasks = taskQueryService.findInProgressTasksForUser(userId);
        return Result.success(tasks);
    }

    @Operation(summary = "获取待我审批的任务")
    @GetMapping("/my/pending-approval")
    @PreAuthorize("hasAuthority('task:approve')")
    public Result<List<TaskDTO>> getPendingApprovalTasks() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<TaskDTO> tasks = taskQueryService.findPendingApprovalForUser(userId);
        return Result.success(tasks);
    }

    @Operation(summary = "获取任务统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyAuthority('task:view', 'task:manage')")
    public Result<TaskQueryService.TaskStatistics> getStatistics() {
        TaskQueryService.TaskStatistics statistics = taskQueryService.getStatistics();
        return Result.success(statistics);
    }

    @Operation(summary = "获取逾期任务")
    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('task:manage')")
    public Result<List<TaskDTO>> getOverdueTasks() {
        List<TaskDTO> tasks = taskQueryService.findOverdueTasks();
        return Result.success(tasks);
    }
}
