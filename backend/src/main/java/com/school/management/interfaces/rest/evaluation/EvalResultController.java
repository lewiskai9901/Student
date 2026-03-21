package com.school.management.interfaces.rest.evaluation;

import com.school.management.common.result.Result;
import com.school.management.domain.evaluation.model.EvalResult;
import com.school.management.domain.evaluation.repository.EvalResultRepository;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评选结果 REST 控制器
 */
@RestController
@Tag(name = "评级中心 - 评选结果", description = "Evaluation Result API")
@RequiredArgsConstructor
public class EvalResultController {

    private final EvalResultRepository evalResultRepository;

    @GetMapping("/eval/batches/{batchId}/results")
    @Operation(summary = "获取批次结果列表")
    @CasbinAccess(resource = "eval:campaign", action = "view")
    public Result<List<Map<String, Object>>> getBatchResults(@PathVariable Long batchId) {
        List<EvalResult> results = evalResultRepository.findByBatchId(batchId);
        return Result.success(results.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/eval/results/target/{type}/{id}")
    @Operation(summary = "获取某目标的评选历史")
    @CasbinAccess(resource = "eval:campaign", action = "view")
    public Result<List<Map<String, Object>>> getTargetHistory(
            @PathVariable String type,
            @PathVariable Long id,
            @RequestParam(required = false) Long campaignId) {
        List<EvalResult> results;
        if (campaignId != null) {
            results = evalResultRepository.findByCampaignAndTarget(campaignId, type, id);
        } else {
            results = evalResultRepository.findRecentByTarget(0L, id, 20);
        }
        return Result.success(results.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    private Map<String, Object> toResponse(EvalResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("batchId", r.getBatchId());
        m.put("campaignId", r.getCampaignId());
        m.put("targetType", r.getTargetType());
        m.put("targetId", r.getTargetId());
        m.put("targetName", r.getTargetName());
        m.put("levelNum", r.getLevelNum());
        m.put("levelName", r.getLevelName());
        m.put("rankNo", r.getRankNo());
        m.put("score", r.getScore());
        m.put("conditionDetails", r.getConditionDetails());
        m.put("upgradeHint", r.getUpgradeHint());
        m.put("createdAt", r.getCreatedAt());
        return m;
    }
}
