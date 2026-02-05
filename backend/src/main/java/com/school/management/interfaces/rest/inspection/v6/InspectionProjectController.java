package com.school.management.interfaces.rest.inspection.v6;

import com.school.management.application.inspection.v6.InspectionProjectApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v6.*;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * V6检查项目控制器
 */
@Tag(name = "V6检查项目", description = "V6检查项目管理接口")
@RestController
@RequestMapping("/v6/inspection-projects")
public class InspectionProjectController {

    private final InspectionProjectApplicationService projectService;

    public InspectionProjectController(InspectionProjectApplicationService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "创建项目")
    @PostMapping
    @PreAuthorize("hasAuthority('inspection:project:create')")
    public Result<ProjectResponse> createProject(
            @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        var command = new InspectionProjectApplicationService.CreateProjectCommand();
        command.setProjectName(request.getProjectName());
        command.setDescription(request.getDescription());
        command.setTemplateId(request.getTemplateId());
        command.setCreatedBy(userDetails.getUserId());

        InspectionProject project = projectService.createProject(command);
        return Result.success(toResponse(project));
    }

    @Operation(summary = "更新项目配置")
    @PutMapping("/{id}/config")
    @PreAuthorize("hasAuthority('inspection:project:update')")
    public Result<ProjectResponse> updateProjectConfig(
            @PathVariable Long id,
            @RequestBody UpdateProjectConfigRequest request) {

        var command = new InspectionProjectApplicationService.UpdateProjectConfigCommand();
        command.setScopeType(request.getScopeType() != null ? ScopeType.fromCode(request.getScopeType()) : null);
        command.setScopeConfig(request.getScopeConfig());
        command.setStartDate(request.getStartDate());
        command.setEndDate(request.getEndDate());
        command.setCycleType(request.getCycleType() != null ? CycleType.fromCode(request.getCycleType()) : null);
        command.setCycleConfig(request.getCycleConfig());
        command.setTimeSlots(request.getTimeSlots());
        command.setSkipHolidays(request.isSkipHolidays());
        command.setSharedSpaceStrategy(request.getSharedSpaceStrategy() != null ?
                SharedSpaceStrategy.fromCode(request.getSharedSpaceStrategy()) : null);
        command.setInspectorAssignmentMode(request.getInspectorAssignmentMode() != null ?
                InspectorAssignmentMode.fromCode(request.getInspectorAssignmentMode()) : null);

        InspectionProject project = projectService.updateProjectConfig(id, command);
        return Result.success(toResponse(project));
    }

    @Operation(summary = "发布项目")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('inspection:project:publish')")
    public Result<ProjectResponse> publishProject(
            @PathVariable Long id,
            @RequestBody(required = false) PublishProjectRequest request) {

        String templateSnapshot = request != null ? request.getTemplateSnapshot() : null;
        InspectionProject project = projectService.publishProject(id, templateSnapshot);
        return Result.success(toResponse(project));
    }

    @Operation(summary = "暂停项目")
    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('inspection:project:update')")
    public Result<ProjectResponse> pauseProject(@PathVariable Long id) {
        InspectionProject project = projectService.pauseProject(id);
        return Result.success(toResponse(project));
    }

    @Operation(summary = "恢复项目")
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('inspection:project:update')")
    public Result<ProjectResponse> resumeProject(@PathVariable Long id) {
        InspectionProject project = projectService.resumeProject(id);
        return Result.success(toResponse(project));
    }

    @Operation(summary = "完成项目")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('inspection:project:update')")
    public Result<ProjectResponse> completeProject(@PathVariable Long id) {
        InspectionProject project = projectService.completeProject(id);
        return Result.success(toResponse(project));
    }

    @Operation(summary = "归档项目")
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('inspection:project:update')")
    public Result<ProjectResponse> archiveProject(@PathVariable Long id) {
        InspectionProject project = projectService.archiveProject(id);
        return Result.success(toResponse(project));
    }

    @Operation(summary = "获取项目详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inspection:project:view')")
    public Result<ProjectResponse> getProject(@PathVariable Long id) {
        return projectService.getProject(id)
                .map(p -> Result.success(toResponse(p)))
                .orElse(Result.error(404, "项目不存在"));
    }

