package com.school.management.controller.task;

import com.school.management.common.result.Result;
import com.school.management.dto.task.TaskApprovalDTO;
import com.school.management.dto.task.TaskApproveRequest;
import com.school.management.dto.task.TaskSubmitRequest;
import com.school.management.service.task.TaskApprovalService;
import com.school.management.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务审批控制器 (V1 - 已弃用)
 *
 * @deprecated 使用 V2 API {@link com.school.management.interfaces.rest.task.TaskController} 替代
 *             V2 API 路径: /api/v2/tasks/{id}/approve
 */
@Deprecated(since = "2.0.0", forRemoval = false)
@Slf4j
@Tag(name = "任务审批 (已弃用)", description = "任务审批 - 请使用 /api/v2/tasks")
@RestController
@RequestMapping("/api/tasks/approvals")
@RequiredArgsConstructor
public class TaskApprovalController {

    private final TaskApprovalService approvalService;

    /**
     * 获取我的待审批列表
     */
    @Operation(summary = "获取我的待审批列表")
    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('task:approve', 'task:manage')")
    public Result<List<TaskApprovalDTO>> getMyPendingApprovals() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<TaskApprovalDTO> approvals = approvalService.getMyPendingApprovals(currentUserId);
        return Result.success(approvals);
    }

    /**
     * 审批任务
     */
    @Operation(summary = "审批任务")
    @PostMapping("/{recordId}/approve")
    @PreAuthorize("hasAnyAuthority('task:approve', 'task:manage')")
    public Result<Void> approveTask(@PathVariable Long recordId,
                                      @Valid @RequestBody TaskApproveRequest request) {
        approvalService.approveTask(recordId, request);
        return Result.success("审批成功", null);
    }

    /**
     * 提交任务完成情况
     */
    @Operation(summary = "提交任务完成情况")
    @PostMapping("/{taskId}/submit")
    @PreAuthorize("hasAnyAuthority('task:execute', 'task:manage')")
    public Result<Void> submitTask(@PathVariable Long taskId,
                                     @Valid @RequestBody TaskSubmitRequest request) {
        approvalService.submitTask(taskId, request);
        return Result.success("提交成功", null);
    }
}
