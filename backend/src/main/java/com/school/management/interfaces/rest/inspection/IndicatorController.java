package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.IndicatorApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.scoring.Indicator;
import com.school.management.domain.inspection.model.scoring.LatePolicy;
import com.school.management.domain.inspection.model.scoring.MissingPolicy;
import com.school.management.domain.inspection.model.scoring.RankDirection;
import com.school.management.domain.inspection.model.scoring.SubmissionDateField;
import com.school.management.domain.inspection.model.scoring.TriggerMode;
import com.school.management.infrastructure.casbin.CasbinAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 指标控制器.
 *
 * <p>评级引擎完美架构 (2026-05-23, Phase 4): DTO 新增 sourceSectionIds (多分区) / triggerMode /
 * countThreshold / weightsBySection / rankDirection / missingPolicy / latePolicy /
 * submissionDateField — 取代/补充单值 sourceSectionId. sourceSectionId 保留向后兼容: 单值传入时
 * 自动包成 list.
 */
@RestController
@RequestMapping("/inspection/indicators")
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
    public Result<Indicator> createLeafIndicator(@RequestBody @Valid CreateLeafRequest request) {
        List<Long> sections = request.resolveSourceSectionIds();
        return Result.success(indicatorService.createLeafIndicator(
                request.getProjectId(), request.getParentIndicatorId(), request.getName(),
                sections, request.getSourceAggregation(),
                request.getTriggerMode(), request.getCountThreshold(),
                request.getWeightsBySection(), request.getRankDirection(),
                request.getMissingPolicyEnum(), request.getLatePolicy(),
                request.getSubmissionDateField(),
                request.getEvaluationPeriod(),
                request.getGradeSchemeId(), request.getNormalization(),
                request.getNormalizationConfig(),
                request.getEvaluationMethod(), request.getGradeThresholds(),
                request.getSortOrder()));
    }

    @PostMapping("/composite")
    @CasbinAccess(resource = "insp:project", action = "edit")
    public Result<Indicator> createCompositeIndicator(@RequestBody @Valid CreateCompositeRequest request) {
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
                                              @RequestBody @Valid UpdateIndicatorRequest request) {
        // 老字段路径 (name / period / scheme / 单 sourceSectionId / agg / 等), 后向兼容
        Indicator updated = indicatorService.updateIndicator(
                id, request.getName(), request.getEvaluationPeriod(),
                request.getGradeSchemeId(), request.getSourceSectionId(), request.getSourceAggregation(),
                request.getCompositeAggregation(), request.getMissingPolicy(),
                request.getNormalization(), request.getNormalizationConfig(),
                request.getEvaluationMethod(), request.getGradeThresholds(),
                request.getSortOrder());

        // Phase 4 新字段: 如客户端提供了任一新评级配置字段, 调 updateEvaluationConfig
        if (request.hasEvaluationConfigFields()) {
            updated = indicatorService.updateEvaluationConfig(id,
                    request.resolveSourceSectionIds(),
                    request.getTriggerMode(),
                    request.getCountThreshold(),
                    request.getWeightsBySection(),
                    request.getRankDirection(),
                    request.getMissingPolicyEnum(),
                    request.getLatePolicy(),
                    request.getSubmissionDateField(),
                    request.getGradeSchemeId(),
                    request.getEvaluationPeriod());
        }
        return Result.success(updated);
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
        @NotNull
        private Long projectId;
        private Long parentIndicatorId;
        @NotBlank
        private String name;

        /** @deprecated 用 sourceSectionIds 多分区, 单值时自动包成 list. */
        @Deprecated
        private Long sourceSectionId;

        /** 多分区组合: 至少 1 个 (单值时可用 sourceSectionId 兜底). */
        private List<Long> sourceSectionIds;

        private String sourceAggregation;

        // ── Phase 4 评级引擎新字段 ──
        private TriggerMode triggerMode;
        private Integer countThreshold;
        private Map<Long, BigDecimal> weightsBySection;
        private RankDirection rankDirection;
        private MissingPolicy missingPolicy;
        private LatePolicy latePolicy;
        private SubmissionDateField submissionDateField;

        private String evaluationPeriod;
        private Long gradeSchemeId;
        private String normalization;
        private String normalizationConfig;
        private String evaluationMethod;
        private String gradeThresholds;
        private Integer sortOrder;

        /** 解析为非空 list: 优先 sourceSectionIds, 否则用单值兜底. */
        @jakarta.validation.constraints.AssertTrue(message = "sourceSectionIds 至少 1 个 (或提供 sourceSectionId)")
        public boolean isSectionsProvided() {
            return (sourceSectionIds != null && !sourceSectionIds.isEmpty()) || sourceSectionId != null;
        }

        @jakarta.validation.constraints.AssertTrue(message = "triggerMode=COUNT 时 countThreshold 必填且 >=1")
        public boolean isCountThresholdValid() {
            if (triggerMode != TriggerMode.COUNT) return true;
            return countThreshold != null && countThreshold >= 1;
        }

        public List<Long> resolveSourceSectionIds() {
            if (sourceSectionIds != null && !sourceSectionIds.isEmpty()) {
                return new ArrayList<>(sourceSectionIds);
            }
            List<Long> l = new ArrayList<>();
            if (sourceSectionId != null) l.add(sourceSectionId);
            return l;
        }

        /** Service 重载需要 MissingPolicy enum, 这里直接返回. */
        public MissingPolicy getMissingPolicyEnum() {
            return missingPolicy;
        }
    }

    @lombok.Data
    public static class CreateCompositeRequest {
        @NotNull
        private Long projectId;
        private Long parentIndicatorId;
        @NotBlank
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
        @NotBlank
        private String name;
        private String evaluationPeriod;
        private Long gradeSchemeId;

        /** 单值 (向后兼容). */
        private Long sourceSectionId;
        /** 多分区 (Phase 4). */
        private List<Long> sourceSectionIds;

        private String sourceAggregation;
        private String compositeAggregation;
        private String missingPolicy;
        private String normalization;
        private String normalizationConfig;
        private String evaluationMethod;
        private String gradeThresholds;
        private Integer sortOrder;

        // ── Phase 4 评级引擎新字段 ──
        private TriggerMode triggerMode;
        private Integer countThreshold;
        private Map<Long, BigDecimal> weightsBySection;
        private RankDirection rankDirection;
        private LatePolicy latePolicy;
        private SubmissionDateField submissionDateField;

        @jakarta.validation.constraints.AssertTrue(message = "triggerMode=COUNT 时 countThreshold 必填且 >=1")
        public boolean isCountThresholdValid() {
            if (triggerMode != TriggerMode.COUNT) return true;
            return countThreshold != null && countThreshold >= 1;
        }

        /** 任一 Phase 4 字段提供则需调 updateEvaluationConfig. */
        public boolean hasEvaluationConfigFields() {
            return (sourceSectionIds != null && !sourceSectionIds.isEmpty())
                    || triggerMode != null
                    || countThreshold != null
                    || (weightsBySection != null && !weightsBySection.isEmpty())
                    || rankDirection != null
                    || latePolicy != null
                    || submissionDateField != null;
        }

        public List<Long> resolveSourceSectionIds() {
            if (sourceSectionIds != null && !sourceSectionIds.isEmpty()) {
                return new ArrayList<>(sourceSectionIds);
            }
            List<Long> l = new ArrayList<>();
            if (sourceSectionId != null) l.add(sourceSectionId);
            return l;
        }

        public MissingPolicy getMissingPolicyEnum() {
            return MissingPolicy.fromString(missingPolicy);
        }
    }

}
