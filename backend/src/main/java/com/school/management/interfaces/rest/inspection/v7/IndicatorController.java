package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.IndicatorApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.scoring.Indicator;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v7/insp/indicators")
@RequiredArgsConstructor
public class IndicatorController {

    private final IndicatorApplicationService indicatorService;

    @GetMapping
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<Indicator>> getIndicatorTree(@RequestParam Long projectId) {
        return Result.success(indicatorService.getIndicatorTree(projectId));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<Indicator> getIndicator(@PathVariable Long id) {
        return Result.success(indicatorService.getIndicator(id));
    }

    @PostMapping("/leaf")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<Indicator> createLeafIndicator(@RequestBody CreateLeafRequest request) {
        return Result.success(indicatorService.createLeafIndicator(
                request.getProjectId(), request.getParentIndicatorId(), request.getName(),
                request.getSourceSectionId(), request.getSourceAggregation(),
                request.getEvaluationPeriod(),
                request.getGradeSchemeId(), request.getNormalization(),
                request.getNormalizationConfig(),
                request.getEvaluationMethod(), request.getGradeThresholds(),
                request.getSortOrder()));
    }

    @PostMapping("/composite")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<Indicator> createCompositeIndicator(@RequestBody CreateCompositeRequest request) {
        return Result.success(indicatorService.createCompositeIndicator(
                request.getProjectId(), request.getParentIndicatorId(), request.getName(),
                request.getCompositeAggregation(), request.getMissingPolicy(),
                request.getEvaluationPeriod(),
                request.getGradeSchemeId(), request.getNormalization(),
                request.getNormalizationConfig(),
                request.getEvaluationMethod(), request.getGradeThresholds(),
                request.getSortOrder()));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<Indicator> updateIndicator(@PathVariable Long id,
                                              @RequestBody UpdateIndicatorRequest request) {
        return Result.success(indicatorService.updateIndicator(
                id, request.getName(), request.getEvaluationPeriod(),
                request.getGradeSchemeId(), request.getSourceSectionId(), request.getSourceAggregation(),
                request.getCompositeAggregation(), request.getMissingPolicy(),
                request.getNormalization(), request.getNormalizationConfig(),
                request.getEvaluationMethod(), request.getGradeThresholds(),
                request.getSortOrder()));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<Void> deleteIndicator(@PathVariable Long id) {
        indicatorService.deleteIndicator(id);
        return Result.success();
    }

    // ── Request DTOs ────────────────────────────────────────────

    @lombok.Data
    public static class CreateLeafRequest {
        private Long projectId;
        private Long parentIndicatorId;
        private String name;
        private Long sourceSectionId;
        private String sourceAggregation;
        private String evaluationPeriod;
        private Long gradeSchemeId;
        private String normalization;
        private String normalizationConfig;
        private String evaluationMethod;
        private String gradeThresholds;
        private Integer sortOrder;
    }

    @lombok.Data
    public static class CreateCompositeRequest {
        private Long projectId;
        private Long parentIndicatorId;
        private String name;
        private String compositeAggregation;
        private String missingPolicy;
        private String evaluationPeriod;
        private Long gradeSchemeId;
        private String normalization;
        private String normalizationConfig;
        private String evaluationMethod;
        private String gradeThresholds;
        private Integer sortOrder;
    }

    @lombok.Data
    public static class UpdateIndicatorRequest {
        private String name;
        private String evaluationPeriod;
        private Long gradeSchemeId;
        private Long sourceSectionId;
        private String sourceAggregation;
        private String compositeAggregation;
        private String missingPolicy;
        private String normalization;
        private String normalizationConfig;
        private String evaluationMethod;
        private String gradeThresholds;
        private Integer sortOrder;
    }

}
