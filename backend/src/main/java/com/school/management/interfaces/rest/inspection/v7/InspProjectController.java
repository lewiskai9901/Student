package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.InspProjectApplicationService;
import com.school.management.application.inspection.v7.ScoreAggregationService;
import com.school.management.application.inspection.v7.ScoringProfileApplicationService;
import com.school.management.application.inspection.v7.TargetPopulationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.execution.*;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v7/insp/projects")
@RequiredArgsConstructor
public class InspProjectController {

    private final InspProjectApplicationService projectService;
    private final ScoringProfileApplicationService scoringService;
    private final TargetPopulationService targetPopulationService;
    private final ScoreAggregationService scoreAggregationService;

    // ========== Project CRUD ==========

    @PostMapping
    @CasbinAccess(resource = "insp:project", action = "create")
    public Result<InspProject> createProject(@RequestBody CreateProjectRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        InspProject project = projectService.createProject(
                request.getProjectName(), request.getRootSectionId(),
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
                request.getProjectName(), request.getRootSectionId(),
                request.getScoringProfileId(), request.getScopeType(),
                request.getScopeConfig(),
                request.getStartDate(), request.getEndDate(),
                request.getAssignmentMode(), request.getReviewRequired(),
                request.getAutoPublish(), userId);
        return Result.success(project);
    }

    @PatchMapping("/{id}/config")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<InspProject> updateOperationalConfig(@PathVariable Long id,
                                                        @RequestBody OperationalConfigRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(projectService.updateOperationalConfig(id,
                request.getAssignmentMode(), request.getReviewRequired(),
                request.getAutoPublish(), request.getProjectName(), userId));
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

    // ========== Scores ==========

    @GetMapping("/{id}/scores")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<ProjectScore>> listProjectScores(@PathVariable Long id) {
        return Result.success(projectService.listProjectScores(id));
    }

    @PostMapping("/{id}/grade-score")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<ProjectScore> gradeScore(@PathVariable Long id,
                                            @RequestParam String cycleDate) {
        return Result.success(scoreAggregationService.gradeProjectScore(id, LocalDate.parse(cycleDate)));
    }

    // ========== Advanced Scoring Settings (project-level) ==========

    @GetMapping("/{id}/advanced-scoring")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<?> getAdvancedScoring(@PathVariable Long id) {
        InspProject project = projectService.getProject(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id));
        if (project.getScoringProfileId() == null) return Result.success(null);
        return Result.success(scoringService.getProfile(project.getScoringProfileId()).orElse(null));
    }

    @PatchMapping("/{id}/advanced-scoring")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<?> updateAdvancedScoring(@PathVariable Long id,
                                            @RequestBody AdvancedScoringRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        InspProject project = projectService.getProject(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + id));
        if (project.getScoringProfileId() == null) {
            throw new IllegalArgumentException("项目未关联评分配置");
        }
        return Result.success(scoringService.updateAdvancedSettings(
                project.getScoringProfileId(),
                request.getTrendFactorEnabled(), request.getTrendLookbackDays(),
                request.getTrendBonusPerPercent(), request.getTrendPenaltyPerPercent(),
                request.getTrendMaxAdjustment(),
                request.getDecayEnabled(), request.getDecayMode(),
                request.getDecayRatePerDay(), request.getDecayFloor(),
                request.getMultiRaterMode(), request.getRaterWeightBy(),
                request.getConsensusThreshold(),
                request.getCalibrationEnabled(), request.getCalibrationMethod(),
                request.getCalibrationPeriodDays(), request.getCalibrationMinSamples(),
                userId));
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

    // ========== Target Preview ==========

    @PostMapping("/target-preview")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<Integer> previewTargetCount(@RequestBody TargetPreviewRequest request) {
        List<TargetPopulationService.TargetInfo> targets = targetPopulationService.resolveTargets(
                request.getScopeType(), request.getScopeConfig(), request.getTargetType());
        return Result.success(targets.size());
    }

    // ========== Target Persons (for PERSON_SCORE field type) ==========

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
        private String projectName;
        private Long rootSectionId;   // 可选：null 表示通过计划关联模板（多模板项目）
        private LocalDate startDate;
    }

    @lombok.Data
    public static class UpdateProjectRequest {
        private String projectName;
        private Long rootSectionId;
        private Long scoringProfileId;
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

    @lombok.Data
    public static class OperationalConfigRequest {
        private String projectName;
        private AssignmentMode assignmentMode;
        private Boolean reviewRequired;
        private Boolean autoPublish;
    }

    @lombok.Data
    public static class AdvancedScoringRequest {
        private Boolean trendFactorEnabled;
        private Integer trendLookbackDays;
        private BigDecimal trendBonusPerPercent;
        private BigDecimal trendPenaltyPerPercent;
        private BigDecimal trendMaxAdjustment;
        private Boolean decayEnabled;
        private String decayMode;
        private BigDecimal decayRatePerDay;
        private BigDecimal decayFloor;
        private String multiRaterMode;
        private String raterWeightBy;
        private BigDecimal consensusThreshold;
        private Boolean calibrationEnabled;
        private String calibrationMethod;
        private Integer calibrationPeriodDays;
        private Integer calibrationMinSamples;
    }
}
