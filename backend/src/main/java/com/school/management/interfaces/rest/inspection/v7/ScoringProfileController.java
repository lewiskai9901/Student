package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.ScoringProfileApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.scoring.*;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v7/insp/scoring-profiles")
@RequiredArgsConstructor
public class ScoringProfileController {

    private final ScoringProfileApplicationService scoringService;

    // ===== ScoringProfile CRUD =====

    @PostMapping
    @CasbinAccess(resource = "insp:scoring-profile", action = "create")
    public Result<ScoringProfile> createProfile(@RequestBody CreateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(scoringService.createProfile(request.getTemplateId(), userId));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:scoring-profile", action = "view")
    public Result<List<ScoringProfile>> listProfiles() {
        return Result.success(scoringService.listProfiles());
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:scoring-profile", action = "view")
    public Result<ScoringProfile> getProfile(@PathVariable Long id) {
        return Result.success(scoringService.getProfile(id)
                .orElseThrow(() -> new IllegalArgumentException("评分配置不存在: " + id)));
    }

    @GetMapping("/by-template/{templateId}")
    @CasbinAccess(resource = "insp:scoring-profile", action = "view")
    public Result<ScoringProfile> getProfileByTemplate(@PathVariable Long templateId) {
        return Result.success(scoringService.getProfileByTemplateId(templateId).orElse(null));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<ScoringProfile> updateProfile(@PathVariable Long id,
                                                 @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(scoringService.updateProfile(id,
                request.getMaxScore(), request.getMinScore(),
                request.getPrecisionDigits(), userId));
    }

    @PutMapping("/{id}/advanced-settings")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<ScoringProfile> updateAdvancedSettings(@PathVariable Long id,
                                                          @RequestBody UpdateAdvancedSettingsRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(scoringService.updateAdvancedSettings(id,
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

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:scoring-profile", action = "delete")
    public Result<Void> deleteProfile(@PathVariable Long id) {
        scoringService.deleteProfile(id);
        return Result.success();
    }

    // ===== Dimensions =====

    @PostMapping("/{id}/dimensions")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<ScoreDimension> createDimension(@PathVariable Long id,
                                                    @RequestBody CreateDimensionRequest request) {
        return Result.success(scoringService.createDimension(id,
                request.getDimensionCode(), request.getDimensionName(),
                request.getWeight(), request.getBaseScore(),
                request.getPassThreshold(), request.getSortOrder()));
    }

    @GetMapping("/{id}/dimensions")
    @CasbinAccess(resource = "insp:scoring-profile", action = "view")
    public Result<List<ScoreDimension>> listDimensions(@PathVariable Long id) {
        return Result.success(scoringService.listDimensions(id));
    }

    @PostMapping("/{id}/dimensions/sync-modules")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<List<ScoreDimension>> syncDimensionsFromModules(@PathVariable Long id) {
        return Result.success(scoringService.syncDimensionsFromModuleRefs(id));
    }

    @PutMapping("/{id}/dimensions/{dimensionId}")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<ScoreDimension> updateDimension(@PathVariable Long id,
                                                    @PathVariable Long dimensionId,
                                                    @RequestBody UpdateDimensionRequest request) {
        return Result.success(scoringService.updateDimension(dimensionId,
                request.getDimensionName(), request.getWeight(),
                request.getBaseScore(), request.getPassThreshold()));
    }

    @DeleteMapping("/{id}/dimensions/{dimensionId}")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<Void> deleteDimension(@PathVariable Long id, @PathVariable Long dimensionId) {
        scoringService.deleteDimension(dimensionId);
        return Result.success();
    }

    // ===== Grade Bands =====

    @PostMapping("/{id}/grade-bands")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<GradeBand> createGradeBand(@PathVariable Long id,
                                              @RequestBody CreateGradeBandRequest request) {
        return Result.success(scoringService.createGradeBand(id,
                request.getDimensionId(), request.getGradeCode(), request.getGradeName(),
                request.getMinScore(), request.getMaxScore(),
                request.getColor(), request.getIcon(), request.getSortOrder()));
    }

    @GetMapping("/{id}/grade-bands")
    @CasbinAccess(resource = "insp:scoring-profile", action = "view")
    public Result<List<GradeBand>> listGradeBands(@PathVariable Long id) {
        return Result.success(scoringService.listGradeBands(id));
    }

    @PutMapping("/{id}/grade-bands/{bandId}")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<GradeBand> updateGradeBand(@PathVariable Long id,
                                              @PathVariable Long bandId,
                                              @RequestBody UpdateGradeBandRequest request) {
        return Result.success(scoringService.updateGradeBand(bandId,
                request.getGradeName(), request.getMinScore(), request.getMaxScore(),
                request.getColor(), request.getIcon()));
    }

    @DeleteMapping("/{id}/grade-bands/{bandId}")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<Void> deleteGradeBand(@PathVariable Long id, @PathVariable Long bandId) {
        scoringService.deleteGradeBand(bandId);
        return Result.success();
    }

    // ===== Calculation Rules =====

    @PostMapping("/{id}/calculation-rules")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<CalculationRuleV7> createRule(@PathVariable Long id,
                                                 @RequestBody CreateRuleRequest request) {
        return Result.success(scoringService.createRule(id,
                request.getRuleCode(), request.getRuleName(), request.getPriority(),
                request.getRuleType(), request.getConfig(), request.getIsEnabled(),
                request.getScopeType(), request.getTargetDimensionIds(),
                request.getActivationCondition(), request.getAppliesTo(),
                request.getEffectiveFrom(), request.getEffectiveUntil(),
                request.getExclusionGroup()));
    }

    @GetMapping("/{id}/calculation-rules")
    @CasbinAccess(resource = "insp:scoring-profile", action = "view")
    public Result<List<CalculationRuleV7>> listRules(@PathVariable Long id) {
        return Result.success(scoringService.listRules(id));
    }

    @PutMapping("/{id}/calculation-rules/{ruleId}")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<CalculationRuleV7> updateRule(@PathVariable Long id,
                                                 @PathVariable Long ruleId,
                                                 @RequestBody UpdateRuleRequest request) {
        return Result.success(scoringService.updateRule(ruleId,
                request.getRuleName(), request.getPriority(),
                request.getRuleType(), request.getConfig(), request.getIsEnabled(),
                request.getScopeType(), request.getTargetDimensionIds(),
                request.getActivationCondition(), request.getAppliesTo(),
                request.getEffectiveFrom(), request.getEffectiveUntil(),
                request.getExclusionGroup()));
    }

    @DeleteMapping("/{id}/calculation-rules/{ruleId}")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<Void> deleteRule(@PathVariable Long id, @PathVariable Long ruleId) {
        scoringService.deleteRule(ruleId);
        return Result.success();
    }

    // ===== Escalation Policies (1.3) =====

    @PostMapping("/{id}/escalation-policies")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<EscalationPolicy> createEscalationPolicy(@PathVariable Long id,
                                                            @RequestBody CreateEscalationPolicyRequest request) {
        return Result.success(scoringService.createEscalationPolicy(id,
                request.getPolicyName(), request.getLookupPeriodDays(),
                request.getEscalationMode(), request.getMultiplier(), request.getAdder(),
                request.getFixedTable(), request.getMaxEscalationFactor(),
                request.getMatchBy(), request.getIsEnabled()));
    }

    @GetMapping("/{id}/escalation-policies")
    @CasbinAccess(resource = "insp:scoring-profile", action = "view")
    public Result<List<EscalationPolicy>> listEscalationPolicies(@PathVariable Long id) {
        return Result.success(scoringService.listEscalationPolicies(id));
    }

    @PutMapping("/{id}/escalation-policies/{policyId}")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<EscalationPolicy> updateEscalationPolicy(@PathVariable Long id,
                                                            @PathVariable Long policyId,
                                                            @RequestBody UpdateEscalationPolicyRequest request) {
        return Result.success(scoringService.updateEscalationPolicy(policyId,
                request.getPolicyName(), request.getLookupPeriodDays(),
                request.getEscalationMode(), request.getMultiplier(), request.getAdder(),
                request.getFixedTable(), request.getMaxEscalationFactor(),
                request.getMatchBy(), request.getIsEnabled()));
    }

    @DeleteMapping("/{id}/escalation-policies/{policyId}")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<Void> deleteEscalationPolicy(@PathVariable Long id, @PathVariable Long policyId) {
        scoringService.deleteEscalationPolicy(policyId);
        return Result.success();
    }

    // ===== Profile Versioning (1.7) =====

    @PostMapping("/{id}/versions")
    @CasbinAccess(resource = "insp:scoring-profile", action = "edit")
    public Result<ScoringProfileVersion> publishVersion(@PathVariable Long id,
                                                         @RequestBody PublishVersionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(scoringService.publishVersion(id, request.getChangeSummary(), userId));
    }

    @GetMapping("/{id}/versions")
    @CasbinAccess(resource = "insp:scoring-profile", action = "view")
    public Result<List<ScoringProfileVersion>> listVersions(@PathVariable Long id) {
        return Result.success(scoringService.listVersions(id));
    }

    @GetMapping("/{id}/versions/{version}")
    @CasbinAccess(resource = "insp:scoring-profile", action = "view")
    public Result<ScoringProfileVersion> getVersion(@PathVariable Long id, @PathVariable Integer version) {
        return Result.success(scoringService.getVersion(id, version)
                .orElseThrow(() -> new IllegalArgumentException("版本不存在: " + version)));
    }

    // ===== Request DTOs =====

    @lombok.Data
    public static class CreateProfileRequest {
        private Long templateId;
    }

    @lombok.Data
    public static class UpdateProfileRequest {
        private BigDecimal maxScore;
        private BigDecimal minScore;
        private Integer precisionDigits;
    }

    @lombok.Data
    public static class CreateDimensionRequest {
        private String dimensionCode;
        private String dimensionName;
        private Integer weight;
        private BigDecimal baseScore;
        private BigDecimal passThreshold;
        private Integer sortOrder;
    }

    @lombok.Data
    public static class UpdateDimensionRequest {
        private String dimensionName;
        private Integer weight;
        private BigDecimal baseScore;
        private BigDecimal passThreshold;
    }

    @lombok.Data
    public static class CreateGradeBandRequest {
        private Long dimensionId;
        private String gradeCode;
        private String gradeName;
        private BigDecimal minScore;
        private BigDecimal maxScore;
        private String color;
        private String icon;
        private Integer sortOrder;
    }

    @lombok.Data
    public static class UpdateGradeBandRequest {
        private String gradeName;
        private BigDecimal minScore;
        private BigDecimal maxScore;
        private String color;
        private String icon;
    }

    @lombok.Data
    public static class CreateRuleRequest {
        private String ruleCode;
        private String ruleName;
        private Integer priority;
        private RuleType ruleType;
        private String config;
        private Boolean isEnabled;
        private String scopeType;
        private String targetDimensionIds;
        private String activationCondition;
        private String appliesTo;
        private LocalDate effectiveFrom;
        private LocalDate effectiveUntil;
        private String exclusionGroup;
    }

    @lombok.Data
    public static class UpdateRuleRequest {
        private String ruleName;
        private Integer priority;
        private RuleType ruleType;
        private String config;
        private Boolean isEnabled;
        private String scopeType;
        private String targetDimensionIds;
        private String activationCondition;
        private String appliesTo;
        private LocalDate effectiveFrom;
        private LocalDate effectiveUntil;
        private String exclusionGroup;
    }

    @lombok.Data
    public static class CreateEscalationPolicyRequest {
        private String policyName;
        private Integer lookupPeriodDays;
        private String escalationMode;
        private BigDecimal multiplier;
        private BigDecimal adder;
        private String fixedTable;
        private BigDecimal maxEscalationFactor;
        private String matchBy;
        private Boolean isEnabled;
    }

    @lombok.Data
    public static class UpdateEscalationPolicyRequest {
        private String policyName;
        private Integer lookupPeriodDays;
        private String escalationMode;
        private BigDecimal multiplier;
        private BigDecimal adder;
        private String fixedTable;
        private BigDecimal maxEscalationFactor;
        private String matchBy;
        private Boolean isEnabled;
    }

    @lombok.Data
    public static class PublishVersionRequest {
        private String changeSummary;
    }

    @lombok.Data
    public static class UpdateAdvancedSettingsRequest {
        // 1.9 趋势因子
        private Boolean trendFactorEnabled;
        private Integer trendLookbackDays;
        private BigDecimal trendBonusPerPercent;
        private BigDecimal trendPenaltyPerPercent;
        private BigDecimal trendMaxAdjustment;
        // 1.10 分数衰减
        private Boolean decayEnabled;
        private String decayMode;
        private BigDecimal decayRatePerDay;
        private BigDecimal decayFloor;
        // 1.11 多评审员聚合
        private String multiRaterMode;
        private String raterWeightBy;
        private BigDecimal consensusThreshold;
        // 1.12 分布校准
        private Boolean calibrationEnabled;
        private String calibrationMethod;
        private Integer calibrationPeriodDays;
        private Integer calibrationMinSamples;
    }
}
