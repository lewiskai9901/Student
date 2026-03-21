package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.ScoringPolicyApplicationService2;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.scoring.PolicyCalcRule;
import com.school.management.domain.inspection.model.v7.scoring.PolicyGradeBand;
import com.school.management.domain.inspection.model.v7.scoring.ScoringPolicy;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 评分方案 REST API
 *
 * GET    /v7/insp/scoring-policies
 * POST   /v7/insp/scoring-policies
 * GET    /v7/insp/scoring-policies/{id}
 * PUT    /v7/insp/scoring-policies/{id}
 * DELETE /v7/insp/scoring-policies/{id}
 *
 * GET    /v7/insp/scoring-policies/{id}/grade-bands
 * POST   /v7/insp/scoring-policies/{id}/grade-bands
 * PUT    /v7/insp/scoring-policies/{id}/grade-bands/{bandId}
 * DELETE /v7/insp/scoring-policies/{id}/grade-bands/{bandId}
 *
 * GET    /v7/insp/scoring-policies/{id}/calc-rules
 * POST   /v7/insp/scoring-policies/{id}/calc-rules
 * PUT    /v7/insp/scoring-policies/{id}/calc-rules/{ruleId}
 * DELETE /v7/insp/scoring-policies/{id}/calc-rules/{ruleId}
 */
@Slf4j
@RestController
@RequestMapping("/v7/insp/scoring-policies")
@RequiredArgsConstructor
public class ScoringPolicyController2 {

    private final ScoringPolicyApplicationService2 policyService;

    // ========== ScoringPolicy ==========

    @GetMapping
    @CasbinAccess(resource = "insp:scoring-policy", action = "view")
    public Result<List<ScoringPolicy>> listPolicies() {
        return Result.success(policyService.listPolicies());
    }

    @PostMapping
    @CasbinAccess(resource = "insp:scoring-policy", action = "create")
    public Result<ScoringPolicy> createPolicy(@RequestBody CreatePolicyRequest request) {
        ScoringPolicy policy = policyService.createPolicy(
                request.getPolicyCode(), request.getPolicyName(), request.getDescription(),
                request.getMaxScore(), request.getMinScore(),
                request.getPrecisionDigits(), request.getSortOrder());
        return Result.success(policy);
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:scoring-policy", action = "view")
    public Result<ScoringPolicy> getPolicy(@PathVariable Long id) {
        return Result.success(policyService.getPolicy(id)
                .orElseThrow(() -> new IllegalArgumentException("评分方案不存在: " + id)));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:scoring-policy", action = "edit")
    public Result<ScoringPolicy> updatePolicy(@PathVariable Long id,
                                               @RequestBody UpdatePolicyRequest request) {
        ScoringPolicy policy = policyService.updatePolicy(id,
                request.getPolicyName(), request.getDescription(),
                request.getMaxScore(), request.getMinScore(),
                request.getPrecisionDigits(), request.getSortOrder());
        return Result.success(policy);
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:scoring-policy", action = "delete")
    public Result<Void> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        return Result.success();
    }

    // ========== PolicyGradeBand ==========

    @GetMapping("/{id}/grade-bands")
    @CasbinAccess(resource = "insp:scoring-policy", action = "view")
    public Result<List<PolicyGradeBand>> listGradeBands(@PathVariable Long id) {
        return Result.success(policyService.listGradeBands(id));
    }

    @PostMapping("/{id}/grade-bands")
    @CasbinAccess(resource = "insp:scoring-policy", action = "edit")
    public Result<PolicyGradeBand> createGradeBand(@PathVariable Long id,
                                                    @RequestBody GradeBandRequest request) {
        PolicyGradeBand band = policyService.createGradeBand(id,
                request.getGradeCode(), request.getGradeName(),
                request.getMinScore(), request.getMaxScore(), request.getSortOrder());
        return Result.success(band);
    }

    @PutMapping("/{id}/grade-bands/{bandId}")
    @CasbinAccess(resource = "insp:scoring-policy", action = "edit")
    public Result<PolicyGradeBand> updateGradeBand(@PathVariable Long id,
                                                    @PathVariable Long bandId,
                                                    @RequestBody GradeBandRequest request) {
        PolicyGradeBand band = policyService.updateGradeBand(id, bandId,
                request.getGradeCode(), request.getGradeName(),
                request.getMinScore(), request.getMaxScore(), request.getSortOrder());
        return Result.success(band);
    }

    @DeleteMapping("/{id}/grade-bands/{bandId}")
    @CasbinAccess(resource = "insp:scoring-policy", action = "delete")
    public Result<Void> deleteGradeBand(@PathVariable Long id, @PathVariable Long bandId) {
        policyService.deleteGradeBand(id, bandId);
        return Result.success();
    }

    // ========== PolicyCalcRule ==========

    @GetMapping("/{id}/calc-rules")
    @CasbinAccess(resource = "insp:scoring-policy", action = "view")
    public Result<List<PolicyCalcRule>> listCalcRules(@PathVariable Long id) {
        return Result.success(policyService.listCalcRules(id));
    }

    @PostMapping("/{id}/calc-rules")
    @CasbinAccess(resource = "insp:scoring-policy", action = "edit")
    public Result<PolicyCalcRule> createCalcRule(@PathVariable Long id,
                                                  @RequestBody CalcRuleRequest request) {
        PolicyCalcRule rule = policyService.createCalcRule(id,
                request.getRuleCode(), request.getRuleName(),
                request.getRuleType(), request.getPriority(), request.getConfig());
        return Result.success(rule);
    }

    @PutMapping("/{id}/calc-rules/{ruleId}")
    @CasbinAccess(resource = "insp:scoring-policy", action = "edit")
    public Result<PolicyCalcRule> updateCalcRule(@PathVariable Long id,
                                                  @PathVariable Long ruleId,
                                                  @RequestBody CalcRuleRequest request) {
        PolicyCalcRule rule = policyService.updateCalcRule(id, ruleId,
                request.getRuleCode(), request.getRuleName(),
                request.getRuleType(), request.getPriority(),
                request.getConfig(), request.getIsEnabled());
        return Result.success(rule);
    }

    @DeleteMapping("/{id}/calc-rules/{ruleId}")
    @CasbinAccess(resource = "insp:scoring-policy", action = "delete")
    public Result<Void> deleteCalcRule(@PathVariable Long id, @PathVariable Long ruleId) {
        policyService.deleteCalcRule(id, ruleId);
        return Result.success();
    }

    // ========== Request DTOs ==========

    @Data
    public static class CreatePolicyRequest {
        private String policyCode;
        private String policyName;
        private String description;
        private BigDecimal maxScore;
        private BigDecimal minScore;
        private Integer precisionDigits;
        private Integer sortOrder;
    }

    @Data
    public static class UpdatePolicyRequest {
        private String policyName;
        private String description;
        private BigDecimal maxScore;
        private BigDecimal minScore;
        private Integer precisionDigits;
        private Integer sortOrder;
    }

    @Data
    public static class GradeBandRequest {
        private String gradeCode;
        private String gradeName;
        private BigDecimal minScore;
        private BigDecimal maxScore;
        private Integer sortOrder;
    }

    @Data
    public static class CalcRuleRequest {
        private String ruleCode;
        private String ruleName;
        private String ruleType;
        private Integer priority;
        private String config;
        private Boolean isEnabled;
    }
}
