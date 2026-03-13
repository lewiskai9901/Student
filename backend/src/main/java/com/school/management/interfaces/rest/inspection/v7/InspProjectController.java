package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.InspProjectApplicationService;
import com.school.management.application.inspection.v7.ScoreAggregationService;
import com.school.management.application.inspection.v7.TargetPopulationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v7/insp/projects")
@RequiredArgsConstructor
public class InspProjectController {

    private final InspProjectApplicationService projectService;
    private final TargetPopulationService targetPopulationService;
    private final ScoreAggregationService scoreAggregationService;

    // ========== Project CRUD ==========

    @PostMapping
    @CasbinAccess(resource = "insp:project", action = "create")
    public Result<InspProject> createProject(@RequestBody CreateProjectRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        InspProject project = projectService.createProject(
                request.getProjectName(), request.getTemplateId(),
                request.getStartDate(), userId);
        return Result.success(project);
    }

    @GetMapping
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<InspProject>> listProjects(
            @RequestParam(required = false) String status) {
        if (status != null) {
            return Result.success(projectService.listProjectsByStatus(
                    ProjectStatus.valueOf(status)));
        }
        return Result.success(projectService.listProjects());
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
                                              @RequestBody UpdateProjectRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        InspProject project = projectService.updateProject(id,
                request.getProjectName(), request.getTemplateId(),
                request.getScoringProfileId(), request.getScopeType(),
                request.getScopeConfig(), request.getTargetType(),
                request.getStartDate(), request.getEndDate(),
                request.getCycleType(), request.getCycleConfig(),
                request.getTimeSlots(), request.getSkipHolidays(),
                request.getHolidayCalendarId(), request.getExcludedDates(),
                request.getAssignmentMode(), request.getReviewRequired(),
                request.getAutoPublish(), userId);
        return Result.success(project);
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:project", action = "delete")
    public Result<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return Result.success();
    }

    // ========== Lifecycle ==========

    @PostMapping("/{id}/publish")
    @CasbinAccess(resource = "insp:project", action = "publish")
    public Result<InspProject> publishProject(@PathVariable Long id,
                                               @RequestBody PublishProjectRequest request) {
        return Result.success(projectService.publishProject(id, request.getTemplateVersionId()));
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

    // ========== Child Projects ==========

    @GetMapping("/{id}/children")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<InspProject>> listChildProjects(@PathVariable Long id) {
        return Result.success(projectService.listChildProjects(id));
    }

    @GetMapping("/{id}/scores")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<ProjectScore>> listProjectScores(@PathVariable Long id) {
        return Result.success(projectService.listProjectScores(id));
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
                                                  @RequestBody AddInspectorRequest request) {
        return Result.success(projectService.addInspector(projectId,
                request.getUserId(), request.getUserName(), request.getRole()));
    }

    @DeleteMapping("/{projectId}/inspectors/{inspectorId}")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<Void> removeInspector(@PathVariable Long projectId,
                                         @PathVariable Long inspectorId) {
        projectService.removeInspector(inspectorId);
        return Result.success();
    }

    // ========== Score Tree ==========

    @GetMapping("/{id}/score-tree")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<Map<String, Object>> getScoreTree(@PathVariable Long id) {
        return Result.success(buildScoreTreeNode(id));
    }

    @PostMapping("/{id}/aggregate-score")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<ProjectScore> aggregateScore(@PathVariable Long id,
                                                @RequestParam String cycleDate) {
        return Result.success(scoreAggregationService.aggregateParentScore(id, LocalDate.parse(cycleDate)));
    }

    // ========== Target Preview ==========

    @PostMapping("/target-preview")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<Integer> previewTargetCount(@RequestBody TargetPreviewRequest request) {
        List<TargetPopulationService.TargetInfo> targets = targetPopulationService.resolveTargets(
                request.getScopeType(), request.getScopeConfig(), request.getTargetType());
        return Result.success(targets.size());
    }

    private Map<String, Object> buildScoreTreeNode(Long projectId) {
        InspProject project = projectService.getProject(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));
        List<ProjectScore> scores = projectService.listProjectScores(projectId);
        List<InspProject> children = projectService.listChildProjects(projectId);

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("project", project);
        node.put("scores", scores);
        node.put("children", children.stream()
                .map(child -> buildScoreTreeNode(child.getId()))
                .collect(Collectors.toList()));
        return node;
    }

    // --- Request DTOs ---

    @lombok.Data
    public static class CreateProjectRequest {
        private String projectName;
        private Long templateId;
        private LocalDate startDate;
    }

    @lombok.Data
    public static class UpdateProjectRequest {
        private String projectName;
        private Long templateId;
        private Long scoringProfileId;
        private ScopeType scopeType;
        private String scopeConfig;
        private TargetType targetType;
        private LocalDate startDate;
        private LocalDate endDate;
        private CycleType cycleType;
        private String cycleConfig;
        private String timeSlots;
        private Boolean skipHolidays;
        private Long holidayCalendarId;
        private String excludedDates;
        private AssignmentMode assignmentMode;
        private Boolean reviewRequired;
        private Boolean autoPublish;
    }

    @lombok.Data
    public static class PublishProjectRequest {
        private Long templateVersionId;
    }

    @lombok.Data
    public static class AddInspectorRequest {
        private Long userId;
        private String userName;
        private InspectorRole role;
    }

    @lombok.Data
    public static class TargetPreviewRequest {
        private ScopeType scopeType;
        private String scopeConfig;
        private TargetType targetType;
    }
}
