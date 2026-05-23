package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.InspProjectApplicationService;
import com.school.management.application.inspection.InspProjectAuthorizationGuard;
import com.school.management.application.inspection.InspTaskApplicationService;
import com.school.management.application.inspection.PeopleWorkbenchQueryService;
import com.school.management.application.inspection.dto.CloneProjectCommand;
import com.school.management.application.inspection.dto.PeopleWorkbenchView;
import com.school.management.application.inspection.dto.ProjectStatsSummary;
import com.school.management.application.inspection.ScoreAggregationService;
import com.school.management.application.inspection.TargetPopulationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.execution.*;
import com.school.management.infrastructure.casbin.CasbinAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 检查项目控制器.
 *
 * <p>评级引擎完美架构 (2026-05-23): DTO 撤销 defaultScoringProfileId — 评分方案
 * 不再由项目层兜底, 而是按 (project, section) 自动定位. /advanced-scoring 端点
 * 同步移除 — 项目层无法再单点指向一个 profile.
 */
@RestController
@RequestMapping("/inspection/projects")
@RequiredArgsConstructor
public class InspProjectController {

    private final InspProjectApplicationService projectService;
    private final TargetPopulationService targetPopulationService;
    private final ScoreAggregationService scoreAggregationService;
    private final InspProjectAuthorizationGuard authGuard;
    private final PeopleWorkbenchQueryService workbenchService;
    private final InspTaskApplicationService taskService;

    // ========== Project CRUD ==========

    @PostMapping
    @CasbinAccess(resource = "insp:project", action = "create")
    public Result<InspProject> createProject(@RequestBody @Valid CreateProjectRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        InspProject project = projectService.createProject(
                request.getProjectName(), request.getRootSectionId(),
                request.getStartDate(), request.getOrgUnitId(), userId);
        return Result.success(project);
    }

