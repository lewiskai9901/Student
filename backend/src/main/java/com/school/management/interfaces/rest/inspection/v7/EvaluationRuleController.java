package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.EvaluationRuleApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v7.scoring.EvaluationLevel;
import com.school.management.domain.inspection.model.v7.scoring.EvaluationResult;
import com.school.management.domain.inspection.model.v7.scoring.EvaluationRule;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 评选规则 REST API
 *
 * GET    /v7/insp/evaluation-rules?projectId=xxx
 * POST   /v7/insp/evaluation-rules
 * GET    /v7/insp/evaluation-rules/{id}
 * PUT    /v7/insp/evaluation-rules/{id}
 * DELETE /v7/insp/evaluation-rules/{id}
 *
 * GET    /v7/insp/evaluation-rules/{id}/levels
 * PUT    /v7/insp/evaluation-rules/{id}/levels    (批量保存)
 *
 * POST   /v7/insp/evaluation-rules/{id}/evaluate?cycleStart=&cycleEnd=
 * GET    /v7/insp/evaluation-rules/{id}/results?cycleDate=
 */
@Slf4j
@RestController
@RequestMapping("/v7/insp/evaluation-rules")
@RequiredArgsConstructor
public class EvaluationRuleController {

    private final EvaluationRuleApplicationService ruleService;

    // ========== EvaluationRule CRUD ==========

    @GetMapping
    @CasbinAccess(resource = "insp:evaluation-rule", action = "view")
    public Result<List<EvaluationRule>> listRules(
            @RequestParam(required = false) Long projectId) {
        if (projectId == null) {
            return Result.error("projectId 参数必填");
        }
        return Result.success(ruleService.listRules(projectId));
    }

    @PostMapping
    @CasbinAccess(resource = "insp:evaluation-rule", action = "create")
    public Result<EvaluationRule> createRule(@RequestBody CreateRuleRequest request) {
        EvaluationRule rule = ruleService.createRule(
                request.getProjectId(), request.getRuleName(), request.getRuleDescription(),
                request.getTargetType(), request.getEvaluationPeriod(),
                request.getAwardName(), request.getRankingEnabled(), request.getSortOrder());
        return Result.success(rule);
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:evaluation-rule", action = "view")
    public Result<RuleWithLevels> getRule(@PathVariable Long id) {
        EvaluationRule rule = ruleService.getRule(id)
                .orElseThrow(() -> new IllegalArgumentException("评选规则不存在: " + id));
        List<EvaluationLevel> levels = ruleService.getLevels(id);
        return Result.success(new RuleWithLevels(rule, levels));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:evaluation-rule", action = "edit")
    public Result<EvaluationRule> updateRule(@PathVariable Long id,
                                              @RequestBody UpdateRuleRequest request) {
        EvaluationRule rule = ruleService.updateRule(id,
                request.getRuleName(), request.getRuleDescription(),
                request.getTargetType(), request.getEvaluationPeriod(),
                request.getAwardName(), request.getRankingEnabled(), request.getSortOrder());
        return Result.success(rule);
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:evaluation-rule", action = "delete")
    public Result<Void> deleteRule(@PathVariable Long id) {
        ruleService.deleteRule(id);
        return Result.success();
    }

    // ========== EvaluationLevel ==========

    @GetMapping("/{id}/levels")
    @CasbinAccess(resource = "insp:evaluation-rule", action = "view")
    public Result<List<EvaluationLevel>> getLevels(@PathVariable Long id) {
        return Result.success(ruleService.getLevels(id));
    }

    @PutMapping("/{id}/levels")
    @CasbinAccess(resource = "insp:evaluation-rule", action = "edit")
    public Result<List<EvaluationLevel>> saveLevels(@PathVariable Long id,
                                                     @RequestBody List<EvaluationLevel> levels) {
        return Result.success(ruleService.saveLevels(id, levels));
    }

    // ========== 执行评选 ==========

    @PostMapping("/{id}/evaluate")
    @CasbinAccess(resource = "insp:evaluation-rule", action = "execute")
    public Result<List<EvaluationResult>> evaluate(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cycleStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cycleEnd) {
        if (cycleEnd.isBefore(cycleStart)) {
            return Result.error("cycleEnd 不能早于 cycleStart");
        }
        List<EvaluationResult> results = ruleService.evaluate(id, cycleStart, cycleEnd);
        return Result.success(results);
    }

    // ========== 查询结果 ==========

    @GetMapping("/{id}/results")
    @CasbinAccess(resource = "insp:evaluation-rule", action = "view")
    public Result<List<EvaluationResult>> getResults(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate cycleDate) {
        return Result.success(ruleService.getResults(id, cycleDate));
    }

    // ========== Request/Response DTOs ==========

    @Data
    public static class CreateRuleRequest {
        private Long projectId;
        private String ruleName;
        private String ruleDescription;
        private String targetType;
        private String evaluationPeriod;
        private String awardName;
        private Boolean rankingEnabled;
        private Integer sortOrder;
    }

    @Data
    public static class UpdateRuleRequest {
        private String ruleName;
        private String ruleDescription;
        private String targetType;
        private String evaluationPeriod;
        private String awardName;
        private Boolean rankingEnabled;
        private Integer sortOrder;
    }

    /** 规则 + 等级列表的组合响应 */
    public record RuleWithLevels(EvaluationRule rule, List<EvaluationLevel> levels) {}
}
