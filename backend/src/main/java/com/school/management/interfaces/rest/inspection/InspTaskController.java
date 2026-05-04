package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.InspTaskApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.execution.InspTask;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/inspection/tasks")
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

    /** V108: 检查员发起临时抽查任务 (AD_HOC) — 项目须 allow_ad_hoc=true */
    @PostMapping("/ad-hoc")
    @CasbinAccess(resource = "insp:task", action = "execute")
    public Result<InspTask> createAdHoc(@RequestBody AdHocRequest request) {
        Long me = com.school.management.common.util.SecurityUtils.getCurrentUserId();
        String myName = com.school.management.common.util.SecurityUtils.getCurrentUsername();
        InspTask task = taskService.createAdHocTask(request.getProjectId(),
                me, myName, request.getReason());
        return Result.success(task);
    }

    /** V108: 列出允许抽查的项目 (前端发起对话框用) */
    @GetMapping("/ad-hoc/allowed-projects")
    @CasbinAccess(resource = "insp:task", action = "view")
    public Result<List<java.util.Map<String, Object>>> listAdHocAllowedProjects() {
        return Result.success(taskService.listAdHocAllowedProjects());
    }

    /** V108: 治理面板 KPI 按 task_type 拆分 (5 维度) */
    @GetMapping("/kpi/by-type")
    @CasbinAccess(resource = "insp:analytics", action = "view")
    public Result<java.util.Map<String, Object>> getTaskTypeKpi(
            @RequestParam(required = false) Long projectId) {
        return Result.success(taskService.getTaskTypeKpi(projectId));
    }

    /** V108: 受检主体发起自查 (SELF_CHECK) — 项目须 allow_self_check=1 */
    @PostMapping("/self-check")
    @CasbinAccess(resource = "insp:task", action = "execute")
    public Result<InspTask> createSelfCheck(@RequestBody SelfCheckRequest request) {
        Long me = com.school.management.common.util.SecurityUtils.getCurrentUserId();
        String myName = com.school.management.common.util.SecurityUtils.getCurrentUsername();
        return Result.success(taskService.createSelfCheckTask(
                request.getProjectId(), me, myName, request.getReason()));
    }

    /** V108: 检查员发起互查 (CROSS_AUDIT) — 必填 dueDate */
    @PostMapping("/cross-audit")
    @CasbinAccess(resource = "insp:task", action = "execute")
    public Result<InspTask> createCrossAudit(@RequestBody CrossAuditRequest request) {
        Long me = com.school.management.common.util.SecurityUtils.getCurrentUserId();
        String myName = com.school.management.common.util.SecurityUtils.getCurrentUsername();
        return Result.success(taskService.createCrossAuditTask(
                request.getProjectId(), me, myName, request.getDueDate(), request.getReason()));
    }

    public static class SelfCheckRequest {
        private Long projectId;
        private String reason;
        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class CrossAuditRequest {
        private Long projectId;
        private java.time.LocalDate dueDate;
        private String reason;
        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public java.time.LocalDate getDueDate() { return dueDate; }
        public void setDueDate(java.time.LocalDate dueDate) { this.dueDate = dueDate; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    /** V108: 单独读项目检查模式配置 */
    @GetMapping("/projects/{id}/inspection-mode")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<java.util.Map<String, Object>> getInspectionMode(@PathVariable Long id) {
        return Result.success(taskService.getInspectionMode(id));
    }

    /** V108: 单独写项目检查模式配置 */
    @PutMapping("/projects/{id}/inspection-mode")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<java.util.Map<String, Object>> updateInspectionMode(
            @PathVariable Long id, @RequestBody InspectionModeRequest request) {
        return Result.success(taskService.updateInspectionMode(id, request));
    }

    public static class InspectionModeRequest {
        private String inspectionMode;
        private Boolean allowAdHoc;
        private Boolean allowSelfCheck;
        private Integer adHocQuotaPerInspector;
        public String getInspectionMode() { return inspectionMode; }
        public void setInspectionMode(String inspectionMode) { this.inspectionMode = inspectionMode; }
        public Boolean getAllowAdHoc() { return allowAdHoc; }
        public void setAllowAdHoc(Boolean allowAdHoc) { this.allowAdHoc = allowAdHoc; }
        public Boolean getAllowSelfCheck() { return allowSelfCheck; }
        public void setAllowSelfCheck(Boolean allowSelfCheck) { this.allowSelfCheck = allowSelfCheck; }
        public Integer getAdHocQuotaPerInspector() { return adHocQuotaPerInspector; }
        public void setAdHocQuotaPerInspector(Integer v) { this.adHocQuotaPerInspector = v; }
    }

    public static class AdHocRequest {
        private Long projectId;
        private String reason;
        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
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

    /**
     * 项目管理员手动延期任务 (P1#5).
     * 用于驳回上限达到后或其他业务原因主动延长 deadline.
     */
    @PostMapping("/{id}/extend-deadline")
    @CasbinAccess(resource = "insp:task", action = "edit")
    public Result<InspTask> extendDeadline(@PathVariable Long id,
                                            @RequestBody ExtendDeadlineRequest request) {
        return Result.success(taskService.extendTaskDeadline(id, request.getNewDeadline()));
    }

    /** review #D: 检查员离职 / 退出时批量解除其所有非终态任务 */
    @PostMapping("/reassign-departed-inspector/{userId}")
    @CasbinAccess(resource = "insp:task", action = "edit")
    public Result<Integer> reassignDepartedInspector(@PathVariable Long userId,
                                                       @RequestBody ReassignDepartedInspectorRequest request) {
        int affected = taskService.reassignDepartedInspector(userId,
                request.getReason(),
                request.getFallbackInspectorId(),
                request.getFallbackInspectorName());
        return Result.success(affected);
    }

    @lombok.Data
    public static class ReassignDepartedInspectorRequest {
        private String reason;
        private Long fallbackInspectorId;
        private String fallbackInspectorName;
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

    @lombok.Data
    public static class ExtendDeadlineRequest {
        private LocalDate newDeadline;
    }
}
