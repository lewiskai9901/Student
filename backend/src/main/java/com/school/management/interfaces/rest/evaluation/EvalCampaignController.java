package com.school.management.interfaces.rest.evaluation;

import com.school.management.application.evaluation.EvalCampaignApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.evaluation.model.*;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评选活动 REST 控制器
 */
@Slf4j
@RestController
@RequestMapping("/eval/campaigns")
@Tag(name = "评级中心 - 评选活动", description = "Evaluation Campaign Management API")
@RequiredArgsConstructor
public class EvalCampaignController {

    private final EvalCampaignApplicationService campaignService;

    // ==================== Campaign CRUD ====================

    @GetMapping
    @Operation(summary = "获取评选活动列表")
    @CasbinAccess(resource = "eval:campaign", action = "view")
    public Result<List<Map<String, Object>>> listCampaigns(
            @RequestParam(required = false) String status) {
        List<EvalCampaign> campaigns = campaignService.listCampaigns(status);
        List<Map<String, Object>> result = campaigns.stream()
                .map(this::toCampaignResponse)
                .collect(Collectors.toList());
        return Result.success(result);
    }

    @PostMapping
    @Operation(summary = "创建评选活动")
    @CasbinAccess(resource = "eval:campaign", action = "add")
    public Result<Map<String, Object>> createCampaign(@RequestBody CreateCampaignRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        EvalCampaign campaign = campaignService.createCampaign(
                req.campaignName(), req.targetType(), req.campaignDescription(),
                req.evaluationPeriod(), req.scopeOrgIds(), userId);
        return Result.success(toCampaignResponse(campaign));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取评选活动详情（含级别+条件）")
    @CasbinAccess(resource = "eval:campaign", action = "view")
    public Result<Map<String, Object>> getCampaign(@PathVariable Long id) {
        return campaignService.getCampaign(id)
                .map(c -> Result.success(toCampaignResponse(c)))
                .orElse(Result.error("评选活动不存在"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新评选活动")
    @CasbinAccess(resource = "eval:campaign", action = "edit")
    public Result<Map<String, Object>> updateCampaign(@PathVariable Long id,
                                                       @RequestBody UpdateCampaignRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        EvalCampaign campaign = campaignService.updateCampaign(id,
                req.campaignName(), req.campaignDescription(),
                req.evaluationPeriod(), req.scopeOrgIds(),
                req.status(), req.isAutoExecute(), userId);
        return Result.success(toCampaignResponse(campaign));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评选活动")
    @CasbinAccess(resource = "eval:campaign", action = "delete")
    public Result<Void> deleteCampaign(@PathVariable Long id) {
        campaignService.deleteCampaign(id);
        return Result.success();
    }

    // ==================== Levels ====================

    @GetMapping("/{id}/levels")
    @Operation(summary = "获取评选活动的级别列表（含条件）")
    @CasbinAccess(resource = "eval:campaign", action = "view")
    public Result<List<Map<String, Object>>> getLevels(@PathVariable Long id) {
        return campaignService.getLevels(id)
                .map(c -> {
                    List<Map<String, Object>> levels = c.getLevels() != null
                            ? c.getLevels().stream().map(this::toLevelResponse).collect(Collectors.toList())
                            : List.of();
                    return Result.success(levels);
                })
                .orElse(Result.error("评选活动不存在"));
    }

    @PutMapping("/{id}/levels")
    @Operation(summary = "批量保存级别+条件")
    @CasbinAccess(resource = "eval:campaign", action = "edit")
    public Result<Void> saveLevels(@PathVariable Long id,
                                    @RequestBody List<SaveLevelRequest> req) {
        List<EvalLevel> levels = req.stream().map(lr -> {
            List<EvalCondition> conditions = lr.conditions() != null
                    ? lr.conditions().stream().map(cr -> EvalCondition.builder()
                            .sourceType(cr.sourceType())
                            .sourceConfig(cr.sourceConfig())
                            .metric(cr.metric())
                            .operator(cr.operator())
                            .threshold(cr.threshold())
                            .scope(cr.scope())
                            .scopeRole(cr.scopeRole())
                            .timeRange(cr.timeRange())
                            .timeRangeDays(cr.timeRangeDays())
                            .description(cr.description())
                            .sortOrder(cr.sortOrder())
                            .build())
                    .collect(Collectors.toList())
                    : new ArrayList<>();

            return EvalLevel.builder()
                    .levelNum(lr.levelNum())
                    .levelName(lr.levelName())
                    .conditionLogic(lr.conditionLogic())
                    .sortOrder(lr.sortOrder())
                    .conditions(conditions)
                    .build();
        }).collect(Collectors.toList());

        campaignService.saveLevels(id, levels);
        return Result.success();
    }

    // ==================== Execute ====================

    @PostMapping("/{id}/execute")
    @Operation(summary = "执行评选")
    @CasbinAccess(resource = "eval:campaign", action = "edit")
    public Result<Map<String, Object>> executeCampaign(@PathVariable Long id,
                                                        @RequestBody ExecuteRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        EvalBatch batch = campaignService.executeCampaign(id, req.cycleStart(), req.cycleEnd(), userId);
        return Result.success(toBatchResponse(batch));
    }

    @GetMapping("/{id}/batches")
    @Operation(summary = "获取执行历史")
    @CasbinAccess(resource = "eval:campaign", action = "view")
    public Result<List<Map<String, Object>>> listBatches(@PathVariable Long id) {
        List<EvalBatch> batches = campaignService.listBatches(id);
        List<Map<String, Object>> result = batches.stream()
                .map(this::toBatchResponse)
                .collect(Collectors.toList());
        return Result.success(result);
    }

    // ==================== Request Records ====================

    public record CreateCampaignRequest(
            String campaignName,
            String targetType,
            String campaignDescription,
            String evaluationPeriod,
            String scopeOrgIds
    ) {}

    public record UpdateCampaignRequest(
            String campaignName,
            String campaignDescription,
            String evaluationPeriod,
            String scopeOrgIds,
            String status,
            Boolean isAutoExecute
    ) {}

    public record SaveLevelRequest(
            Integer levelNum,
            String levelName,
            String conditionLogic,
            Integer sortOrder,
            List<SaveConditionRequest> conditions
    ) {}

    public record SaveConditionRequest(
            String sourceType,
            String sourceConfig,
            String metric,
            String operator,
            String threshold,
            String scope,
            String scopeRole,
            String timeRange,
            Integer timeRangeDays,
            String description,
            Integer sortOrder
    ) {}

    public record ExecuteRequest(
            LocalDate cycleStart,
            LocalDate cycleEnd
    ) {}

    // ==================== Response Helpers ====================

    private Map<String, Object> toCampaignResponse(EvalCampaign c) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("campaignName", c.getCampaignName());
        m.put("campaignDescription", c.getCampaignDescription());
        m.put("targetType", c.getTargetType());
        m.put("scopeOrgIds", c.getScopeOrgIds());
        m.put("evaluationPeriod", c.getEvaluationPeriod());
        m.put("status", c.getStatus());
        m.put("isAutoExecute", c.getIsAutoExecute());
        m.put("lastExecutedAt", c.getLastExecutedAt());
        m.put("nextExecuteAt", c.getNextExecuteAt());
        m.put("sortOrder", c.getSortOrder());
        m.put("createdAt", c.getCreatedAt());
        m.put("updatedAt", c.getUpdatedAt());
        if (c.getLevels() != null) {
            m.put("levels", c.getLevels().stream().map(this::toLevelResponse).collect(Collectors.toList()));
        }
        return m;
    }

    private Map<String, Object> toLevelResponse(EvalLevel l) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("campaignId", l.getCampaignId());
        m.put("levelNum", l.getLevelNum());
        m.put("levelName", l.getLevelName());
        m.put("conditionLogic", l.getConditionLogic());
        m.put("sortOrder", l.getSortOrder());
        if (l.getConditions() != null) {
            m.put("conditions", l.getConditions().stream().map(this::toConditionResponse).collect(Collectors.toList()));
        }
        return m;
    }

    private Map<String, Object> toConditionResponse(EvalCondition c) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("levelId", c.getLevelId());
        m.put("sourceType", c.getSourceType());
        m.put("sourceConfig", c.getSourceConfig());
        m.put("metric", c.getMetric());
        m.put("operator", c.getOperator());
        m.put("threshold", c.getThreshold());
        m.put("scope", c.getScope());
        m.put("scopeRole", c.getScopeRole());
        m.put("timeRange", c.getTimeRange());
        m.put("timeRangeDays", c.getTimeRangeDays());
        m.put("description", c.getDescription());
        m.put("sortOrder", c.getSortOrder());
        return m;
    }

    private Map<String, Object> toBatchResponse(EvalBatch b) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("campaignId", b.getCampaignId());
        m.put("cycleStart", b.getCycleStart());
        m.put("cycleEnd", b.getCycleEnd());
        m.put("totalTargets", b.getTotalTargets());
        m.put("executedAt", b.getExecutedAt());
        m.put("executedBy", b.getExecutedBy());
        m.put("status", b.getStatus());
        m.put("summary", b.getSummary());
        return m;
    }
}
