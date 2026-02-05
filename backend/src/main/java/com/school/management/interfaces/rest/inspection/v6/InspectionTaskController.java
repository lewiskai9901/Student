package com.school.management.interfaces.rest.inspection.v6;

import com.school.management.application.inspection.v6.InspectionTaskApplicationService;
import com.school.management.application.inspection.v6.TaskGenerationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v6.*;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * V6检查任务控制器
 */
@Tag(name = "V6检查任务", description = "V6检查任务管理接口")
@RestController
@RequestMapping("/v6/inspection-tasks")
public class InspectionTaskController {

    private final InspectionTaskApplicationService taskService;
    private final TaskGenerationService taskGenerationService;

    public InspectionTaskController(InspectionTaskApplicationService taskService,
                                     TaskGenerationService taskGenerationService) {
        this.taskService = taskService;
        this.taskGenerationService = taskGenerationService;
    }

    @Operation(summary = "手动生成任务")
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('inspection:task:generate')")
    public Result<List<TaskResponse>> generateTasks(
            @RequestParam Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<InspectionTask> tasks = taskGenerationService.generateTasksForProject(projectId, date);
        return Result.success(tasks.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "批量生成任务")
    @PostMapping("/generate-batch")
    @PreAuthorize("hasAuthority('inspection:task:generate')")
    public Result<List<TaskResponse>> generateTasksBatch(
            @RequestParam Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<InspectionTask> tasks = taskGenerationService.generateTasksForDateRange(projectId, startDate, endDate);
        return Result.success(tasks.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inspection:task:view')")
    public Result<TaskResponse> getTask(@PathVariable Long id) {
        return taskService.getTask(id)
                .map(t -> Result.success(toResponse(t)))
                .orElse(Result.error(404, "资源不存在"));
    }

    @Operation(summary = "分页查询任务")
    @GetMapping
    @PreAuthorize("hasAuthority('inspection:task:view')")
    public Result<Map<String, Object>> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long inspectorId) {

        TaskStatus taskStatus = status != null ? TaskStatus.fromCode(status) : null;
        List<InspectionTask> tasks = taskService.listTasks(page, size, projectId, taskStatus, startDate, endDate, inspectorId);
        long total = taskService.countTasks(projectId, taskStatus, startDate, endDate, inspectorId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", tasks.stream().map(this::toResponse).collect(Collectors.toList()));
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    @Operation(summary = "获取可领取的任务")
    @GetMapping("/available")
    @PreAuthorize("hasAuthority('inspection:task:view')")
    public Result<List<TaskResponse>> getAvailableTasks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<InspectionTask> tasks = taskService.getAvailableTasks(targetDate);
        return Result.success(tasks.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "获取我的任务")
    @GetMapping("/my-tasks")
    @PreAuthorize("hasAuthority('inspection:task:view')")
    public Result<List<TaskResponse>> getMyTasks(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<InspectionTask> tasks = taskService.getMyTasks(userDetails.getUserId());
        return Result.success(tasks.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "领取任务")
    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<TaskResponse> claimTask(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        InspectionTask task = taskService.claimTask(id, userDetails.getUserId(), userDetails.getRealName());
        return Result.success(toResponse(task));
    }

    @Operation(summary = "开始任务")
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<TaskResponse> startTask(@PathVariable Long id) {
        InspectionTask task = taskService.startTask(id);
        return Result.success(toResponse(task));
    }

    @Operation(summary = "提交任务")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<TaskResponse> submitTask(@PathVariable Long id) {
        InspectionTask task = taskService.submitTask(id);
        return Result.success(toResponse(task));
    }

    @Operation(summary = "审核任务")
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('inspection:task:review')")
    public Result<TaskResponse> reviewTask(@PathVariable Long id) {
        InspectionTask task = taskService.reviewTask(id);
        return Result.success(toResponse(task));
    }

    @Operation(summary = "发布任务")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('inspection:task:publish')")
    public Result<TaskResponse> publishTask(@PathVariable Long id) {
        InspectionTask task = taskService.publishTask(id);
        return Result.success(toResponse(task));
    }

    @Operation(summary = "取消任务")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('inspection:task:cancel')")
    public Result<TaskResponse> cancelTask(@PathVariable Long id) {
        InspectionTask task = taskService.cancelTask(id);
        return Result.success(toResponse(task));
    }

    // ========== 检查目标相关 ==========

    @Operation(summary = "获取任务的检查目标")
    @GetMapping("/{id}/targets")
    @PreAuthorize("hasAuthority('inspection:task:view')")
    public Result<List<TargetResponse>> getTargets(@PathVariable Long id) {
        List<InspectionTarget> targets = taskService.getTargetsByTask(id);
        return Result.success(targets.stream().map(this::toTargetResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "锁定目标")
    @PostMapping("/targets/{targetId}/lock")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<TargetResponse> lockTarget(
            @PathVariable Long targetId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        InspectionTarget target = taskService.lockTarget(targetId, userDetails.getUserId());
        return Result.success(toTargetResponse(target));
    }

    @Operation(summary = "解锁目标")
    @PostMapping("/targets/{targetId}/unlock")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<TargetResponse> unlockTarget(@PathVariable Long targetId) {
        InspectionTarget target = taskService.unlockTarget(targetId);
        return Result.success(toTargetResponse(target));
    }

    @Operation(summary = "完成目标检查")
    @PostMapping("/targets/{targetId}/complete")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<TargetResponse> completeTarget(@PathVariable Long targetId) {
        InspectionTarget target = taskService.completeTarget(targetId);
        return Result.success(toTargetResponse(target));
    }

    @Operation(summary = "跳过目标")
    @PostMapping("/targets/{targetId}/skip")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<TargetResponse> skipTarget(
            @PathVariable Long targetId,
            @RequestBody SkipTargetRequest request) {

        InspectionTarget target = taskService.skipTarget(targetId, request.getReason());
        return Result.success(toTargetResponse(target));
    }

    @Operation(summary = "添加扣分")
    @PostMapping("/targets/{targetId}/deductions")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<Void> addDeduction(
            @PathVariable Long targetId,
            @RequestBody AddDeductionRequest request) {

        taskService.addDeduction(targetId, request.getDeduction());
        return Result.success();
    }

    @Operation(summary = "添加加分")
    @PostMapping("/targets/{targetId}/bonuses")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<Void> addBonus(
            @PathVariable Long targetId,
            @RequestBody AddBonusRequest request) {

        taskService.addBonus(targetId, request.getBonus());
        return Result.success();
    }

    // ========== 转换方法 ==========

    private TaskResponse toResponse(InspectionTask task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTaskCode(task.getTaskCode());
        response.setProjectId(task.getProjectId());
        response.setTaskDate(task.getTaskDate());
        response.setTimeSlot(task.getTimeSlot());
        response.setStatus(task.getStatus() != null ? task.getStatus().getCode() : null);
        response.setStatusLabel(task.getStatus() != null ? task.getStatus().getLabel() : null);
        response.setInspectorId(task.getInspectorId());
        response.setInspectorName(task.getInspectorName());
        response.setClaimedAt(task.getClaimedAt());
        response.setStartedAt(task.getStartedAt());
        response.setSubmittedAt(task.getSubmittedAt());
        response.setPublishedAt(task.getPublishedAt());
        response.setTotalTargets(task.getTotalTargets());
        response.setCompletedTargets(task.getCompletedTargets());
        response.setSkippedTargets(task.getSkippedTargets());
        response.setCreatedAt(task.getCreatedAt());
        return response;
    }

    private TargetResponse toTargetResponse(InspectionTarget target) {
        TargetResponse response = new TargetResponse();
        response.setId(target.getId());
        response.setTaskId(target.getTaskId());
        response.setTargetType(target.getTargetType() != null ? target.getTargetType().getCode() : null);
        response.setTargetId(target.getTargetId());
        response.setTargetName(target.getTargetName());
        response.setTargetCode(target.getTargetCode());
        response.setOrgUnitId(target.getOrgUnitId());
        response.setOrgUnitName(target.getOrgUnitName());
        response.setClassId(target.getClassId());
        response.setClassName(target.getClassName());
        response.setWeightRatio(target.getWeightRatio());
        response.setStatus(target.getStatus() != null ? target.getStatus().getCode() : null);
        response.setStatusLabel(target.getStatus() != null ? target.getStatus().getLabel() : null);
        response.setBaseScore(target.getBaseScore());
        response.setFinalScore(target.getFinalScore());
        response.setDeductionTotal(target.getDeductionTotal());
        response.setBonusTotal(target.getBonusTotal());
        return response;
    }

    // ========== DTO类 ==========

    public static class TaskResponse {
        private Long id;
        private String taskCode;
        private Long projectId;
        private LocalDate taskDate;
        private String timeSlot;
        private String status;
        private String statusLabel;
        private Long inspectorId;
        private String inspectorName;
        private java.time.LocalDateTime claimedAt;
        private java.time.LocalDateTime startedAt;
        private java.time.LocalDateTime submittedAt;
        private java.time.LocalDateTime publishedAt;
        private Integer totalTargets;
        private Integer completedTargets;
        private Integer skippedTargets;
        private java.time.LocalDateTime createdAt;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTaskCode() { return taskCode; }
        public void setTaskCode(String taskCode) { this.taskCode = taskCode; }
        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public LocalDate getTaskDate() { return taskDate; }
        public void setTaskDate(LocalDate taskDate) { this.taskDate = taskDate; }
        public String getTimeSlot() { return timeSlot; }
        public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusLabel() { return statusLabel; }
        public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
        public Long getInspectorId() { return inspectorId; }
        public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
        public String getInspectorName() { return inspectorName; }
        public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }
        public java.time.LocalDateTime getClaimedAt() { return claimedAt; }
        public void setClaimedAt(java.time.LocalDateTime claimedAt) { this.claimedAt = claimedAt; }
        public java.time.LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(java.time.LocalDateTime startedAt) { this.startedAt = startedAt; }
        public java.time.LocalDateTime getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(java.time.LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
        public java.time.LocalDateTime getPublishedAt() { return publishedAt; }
        public void setPublishedAt(java.time.LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
        public Integer getTotalTargets() { return totalTargets; }
        public void setTotalTargets(Integer totalTargets) { this.totalTargets = totalTargets; }
        public Integer getCompletedTargets() { return completedTargets; }
        public void setCompletedTargets(Integer completedTargets) { this.completedTargets = completedTargets; }
        public Integer getSkippedTargets() { return skippedTargets; }
        public void setSkippedTargets(Integer skippedTargets) { this.skippedTargets = skippedTargets; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class TargetResponse {
        private Long id;
        private Long taskId;
        private String targetType;
        private Long targetId;
        private String targetName;
        private String targetCode;
        private Long orgUnitId;
        private String orgUnitName;
        private Long classId;
        private String className;
        private BigDecimal weightRatio;
        private String status;
        private String statusLabel;
        private BigDecimal baseScore;
        private BigDecimal finalScore;
        private BigDecimal deductionTotal;
        private BigDecimal bonusTotal;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public String getTargetType() { return targetType; }
        public void setTargetType(String targetType) { this.targetType = targetType; }
        public Long getTargetId() { return targetId; }
        public void setTargetId(Long targetId) { this.targetId = targetId; }
        public String getTargetName() { return targetName; }
        public void setTargetName(String targetName) { this.targetName = targetName; }
        public String getTargetCode() { return targetCode; }
        public void setTargetCode(String targetCode) { this.targetCode = targetCode; }
        public Long getOrgUnitId() { return orgUnitId; }
        public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
        public String getOrgUnitName() { return orgUnitName; }
        public void setOrgUnitName(String orgUnitName) { this.orgUnitName = orgUnitName; }
        public Long getClassId() { return classId; }
        public void setClassId(Long classId) { this.classId = classId; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public BigDecimal getWeightRatio() { return weightRatio; }
        public void setWeightRatio(BigDecimal weightRatio) { this.weightRatio = weightRatio; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusLabel() { return statusLabel; }
        public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
        public BigDecimal getBaseScore() { return baseScore; }
        public void setBaseScore(BigDecimal baseScore) { this.baseScore = baseScore; }
        public BigDecimal getFinalScore() { return finalScore; }
        public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }
        public BigDecimal getDeductionTotal() { return deductionTotal; }
        public void setDeductionTotal(BigDecimal deductionTotal) { this.deductionTotal = deductionTotal; }
        public BigDecimal getBonusTotal() { return bonusTotal; }
        public void setBonusTotal(BigDecimal bonusTotal) { this.bonusTotal = bonusTotal; }
    }

    public static class SkipTargetRequest {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class AddDeductionRequest {
        private BigDecimal deduction;
        public BigDecimal getDeduction() { return deduction; }
        public void setDeduction(BigDecimal deduction) { this.deduction = deduction; }
    }

    public static class AddBonusRequest {
        private BigDecimal bonus;
        public BigDecimal getBonus() { return bonus; }
        public void setBonus(BigDecimal bonus) { this.bonus = bonus; }
    }
}
