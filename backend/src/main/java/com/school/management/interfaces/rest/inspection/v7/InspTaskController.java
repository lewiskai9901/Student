package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.InspTaskApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.execution.InspTask;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/v7/insp/tasks")
@RequiredArgsConstructor
public class InspTaskController {

    private final InspTaskApplicationService taskService;

    // ========== Task CRUD ==========

    @PostMapping
    @CasbinAccess(resource = "insp:task", action = "create")
    public Result<InspTask> createTask(@RequestBody CreateTaskRequest request) {
        InspTask task = taskService.createTask(request.getProjectId(),
                request.getTaskDate(), request.getTimeSlotCode(),
                request.getTimeSlotStart(), request.getTimeSlotEnd());
        return Result.success(task);
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:task", action = "view")
    public Result<InspTask> getTask(@PathVariable Long id) {
        return Result.success(taskService.getTask(id)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + id)));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:task", action = "view")
    public Result<List<InspTask>> listTasks(@RequestParam(required = false) Long projectId) {
        if (projectId != null) {
            return Result.success(taskService.listTasksByProject(projectId));
        }
        return Result.success(taskService.listAllTasks());
    }

    @GetMapping("/available")
    @CasbinAccess(resource = "insp:task", action = "view")
    public Result<List<InspTask>> listAvailableTasks() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(taskService.listAvailableTasksForUser(userId));
    }

    @GetMapping("/my-tasks")
    @CasbinAccess(resource = "insp:task", action = "view")
    public Result<List<InspTask>> listMyTasks() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(taskService.listMyTasks(userId));
    }

    // ========== Task Lifecycle ==========

    @PostMapping("/{id}/claim")
    @CasbinAccess(resource = "insp:task", action = "execute")
    public Result<InspTask> claimTask(@PathVariable Long id,
                                       @RequestBody ClaimTaskRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(taskService.claimTask(id, userId, request.getInspectorName()));
    }

    @PostMapping("/{id}/start")
    @CasbinAccess(resource = "insp:task", action = "execute")
    public Result<InspTask> startTask(@PathVariable Long id) {
        return Result.success(taskService.startTask(id));
    }

    @PostMapping("/{id}/submit")
    @CasbinAccess(resource = "insp:task", action = "execute")
    public Result<InspTask> submitTask(@PathVariable Long id) {
        return Result.success(taskService.submitTask(id));
    }

    @PostMapping("/{id}/withdraw")
    @CasbinAccess(resource = "insp:task", action = "execute")
    public Result<InspTask> withdrawTask(@PathVariable Long id) {
        return Result.success(taskService.withdrawTask(id));
    }

    @PostMapping("/{id}/reject")
    @CasbinAccess(resource = "insp:task", action = "review")
    public Result<InspTask> rejectTask(@PathVariable Long id,
                                        @RequestBody(required = false) RejectTaskRequest request) {
        String comment = request != null ? request.getComment() : "驳回";
        return Result.success(taskService.rejectTask(id, comment));
    }

    @PostMapping("/{id}/review")
    @CasbinAccess(resource = "insp:task", action = "review")
    public Result<InspTask> reviewTask(@PathVariable Long id,
                                        @RequestBody ReviewTaskRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        taskService.startReview(id, userId, request.getReviewerName());
        return Result.success(taskService.reviewTask(id, request.getComment()));
    }

    @PostMapping("/{id}/publish")
    @CasbinAccess(resource = "insp:task", action = "publish")
    public Result<InspTask> publishTask(@PathVariable Long id) {
        return Result.success(taskService.publishTask(id));
    }

    @PostMapping("/{id}/cancel")
    @CasbinAccess(resource = "insp:task", action = "edit")
    public Result<InspTask> cancelTask(@PathVariable Long id) {
        return Result.success(taskService.cancelTask(id));
    }

    @PostMapping("/{id}/assign")
    @CasbinAccess(resource = "insp:task", action = "edit")
    public Result<InspTask> assignTask(@PathVariable Long id,
                                        @RequestBody AssignTaskRequest request) {
        return Result.success(taskService.assignTask(id,
                request.getInspectorId(), request.getInspectorName()));
    }

    @PostMapping("/{id}/repopulate")
    @CasbinAccess(resource = "insp:task", action = "edit")
    public Result<InspTask> repopulateSubmissions(@PathVariable Long id) {
        return Result.success(taskService.repopulateSubmissions(id));
    }

    // --- Request DTOs ---

    @lombok.Data
    public static class CreateTaskRequest {
        private Long projectId;
        private LocalDate taskDate;
        private String timeSlotCode;
        private LocalTime timeSlotStart;
        private LocalTime timeSlotEnd;
    }

    @lombok.Data
    public static class ClaimTaskRequest {
        private String inspectorName;
    }

    @lombok.Data
    public static class ReviewTaskRequest {
        private String reviewerName;
        private String comment;
    }

    @lombok.Data
    public static class AssignTaskRequest {
        private Long inspectorId;
        private String inspectorName;
    }

    @lombok.Data
    public static class RejectTaskRequest {
        private String comment;
    }
}