    @GetMapping
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<InspProject>> listProjects(
            @RequestParam(required = false) String status) {
        if (status != null) {
            try {
                return Result.success(projectService.listProjectsByStatus(
                        ProjectStatus.valueOf(status)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("无效的项目状态: " + status);
            }
        }
        return Result.success(projectService.listProjects());
    }

    @GetMapping("/with-stats")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<ProjectStatsSummary>> listProjectsWithStats(
            @RequestParam(required = false) String status) {
        ProjectStatus s = null;
        if (status != null && !status.isBlank()) {
            try { s = ProjectStatus.valueOf(status); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("无效的项目状态: " + status); }
        }
        return Result.success(projectService.listProjectsWithStats(s));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<InspProject> getProject(@PathVariable Long id) {
        return Result.success(projectService.getProject(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id)));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<InspProject> updateProject(@PathVariable Long id,
                                              @RequestBody @Valid UpdateProjectRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        authGuard.assertCanEditSettings(id, userId);
        InspProject project = projectService.updateProject(id,
                request.getProjectName(), request.getRootSectionId(),
                request.getScopeType(), request.getScopeConfig(),
                request.getStartDate(), request.getEndDate(),
                request.getAssignmentMode(), request.getReviewRequired(),
                request.getAutoPublish(), userId);
        return Result.success(project);
    }

    @PatchMapping("/{id}/config")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<InspProject> updateOperationalConfig(@PathVariable Long id,
                                                        @RequestBody @Valid OperationalConfigRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        authGuard.assertCanEditSettings(id, userId);
        return Result.success(projectService.updateOperationalConfig(id,
                request.getAssignmentMode(), request.getReviewRequired(),
                request.getAutoPublish(), request.getProjectName(), userId));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:project", action = "delete")
    public Result<Void> deleteProject(@PathVariable Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();
        authGuard.assertCanEditSettings(id, userId);
        projectService.deleteProject(id);
        return Result.success();
    }

    @PostMapping("/{id}/clone")
    @CasbinAccess(resource = "insp:project", action = "create")
    public Result<InspProject> cloneProject(@PathVariable Long id,
                                             @RequestBody @Valid CloneProjectRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        CloneProjectCommand command = new CloneProjectCommand(
                request.getProjectName(),
                request.getOrgUnitId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getCloneInspectors());
        return Result.success(projectService.cloneProject(id, command, userId));
    }

    // ========== Lifecycle ==========

    @PostMapping("/{id}/publish")
    @CasbinAccess(resource = "insp:project", action = "publish")
    public Result<InspProject> publishProject(@PathVariable Long id,
                                               @RequestBody @Valid PublishProjectRequest request) {
        return Result.success(projectService.publishProject(id, request.getTemplateVersionId()));
    }

    @PostMapping("/{id}/upgrade-template-version")
    @CasbinAccess(resource = "insp:project", action = "publish")
    public Result<InspProject> upgradeTemplateVersion(@PathVariable Long id) {
        return Result.success(projectService.upgradeTemplateVersion(id));
    }

    @GetMapping("/{id}/template-version-status")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<java.util.Map<String, Object>> getTemplateVersionStatus(@PathVariable Long id) {
        return Result.success(projectService.getTemplateVersionStatus(id));
    }

    @PutMapping("/{id}/policy")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<InspProject> updatePolicyConfig(@PathVariable Long id,
                                                    @RequestBody @Valid UpdatePolicyConfigRequest request) {
        Long userId = com.school.management.common.util.SecurityUtils.requireCurrentUserId();
        authGuard.assertCanEditSettings(id, userId);
        return Result.success(projectService.updatePolicyConfig(id,
                request.getMaxRejectCount(),
                request.getMaxEscalationLevel(),
                request.getAppealWindowDays(),
                userId));
    }

    @lombok.Data
    public static class UpdatePolicyConfigRequest {
        private Integer maxRejectCount;
        private Integer maxEscalationLevel;
        private Integer appealWindowDays;
    }

    @PostMapping("/{id}/pause")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<InspProject> pauseProject(@PathVariable Long id) {
        return Result.success(projectService.pauseProject(id));
    }

    @PostMapping("/{id}/resume")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<InspProject> resumeProject(@PathVariable Long id) {
        return Result.success(projectService.resumeProject(id));
    }

    @PostMapping("/{id}/complete")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<InspProject> completeProject(@PathVariable Long id) {
        return Result.success(projectService.completeProject(id));
    }

    @PostMapping("/{id}/archive")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<InspProject> archiveProject(@PathVariable Long id) {
        return Result.success(projectService.archiveProject(id));
    }

    // ========== Scores ==========

    @GetMapping("/{id}/scores")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<ProjectScore>> listProjectScores(@PathVariable Long id) {
        return Result.success(projectService.listProjectScores(id));
    }

    @PostMapping("/{id}/grade-score")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<ProjectScore> gradeScore(@PathVariable Long id,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cycleDate) {
        return Result.success(scoreAggregationService.gradeProjectScore(id, cycleDate));
    }

    // ========== Inspector Pool ==========

    @GetMapping("/{projectId}/inspectors")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<ProjectInspector>> listInspectors(@PathVariable Long projectId) {
        return Result.success(projectService.listInspectors(projectId));
    }

    @PostMapping("/{projectId}/inspectors")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<ProjectInspector> addInspector(@PathVariable Long projectId,
                                                  @RequestBody @Valid AddInspectorRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        authGuard.assertCanEditSettings(projectId, userId);
        return Result.success(projectService.addInspector(projectId,
                request.getUserId(), request.getUserName(), request.getRole()));
    }

    @DeleteMapping("/{projectId}/inspectors/{inspectorId}")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<Void> removeInspector(@PathVariable Long projectId,
                                         @PathVariable Long inspectorId) {
        Long userId = SecurityUtils.requireCurrentUserId();
        authGuard.assertCanEditSettings(projectId, userId);
        projectService.removeInspector(inspectorId);
        return Result.success();
    }

    // ========== Phase B: People Workbench + Role Matrix + Batch Assign ==========

    /**
     * Phase B - 人员工作台聚合接口.
     * 一次返回 inspectors + per-person stats + 4 段任务分组 + 项目级 summary,
     * 避免前端 N+1. 见 PeopleWorkbenchView.
     */
    @GetMapping("/{projectId}/people-workbench")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<PeopleWorkbenchView> getPeopleWorkbench(@PathVariable Long projectId) {
        return Result.success(workbenchService.getWorkbench(projectId));
    }

    /**
     * Phase B - 角色矩阵: 给某人加一个角色 (允许多角色多行).
     */
    @PostMapping("/{projectId}/inspectors/{userId}/roles/{role}")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<ProjectInspector> addInspectorRole(@PathVariable Long projectId,
                                                      @PathVariable Long userId,
                                                      @PathVariable InspectorRole role,
                                                      @RequestParam(required = false) String userName) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        authGuard.assertCanEditSettings(projectId, currentUserId);
        return Result.success(projectService.addInspectorRole(projectId, userId, userName, role));
    }

    /**
     * Phase B - 角色矩阵: 移除某人某角色.
     * LEAD 不变量守护: 若移除的是仅剩 1 LEAD 抛 409.
     */
    @DeleteMapping("/{projectId}/inspectors/{userId}/roles/{role}")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<Void> removeInspectorRole(@PathVariable Long projectId,
                                             @PathVariable Long userId,
                                             @PathVariable InspectorRole role) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        authGuard.assertCanEditSettings(projectId, currentUserId);
        projectService.removeInspectorRole(projectId, userId, role);
        return Result.success();
    }

    /**
     * Phase B - 批量指派: 把若干任务指派给同一个检查员.
     * Per-task 独立事务以支持 partial-success (沿用 reassignDepartedInspector 模式).
     */
    @PostMapping("/{projectId}/inspectors/batch-assign")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<BatchAssignResponse> batchAssignTasks(@PathVariable Long projectId,
                                                         @RequestBody @Valid BatchAssignRequest request) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        authGuard.assertCanEditSettings(projectId, currentUserId);
        int success = 0;
        int failure = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();
        for (Long taskId : request.getTaskIds()) {
            try {
                taskService.assignTask(taskId, request.getInspectorId(), request.getInspectorName());
                success++;
            } catch (Exception e) {
                failure++;
                errors.add("任务 " + taskId + ": " + e.getMessage());
            }
        }
        return Result.success(new BatchAssignResponse(success, failure, errors));
    }

    @lombok.Data
    public static class BatchAssignRequest {
        @NotNull
        private java.util.List<Long> taskIds;
        @NotNull
        private Long inspectorId;
        private String inspectorName;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class BatchAssignResponse {
        private int successCount;
        private int failureCount;
        private java.util.List<String> errors;
    }

    // ========== Target Preview ==========

    @PostMapping("/target-preview")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<Integer> previewTargetCount(@RequestBody @Valid TargetPreviewRequest request) {
        List<TargetPopulationService.TargetInfo> targets = targetPopulationService.resolveTargets(
                request.getScopeType(), request.getScopeConfig(), request.getTargetType());
        return Result.success(targets.size());
    }

    @GetMapping("/targets/persons")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<TargetPopulationService.PersonInfo>> getTargetPersons(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        return Result.success(targetPopulationService.resolvePersonsForTarget(targetType, targetId));
    }

    // --- Request DTOs ---

    @lombok.Data
    public static class CreateProjectRequest {
        @NotBlank
        private String projectName;
        private Long rootSectionId;   // 可选：null 表示通过计划关联模板（多模板项目）
        @NotNull                      // 必填：项目覆盖范围的 org root (数据权限边界)
        private Long orgUnitId;
        private LocalDate startDate;
    }

    @lombok.Data
    public static class UpdateProjectRequest {
        // 部分更新 DTO — 字段均可选, 仅非 null 字段被应用 (见 InspProject.updateInfo).
        private String projectName;
        private Long rootSectionId;
        private ScopeType scopeType;
        private String scopeConfig;
        private LocalDate startDate;
        private LocalDate endDate;
        private AssignmentMode assignmentMode;
        private Boolean reviewRequired;
        private Boolean autoPublish;
    }

    @lombok.Data
    public static class PublishProjectRequest {
        private Long templateVersionId;
    }

    @lombok.Data
    public static class CloneProjectRequest {
        @NotBlank
        private String projectName;
        @NotNull
        private Long orgUnitId;
        @NotNull
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean cloneInspectors;
    }

    @lombok.Data
    public static class AddInspectorRequest {
        @NotNull
        private Long userId;
        private String userName;
        @NotNull
        private InspectorRole role;
    }

    @lombok.Data
    public static class TargetPreviewRequest {
        @NotNull
        private ScopeType scopeType;
        private String scopeConfig;
        @NotNull
        private TargetType targetType;
    }

    @lombok.Data
    public static class OperationalConfigRequest {
        private String projectName;
        private AssignmentMode assignmentMode;
        private Boolean reviewRequired;
        private Boolean autoPublish;
    }
}
