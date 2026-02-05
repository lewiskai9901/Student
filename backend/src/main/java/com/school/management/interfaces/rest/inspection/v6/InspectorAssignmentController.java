package com.school.management.interfaces.rest.inspection.v6;

import com.school.management.application.inspection.v6.InspectorAssignmentService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.persistence.inspection.v6.ProjectInspectorConfigPO;
import com.school.management.infrastructure.persistence.inspection.v6.TaskInspectorAssignmentPO;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * V6检查员分配控制器
 */
@Tag(name = "V6检查员分配", description = "V6检查员分配管理接口")
@RestController
@RequestMapping("/v6/inspector-assignments")
public class InspectorAssignmentController {

    private final InspectorAssignmentService assignmentService;

    public InspectorAssignmentController(InspectorAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    // ========== 项目检查员配置 ==========

    @Operation(summary = "获取项目检查员配置")
    @GetMapping("/projects/{projectId}/inspectors")
    @PreAuthorize("hasAuthority('inspection:project:view')")
    public Result<List<InspectorConfigResponse>> getProjectInspectors(@PathVariable Long projectId) {
        List<ProjectInspectorConfigPO> configs = assignmentService.getProjectInspectors(projectId);
        return Result.success(configs.stream().map(this::toConfigResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "添加项目检查员")
    @PostMapping("/projects/{projectId}/inspectors")
    @PreAuthorize("hasAuthority('inspection:project:update')")
    public Result<InspectorConfigResponse> addProjectInspector(
            @PathVariable Long projectId,
            @RequestBody AddInspectorRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ProjectInspectorConfigPO config = assignmentService.addProjectInspector(
                projectId, request.getInspectorId(), request.getInspectorName(),
                request.isDefault(), request.getScopeType(), request.getScopeIds(),
                userDetails.getUserId()
        );
        return Result.success(toConfigResponse(config));
    }

    @Operation(summary = "批量添加项目检查员")
    @PostMapping("/projects/{projectId}/inspectors/batch")
    @PreAuthorize("hasAuthority('inspection:project:update')")
    public Result<Void> addProjectInspectors(
            @PathVariable Long projectId,
            @RequestBody List<InspectorAssignmentService.InspectorConfig> inspectors,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        assignmentService.addProjectInspectors(projectId, inspectors, userDetails.getUserId());
        return Result.success();
    }

    @Operation(summary = "移除项目检查员")
    @DeleteMapping("/projects/{projectId}/inspectors/{inspectorId}")
    @PreAuthorize("hasAuthority('inspection:project:update')")
    public Result<Void> removeProjectInspector(
            @PathVariable Long projectId,
            @PathVariable Long inspectorId) {

        assignmentService.removeProjectInspector(projectId, inspectorId);
        return Result.success();
    }

    // ========== 任务检查员分配 ==========

    @Operation(summary = "获取任务检查员分配")
    @GetMapping("/tasks/{taskId}/inspectors")
    @PreAuthorize("hasAuthority('inspection:task:view')")
    public Result<List<AssignmentResponse>> getTaskAssignments(@PathVariable Long taskId) {
        List<TaskInspectorAssignmentPO> assignments = assignmentService.getTaskAssignments(taskId);
        return Result.success(assignments.stream().map(this::toAssignmentResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "分配检查员到任务")
    @PostMapping("/tasks/{taskId}/inspectors")
    @PreAuthorize("hasAuthority('inspection:task:assign')")
    public Result<AssignmentResponse> assignInspector(
            @PathVariable Long taskId,
            @RequestBody AssignInspectorRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TaskInspectorAssignmentPO assignment = assignmentService.assignInspector(
                taskId, request.getInspectorId(), request.getInspectorName(),
                request.getScopeType(), request.getScopeIds(), userDetails.getUserId()
        );
        return Result.success(toAssignmentResponse(assignment));
    }

    @Operation(summary = "批量分配检查员")
    @PostMapping("/tasks/{taskId}/inspectors/batch")
    @PreAuthorize("hasAuthority('inspection:task:assign')")
    public Result<Void> assignInspectors(
            @PathVariable Long taskId,
            @RequestBody List<InspectorAssignmentService.InspectorAssignment> assignments,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        assignmentService.assignInspectors(taskId, assignments, userDetails.getUserId());
        return Result.success();
    }

    @Operation(summary = "接受任务分配")
    @PostMapping("/assignments/{id}/accept")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<Void> acceptAssignment(@PathVariable Long id) {
        assignmentService.acceptAssignment(id);
        return Result.success();
    }

    @Operation(summary = "拒绝任务分配")
    @PostMapping("/assignments/{id}/decline")
    @PreAuthorize("hasAuthority('inspection:task:execute')")
    public Result<Void> declineAssignment(@PathVariable Long id) {
        assignmentService.declineAssignment(id);
        return Result.success();
    }

    @Operation(summary = "自动分配检查员")
    @PostMapping("/tasks/{taskId}/auto-assign")
    @PreAuthorize("hasAuthority('inspection:task:assign')")
    public Result<Void> autoAssignInspectors(
            @PathVariable Long taskId,
            @RequestParam Long projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        assignmentService.autoAssignInspectors(taskId, projectId, userDetails.getUserId());
        return Result.success();
    }

    // ========== 转换方法 ==========

    private InspectorConfigResponse toConfigResponse(ProjectInspectorConfigPO po) {
        InspectorConfigResponse response = new InspectorConfigResponse();
        response.setId(po.getId());
        response.setProjectId(po.getProjectId());
        response.setInspectorId(po.getInspectorId());
        response.setInspectorName(po.getInspectorName());
        response.setDefault(po.getIsDefault() != null && po.getIsDefault());
        response.setScopeType(po.getScopeType());
        response.setScopeIds(po.getScopeIds());
        response.setCreatedAt(po.getCreatedAt());
        return response;
    }

    private AssignmentResponse toAssignmentResponse(TaskInspectorAssignmentPO po) {
        AssignmentResponse response = new AssignmentResponse();
        response.setId(po.getId());
        response.setTaskId(po.getTaskId());
        response.setInspectorId(po.getInspectorId());
        response.setInspectorName(po.getInspectorName());
        response.setScopeType(po.getScopeType());
        response.setScopeIds(po.getScopeIds());
        response.setStatus(po.getStatus());
        response.setAcceptedAt(po.getAcceptedAt());
        return response;
    }

    // ========== DTO类 ==========

    public static class AddInspectorRequest {
        private Long inspectorId;
        private String inspectorName;
        private boolean isDefault;
        private String scopeType;
        private String scopeIds;

        public Long getInspectorId() { return inspectorId; }
        public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
        public String getInspectorName() { return inspectorName; }
        public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }
        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean aDefault) { isDefault = aDefault; }
        public String getScopeType() { return scopeType; }
        public void setScopeType(String scopeType) { this.scopeType = scopeType; }
        public String getScopeIds() { return scopeIds; }
        public void setScopeIds(String scopeIds) { this.scopeIds = scopeIds; }
    }

    public static class AssignInspectorRequest {
        private Long inspectorId;
        private String inspectorName;
        private String scopeType;
        private String scopeIds;

        public Long getInspectorId() { return inspectorId; }
        public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
        public String getInspectorName() { return inspectorName; }
        public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }
        public String getScopeType() { return scopeType; }
        public void setScopeType(String scopeType) { this.scopeType = scopeType; }
        public String getScopeIds() { return scopeIds; }
        public void setScopeIds(String scopeIds) { this.scopeIds = scopeIds; }
    }

    public static class InspectorConfigResponse {
        private Long id;
        private Long projectId;
        private Long inspectorId;
        private String inspectorName;
        private boolean isDefault;
        private String scopeType;
        private String scopeIds;
        private java.time.LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public Long getInspectorId() { return inspectorId; }
        public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
        public String getInspectorName() { return inspectorName; }
        public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }
        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean aDefault) { isDefault = aDefault; }
        public String getScopeType() { return scopeType; }
        public void setScopeType(String scopeType) { this.scopeType = scopeType; }
        public String getScopeIds() { return scopeIds; }
        public void setScopeIds(String scopeIds) { this.scopeIds = scopeIds; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    public static class AssignmentResponse {
        private Long id;
        private Long taskId;
        private Long inspectorId;
        private String inspectorName;
        private String scopeType;
        private String scopeIds;
        private String status;
        private java.time.LocalDateTime acceptedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        public Long getInspectorId() { return inspectorId; }
        public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
        public String getInspectorName() { return inspectorName; }
        public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }
        public String getScopeType() { return scopeType; }
        public void setScopeType(String scopeType) { this.scopeType = scopeType; }
        public String getScopeIds() { return scopeIds; }
        public void setScopeIds(String scopeIds) { this.scopeIds = scopeIds; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public java.time.LocalDateTime getAcceptedAt() { return acceptedAt; }
        public void setAcceptedAt(java.time.LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    }
}
