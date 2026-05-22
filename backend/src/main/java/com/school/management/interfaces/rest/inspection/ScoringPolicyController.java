package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.ScoringPolicyApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.scoring.PolicyCalcRule;
import com.school.management.domain.inspection.model.scoring.PolicyGradeBand;
import com.school.management.domain.inspection.model.scoring.ScoringPolicy;
import com.school.management.infrastructure.casbin.CasbinAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 评分方案 REST API
 *
 * GET    /inspection/scoring-policies
 * POST   /inspection/scoring-policies
 * GET    /inspection/scoring-policies/{id}
 * PUT    /inspection/scoring-policies/{id}
 * DELETE /inspection/scoring-policies/{id}
 *
 * GET    /inspection/scoring-policies/{id}/grade-bands
 * POST   /inspection/scoring-policies/{id}/grade-bands
 * PUT    /inspection/scoring-policies/{id}/grade-bands/{bandId}
 * DELETE /inspection/scoring-policies/{id}/grade-bands/{bandId}
 *
 * GET    /inspection/scoring-policies/{id}/calc-rules
 * POST   /inspection/scoring-policies/{id}/calc-rules
 * PUT    /inspection/scoring-policies/{id}/calc-rules/{ruleId}
 * DELETE /inspection/scoring-policies/{id}/calc-rules/{ruleId}
 */
@Slf4j
@RestController
@RequestMapping("/inspection/scoring-policies")
@RequiredArgsConstructor
public class ScoringPolicyController {

    private final ScoringPolicyApplicationService policyService;

    // ========== ScoringPolicy ==========

    @GetMapping
    @CasbinAccess(resource = "insp:scoring-policy", action = "view")
    public Result<List<ScoringPolicy>> listPolicies() {
        return Result.success(policyService.listPolicies());
    }

    @PostMapping
    @CasbinAccess(resource = "insp:scoring-policy", action = "create")
    public Result<ScoringPolicy> createPolicy(@RequestBody @Valid CreatePolicyRequest request) {
        ScoringPolicy policy = policyService.createPolicy(
                request.getPolicyCode(), request.getPolicyName(), request.getDescription(),
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
                                               @RequestBody @Valid UpdatePolicyRequest request) {
        ScoringPolicy policy = policyService.updatePolicy(id,
                request.getPolicyName(), request.getDescription(),
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
                                                    @RequestBody @Valid GradeBandRequest request) {
        PolicyGradeBand band = policyService.createGradeBand(id,
                request.getGradeCode(), request.getGradeName(),
                request.getMinPercent(), request.getMaxPercent(), request.getSortOrder());
        return Result.success(band);
    }

    @PutMapping("/{id}/grade-bands/{bandId}")
    @CasbinAccess(resource = "insp:scoring-policy", action = "edit")
    public Result<PolicyGradeBand> updateGradeBand(@PathVariable Long id,
                                                    @PathVariable Long bandId,
                                                    @RequestBody @Valid GradeBandRequest request) {
        PolicyGradeBand band = policyService.updateGradeBand(id, bandId,
                request.getGradeCode(), request.getGradeName(),
                request.getMinPercent(), request.getMaxPercent(), request.getSortOrder());
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
                                                  @RequestBody @Valid CalcRuleRequest request) {
        PolicyCalcRule rule = policyService.createCalcRule(id,
                request.getRuleCode(), request.getRuleName(),
                request.getRuleType(), request.getPriority(), request.getConfig());
        return Result.success(rule);
    }

    @PutMapping("/{id}/calc-rules/{ruleId}")
    @CasbinAccess(resource = "insp:scoring-policy", action = "edit")
    public Result<PolicyCalcRule> updateCalcRule(@PathVariable Long id,
                                                  @PathVariable Long ruleId,
                                                  @RequestBody @Valid CalcRuleRequest request) {
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
        @NotBlank
        private String policyCode;
        @NotBlank
        private String policyName;
        private String description;
        private Integer precisionDigits;
        private Integer sortOrder;
    }

    @Data
    public static class UpdatePolicyRequest {
        @NotBlank
        private String policyName;
        private String description;
        private Integer precisionDigits;
        private Integer sortOrder;
    }

    @Data
    public static class GradeBandRequest {
        @NotBlank
        private String gradeCode;
        @NotBlank
        private String gradeName;
        private BigDecimal minPercent;
        private BigDecimal maxPercent;
        private Integer sortOrder;
    }

    @Data
    public static class CalcRuleRequest {
        @NotBlank
        private String ruleCode;
        @NotBlank
        private String ruleName;
        @NotBlank
        private String ruleType;
        private Integer priority;
        private String config;
        private Boolean isEnabled;
    }
}
