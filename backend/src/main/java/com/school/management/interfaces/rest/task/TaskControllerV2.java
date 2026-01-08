package com.school.management.interfaces.rest.task;

import com.school.management.application.task.*;
import com.school.management.common.result.Result;
import com.school.management.domain.task.model.*;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * V2 REST controller for task management.
 */
@RestController
@RequestMapping("/v2/tasks")
@Tag(name = "Tasks V2", description = "Task management API (V2)")
@RequiredArgsConstructor
public class TaskControllerV2 {

    private final TaskApplicationService taskService;
    private final JwtTokenService jwtTokenService;

    @PostMapping
    @Operation(summary = "Create a new task")
    @PreAuthorize("hasAuthority('task:create')")
    public Result<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        CreateTaskCommand command = CreateTaskCommand.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .priority(TaskPriority.fromValue(request.getPriority()))
            .departmentId(request.getDepartmentId())
            .dueDate(request.getDueDate())
            .workflowTemplateId(request.getWorkflowTemplateId())
            .attachmentIds(request.getAttachmentIds())
            .assignees(request.getAssignees() != null ? request.getAssignees().stream()
                .map(a -> CreateTaskCommand.AssigneeInfo.builder()
                    .userId(a.getUserId())
                    .userName(a.getUserName())
                    .departmentId(a.getDepartmentId())
                    .departmentName(a.getDepartmentName())
                    .build())
                .collect(Collectors.toList()) : null)
            .createdBy(getCurrentUserId())
            .createdByName(getCurrentUserName())
            .build();

        Task task = taskService.createTask(command);
        return Result.success(toResponse(task));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    @PreAuthorize("hasAnyAuthority('task:view', 'task:manage')")
    public Result<TaskResponse> getTask(@PathVariable Long id) {
        return taskService.getTask(id)
            .map(task -> Result.success(toResponse(task)))
            .orElse(Result.error("Task not found"));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my assigned tasks")
    @PreAuthorize("hasAuthority('task:execute')")
    public Result<List<TaskResponse>> getMyTasks() {
        List<TaskResponse> tasks = taskService.getMyTasks(getCurrentUserId())
            .stream().map(this::toResponse).collect(Collectors.toList());
        return Result.success(tasks);
    }

    @GetMapping("/created")
    @Operation(summary = "Get tasks I created")
    @PreAuthorize("hasAnyAuthority('task:create', 'task:manage')")
    public Result<List<TaskResponse>> getCreatedTasks() {
        List<TaskResponse> tasks = taskService.getCreatedTasks(getCurrentUserId())
            .stream().map(this::toResponse).collect(Collectors.toList());
        return Result.success(tasks);
    }

    @GetMapping("/pending-approval")
    @Operation(summary = "Get tasks pending my approval")
    @PreAuthorize("hasAuthority('task:approve')")
    public Result<List<TaskResponse>> getPendingApprovalTasks() {
        List<TaskResponse> tasks = taskService.getPendingApprovalTasks(getCurrentUserId())
            .stream().map(this::toResponse).collect(Collectors.toList());
        return Result.success(tasks);
    }

    @PutMapping("/{id}/accept")
    @Operation(summary = "Accept a task assignment")
    @PreAuthorize("hasAuthority('task:execute')")
    public Result<TaskResponse> acceptTask(@PathVariable Long id) {
        Task task = taskService.acceptTask(id, getCurrentUserId(), getCurrentUserName());
        return Result.success(toResponse(task));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit task work")
    @PreAuthorize("hasAuthority('task:execute')")
    public Result<TaskResponse> submitTask(@PathVariable Long id,
            @Valid @RequestBody SubmitTaskRequest request) {
        SubmitTaskCommand command = SubmitTaskCommand.builder()
            .taskId(id)
            .content(request.getContent())
            .attachmentIds(request.getAttachmentIds())
            .submittedBy(getCurrentUserId())
            .submittedByName(getCurrentUserName())
            .build();

        taskService.submitTask(command);
        Task task = taskService.getTask(id).orElseThrow();
        return Result.success(toResponse(task));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve a task submission")
    @PreAuthorize("hasAuthority('task:approve')")
    public Result<TaskResponse> approveTask(@PathVariable Long id,
            @Valid @RequestBody ApproveRequest request) {
        ApproveTaskCommand command = ApproveTaskCommand.builder()
            .taskId(id)
            .assigneeUserId(request.getAssigneeUserId())
            .comment(request.getComment())
            .reviewerId(getCurrentUserId())
            .reviewerName(getCurrentUserName())
            .build();

        Task task = taskService.approveTask(command);
        return Result.success(toResponse(task));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject a task submission")
    @PreAuthorize("hasAuthority('task:approve')")
    public Result<TaskResponse> rejectTask(@PathVariable Long id,
            @Valid @RequestBody RejectRequest request) {
        RejectTaskCommand command = RejectTaskCommand.builder()
            .taskId(id)
            .assigneeUserId(request.getAssigneeUserId())
            .reason(request.getReason())
            .reviewerId(getCurrentUserId())
            .reviewerName(getCurrentUserName())
            .build();

        Task task = taskService.rejectTask(command);
        return Result.success(toResponse(task));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel a task")
    @PreAuthorize("hasAuthority('task:manage')")
    public Result<TaskResponse> cancelTask(@PathVariable Long id,
            @RequestParam(required = false) String reason) {
        Task task = taskService.cancelTask(id, reason, getCurrentUserId());
        return Result.success(toResponse(task));
    }

    private Long getCurrentUserId() {
        return jwtTokenService.getCurrentUserId();
    }

    private String getCurrentUserName() {
        return jwtTokenService.getCurrentUserName();
    }

    private TaskResponse toResponse(Task task) {
        TaskResponse r = new TaskResponse();
        r.setId(task.getId());
        r.setTaskCode(task.getTaskCode());
        r.setTitle(task.getTitle());
        r.setDescription(task.getDescription());
        r.setPriority(task.getPriority());
        r.setStatus(task.getStatus());
        r.setAssignerId(task.getAssignerId());
        r.setAssignerName(task.getAssignerName());
        r.setAssignmentType(task.getAssignmentType());
        r.setDepartmentId(task.getDepartmentId());
        r.setDueDate(task.getDueDate());
        r.setAcceptedAt(task.getAcceptedAt());
        r.setSubmittedAt(task.getSubmittedAt());
        r.setCompletedAt(task.getCompletedAt());
        r.setAssigneeCount(task.getAssigneeCount());
        r.setCompletedAssigneeCount(task.getCompletedAssigneeCount());
        r.setCompletionPercentage(task.getCompletionPercentage());
        r.setOverdue(task.isOverdue());
        r.setCreatedAt(task.getCreatedAt());
        r.setAllowedTransitions(task.getStatus().getAllowedTransitions().stream()
            .map(TaskStatus::name).collect(Collectors.toList()));
        if (task.getAssignees() != null) {
            r.setAssignees(task.getAssignees().stream()
                .map(this::toAssigneeResponse).collect(Collectors.toList()));
        }
        return r;
    }

    private AssigneeResponse toAssigneeResponse(Assignee a) {
        AssigneeResponse r = new AssigneeResponse();
        r.setId(a.getId());
        r.setUserId(a.getUserId());
        r.setUserName(a.getUserName());
        r.setDepartmentId(a.getDepartmentId());
        r.setDepartmentName(a.getDepartmentName());
        r.setStatus(a.getStatus());
        r.setAcceptedAt(a.getAcceptedAt());
        r.setSubmittedAt(a.getSubmittedAt());
        r.setCompletedAt(a.getCompletedAt());
        return r;
    }
}