    @Operation(summary = "分页查询项目")
    @GetMapping
    @PreAuthorize("hasAuthority('inspection:project:view')")
    public Result<Map<String, Object>> listProjects(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        ProjectStatus projectStatus = status != null ? ProjectStatus.fromCode(status) : null;
        List<InspectionProject> projects = projectService.listProjects(page, size, projectStatus, keyword);
        long total = projectService.countProjects(projectStatus, keyword);

        Map<String, Object> result = new HashMap<>();
        result.put("list", projects.stream().map(this::toResponse).collect(Collectors.toList()));
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);

        return Result.success(result);
    }

    @Operation(summary = "删除项目")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inspection:project:delete')")
    public Result<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return Result.success();
    }

    @Operation(summary = "获取枚举选项")
    @GetMapping("/options")
    public Result<Map<String, Object>> getOptions() {
        Map<String, Object> options = new HashMap<>();

        options.put("scopeTypes", java.util.Arrays.stream(ScopeType.values())
                .map(e -> Map.of("code", e.getCode(), "label", e.getLabel(), "description", e.getDescription()))
                .collect(Collectors.toList()));

        options.put("cycleTypes", java.util.Arrays.stream(CycleType.values())
                .map(e -> Map.of("code", e.getCode(), "label", e.getLabel(), "description", e.getDescription()))
                .collect(Collectors.toList()));

        options.put("sharedSpaceStrategies", java.util.Arrays.stream(SharedSpaceStrategy.values())
                .map(e -> Map.of("code", e.getCode(), "label", e.getLabel(), "description", e.getDescription()))
                .collect(Collectors.toList()));

        options.put("inspectorAssignmentModes", java.util.Arrays.stream(InspectorAssignmentMode.values())
                .map(e -> Map.of("code", e.getCode(), "label", e.getLabel(), "description", e.getDescription()))
                .collect(Collectors.toList()));

        options.put("projectStatuses", java.util.Arrays.stream(ProjectStatus.values())
                .map(e -> Map.of("code", e.getCode(), "label", e.getLabel()))
                .collect(Collectors.toList()));

        return Result.success(options);
    }

    private ProjectResponse toResponse(InspectionProject project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setProjectCode(project.getProjectCode());
        response.setProjectName(project.getProjectName());
        response.setDescription(project.getDescription());
        response.setTemplateId(project.getTemplateId());
        response.setScopeType(project.getScopeType() != null ? project.getScopeType().getCode() : null);
        response.setScopeConfig(project.getScopeConfig());
        response.setStartDate(project.getStartDate());
        response.setEndDate(project.getEndDate());
        response.setCycleType(project.getCycleType() != null ? project.getCycleType().getCode() : null);
        response.setCycleConfig(project.getCycleConfig());
        response.setTimeSlots(project.getTimeSlots());
        response.setSkipHolidays(project.isSkipHolidays());
        response.setSharedSpaceStrategy(project.getSharedSpaceStrategy() != null ? project.getSharedSpaceStrategy().getCode() : null);
        response.setInspectorAssignmentMode(project.getInspectorAssignmentMode() != null ? project.getInspectorAssignmentMode().getCode() : null);
        response.setStatus(project.getStatus() != null ? project.getStatus().getCode() : null);
        response.setStatusLabel(project.getStatus() != null ? project.getStatus().getLabel() : null);
        response.setPublishedAt(project.getPublishedAt());
        response.setTotalTasks(project.getTotalTasks());
        response.setCompletedTasks(project.getCompletedTasks());
        response.setCreatedAt(project.getCreatedAt());
        return response;
    }

    // Request/Response DTOs
    public static class CreateProjectRequest {
        private String projectName;
        private String description;
        private Long templateId;

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getTemplateId() { return templateId; }
        public void setTemplateId(Long templateId) { this.templateId = templateId; }
    }

    public static class UpdateProjectConfigRequest {
        private String scopeType;
        private String scopeConfig;
        private LocalDate startDate;
        private LocalDate endDate;
        private String cycleType;
        private String cycleConfig;
        private String timeSlots;
        private boolean skipHolidays;
        private String sharedSpaceStrategy;
        private String inspectorAssignmentMode;

        public String getScopeType() { return scopeType; }
        public void setScopeType(String scopeType) { this.scopeType = scopeType; }
        public String getScopeConfig() { return scopeConfig; }
        public void setScopeConfig(String scopeConfig) { this.scopeConfig = scopeConfig; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getCycleType() { return cycleType; }
        public void setCycleType(String cycleType) { this.cycleType = cycleType; }
        public String getCycleConfig() { return cycleConfig; }
        public void setCycleConfig(String cycleConfig) { this.cycleConfig = cycleConfig; }
        public String getTimeSlots() { return timeSlots; }
        public void setTimeSlots(String timeSlots) { this.timeSlots = timeSlots; }
        public boolean isSkipHolidays() { return skipHolidays; }
        public void setSkipHolidays(boolean skipHolidays) { this.skipHolidays = skipHolidays; }
        public String getSharedSpaceStrategy() { return sharedSpaceStrategy; }
        public void setSharedSpaceStrategy(String sharedSpaceStrategy) { this.sharedSpaceStrategy = sharedSpaceStrategy; }
        public String getInspectorAssignmentMode() { return inspectorAssignmentMode; }
        public void setInspectorAssignmentMode(String inspectorAssignmentMode) { this.inspectorAssignmentMode = inspectorAssignmentMode; }
    }

    public static class PublishProjectRequest {
        private String templateSnapshot;

        public String getTemplateSnapshot() { return templateSnapshot; }
        public void setTemplateSnapshot(String templateSnapshot) { this.templateSnapshot = templateSnapshot; }
    }

    public static class ProjectResponse {
        private Long id;
        private String projectCode;
        private String projectName;
        private String description;
        private Long templateId;
        private String scopeType;
        private String scopeConfig;
        private LocalDate startDate;
        private LocalDate endDate;
        private String cycleType;
        private String cycleConfig;
        private String timeSlots;
        private boolean skipHolidays;
        private String sharedSpaceStrategy;
        private String inspectorAssignmentMode;
        private String status;
        private String statusLabel;
        private java.time.LocalDateTime publishedAt;
        private Integer totalTasks;
        private Integer completedTasks;
        private java.time.LocalDateTime createdAt;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getProjectCode() { return projectCode; }
        public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getTemplateId() { return templateId; }
        public void setTemplateId(Long templateId) { this.templateId = templateId; }
        public String getScopeType() { return scopeType; }
        public void setScopeType(String scopeType) { this.scopeType = scopeType; }
        public String getScopeConfig() { return scopeConfig; }
        public void setScopeConfig(String scopeConfig) { this.scopeConfig = scopeConfig; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getCycleType() { return cycleType; }
        public void setCycleType(String cycleType) { this.cycleType = cycleType; }
        public String getCycleConfig() { return cycleConfig; }
        public void setCycleConfig(String cycleConfig) { this.cycleConfig = cycleConfig; }
        public String getTimeSlots() { return timeSlots; }
        public void setTimeSlots(String timeSlots) { this.timeSlots = timeSlots; }
        public boolean isSkipHolidays() { return skipHolidays; }
        public void setSkipHolidays(boolean skipHolidays) { this.skipHolidays = skipHolidays; }
        public String getSharedSpaceStrategy() { return sharedSpaceStrategy; }
        public void setSharedSpaceStrategy(String sharedSpaceStrategy) { this.sharedSpaceStrategy = sharedSpaceStrategy; }
        public String getInspectorAssignmentMode() { return inspectorAssignmentMode; }
        public void setInspectorAssignmentMode(String inspectorAssignmentMode) { this.inspectorAssignmentMode = inspectorAssignmentMode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStatusLabel() { return statusLabel; }
        public void setStatusLabel(String statusLabel) { this.statusLabel = statusLabel; }
        public java.time.LocalDateTime getPublishedAt() { return publishedAt; }
        public void setPublishedAt(java.time.LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
        public Integer getTotalTasks() { return totalTasks; }
        public void setTotalTasks(Integer totalTasks) { this.totalTasks = totalTasks; }
        public Integer getCompletedTasks() { return completedTasks; }
        public void setCompletedTasks(Integer completedTasks) { this.completedTasks = completedTasks; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
