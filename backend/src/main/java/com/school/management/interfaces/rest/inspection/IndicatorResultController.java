package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.evaluation.IndicatorEvaluationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.evaluation.IndicatorResult;
import com.school.management.domain.inspection.model.evaluation.ResultStatus;
import com.school.management.domain.inspection.repository.IndicatorResultRepository;
import com.school.management.infrastructure.casbin.CasbinAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评级结果控制器 — 评级引擎完美架构 Phase 4 (2026-05-23).
 *
 * <p>暴露 {@link IndicatorResult} 的查询 / 修订链 / 手动 publish / 手动评估端点.
 */
@Slf4j
@RestController
@RequestMapping("/inspection/indicator-results")
@RequiredArgsConstructor
public class IndicatorResultController {

    private final IndicatorEvaluationService evaluationService;
    private final IndicatorResultRepository resultRepository;

    /**
     * 列表查询 (按 indicator + 可选 target / periodKey / status 过滤).
     * 数据权限由 mapper 层 {@code @DataPermission} + Filler 注入的 org_unit_id 控制 (Phase 1+5).
     */
    @GetMapping
    @CasbinAccess(resource = "insp:result", action = "view")
    public Result<List<IndicatorResult>> list(
            @RequestParam Long indicatorId,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String periodKey,
            @RequestParam(required = false) ResultStatus status) {
        List<IndicatorResult> all = (status != null)
                ? resultRepository.findByIndicatorIdAndStatus(indicatorId, status)
                : resultRepository.findByIndicatorId(indicatorId);

        List<IndicatorResult> filtered = all.stream()
                .filter(r -> targetId == null || targetId.equals(r.getTargetId()))
                .filter(r -> periodKey == null || periodKey.equals(r.getPeriodKey()))
                .collect(Collectors.toList());
        return Result.success(filtered);
    }

    /**
     * 修订链查询: 给定任一结果 id, 返回 (indicator, target, period) 三元组下的整条链
     * (按 computedAt ASC, 头条是最早版本).
     */
    @GetMapping("/{id}/history")
    @CasbinAccess(resource = "insp:result", action = "view")
    public Result<List<IndicatorResult>> history(@PathVariable Long id) {
        IndicatorResult anchor = resultRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("IndicatorResult 不存在: " + id));
        return Result.success(resultRepository.findRevisionHistory(
                anchor.getIndicatorId(), anchor.getTargetId(), anchor.getPeriodKey()));
    }

    /** 单条 DRAFT → PUBLISHED. */
    @PostMapping("/{id}/publish")
    @CasbinAccess(resource = "insp:result", action = "edit")
    public Result<IndicatorResult> publish(@PathVariable Long id) {
        return Result.success(evaluationService.publish(id));
    }

    /**
     * 手动评估入口 — MANUAL trigger. periodKey 由 service 内部按 startDate/endDate 生成.
     */
    @PostMapping("/manual-evaluate")
    @CasbinAccess(resource = "insp:result", action = "create")
    public Result<List<IndicatorResult>> manualEvaluate(@RequestBody @Valid ManualEvaluateRequest request) {
        List<IndicatorResult> drafts = evaluationService.evaluateManual(
                request.getIndicatorId(), request.getStartDate(), request.getEndDate());
        return Result.success(drafts);
    }

    @lombok.Data
    public static class ManualEvaluateRequest {
        @NotNull(message = "indicatorId 必填")
        private Long indicatorId;
        @NotNull(message = "startDate 必填")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startDate;
        @NotNull(message = "endDate 必填")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate endDate;

        @jakarta.validation.constraints.AssertTrue(message = "endDate 不得早于 startDate")
        public boolean isDateRangeValid() {
            if (startDate == null || endDate == null) return true;
            return !endDate.isBefore(startDate);
        }
    }
}
