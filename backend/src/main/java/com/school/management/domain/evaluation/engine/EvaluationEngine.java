package com.school.management.domain.evaluation.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.evaluation.model.*;
import com.school.management.domain.evaluation.repository.EvalBatchRepository;
import com.school.management.domain.evaluation.repository.EvalCampaignRepository;
import com.school.management.domain.evaluation.repository.EvalResultRepository;
import com.school.management.infrastructure.persistence.inspection.v7.execution.InspSubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评选执行引擎
 *
 * 流程:
 *   1. 加载活动配置 + 所有级别 + 所有条件
 *   2. 从 insp_submissions 表解析范围内目标列表
 *   3. 对每个目标: 从最高级别开始逐级评估，首个满足即为结果
 *   4. 按综合分排名
 *   5. 保存批次 + 结果
 */
@Slf4j
@Component("evalCenterEngine")
@RequiredArgsConstructor
public class EvaluationEngine {

    private final EvalCampaignRepository campaignRepository;
    private final EvalBatchRepository batchRepository;
    private final EvalResultRepository resultRepository;
    private final InspSubmissionMapper submissionMapper;
    private final ObjectMapper objectMapper;
    private final List<ConditionEvaluator> evaluators;

    @Transactional
    public EvalBatch execute(Long campaignId, LocalDate cycleStart, LocalDate cycleEnd, Long executedBy) {
        log.info("EvaluationEngine.execute: campaignId={}, cycle={}/{}", campaignId, cycleStart, cycleEnd);

        // 1. 加载活动配置（含级别和条件）
        EvalCampaign campaign = campaignRepository.findByIdWithLevels(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("评选活动不存在: " + campaignId));

        List<EvalLevel> levels = campaign.getLevels() != null ? campaign.getLevels() : Collections.emptyList();
        // 按 levelNum 升序（1=最高，先判断）
        levels = levels.stream().sorted(Comparator.comparingInt(EvalLevel::getLevelNum)).toList();

        // 2. 解析评估目标列表
        List<TargetInfo> targets = resolveTargets(campaign, cycleStart, cycleEnd);
        log.info("Resolved {} targets for campaign {}", targets.size(), campaignId);

        // 3. 对每个目标评估
        List<EvalResult> results = new ArrayList<>();
        for (TargetInfo target : targets) {
            EvalResult result = evaluateTarget(campaign, levels, target, cycleStart, cycleEnd);
            results.add(result);
        }

        // 4. 按综合分排名（分数越高排名越前；无分数排最后）
        results.sort((a, b) -> {
            if (a.getScore() == null && b.getScore() == null) return 0;
            if (a.getScore() == null) return 1;
            if (b.getScore() == null) return -1;
            return b.getScore().compareTo(a.getScore());
        });

        // 设置排名
        List<EvalResult> rankedResults = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            EvalResult r = results.get(i);
            rankedResults.add(EvalResult.builder()
                    .batchId(r.getBatchId())
                    .campaignId(r.getCampaignId())
                    .targetType(r.getTargetType())
                    .targetId(r.getTargetId())
                    .targetName(r.getTargetName())
                    .levelNum(r.getLevelNum())
                    .levelName(r.getLevelName())
                    .rankNo(i + 1)
                    .score(r.getScore())
                    .conditionDetails(r.getConditionDetails())
                    .upgradeHint(r.getUpgradeHint())
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // 5. 生成汇总
        String summary = buildSummary(rankedResults, levels);

        // 6. 保存批次
        EvalBatch batch = EvalBatch.builder()
                .campaignId(campaignId)
                .tenantId(0L)
                .cycleStart(cycleStart)
                .cycleEnd(cycleEnd)
                .totalTargets(rankedResults.size())
                .executedAt(LocalDateTime.now())
                .executedBy(executedBy)
                .status("COMPLETED")
                .summary(summary)
                .createdAt(LocalDateTime.now())
                .build();
        batchRepository.save(batch);

        // 补充 batchId 后保存结果
        final Long batchId = batch.getId();
        List<EvalResult> finalResults = rankedResults.stream().map(r ->
                EvalResult.builder()
                        .batchId(batchId)
                        .campaignId(r.getCampaignId())
                        .targetType(r.getTargetType())
                        .targetId(r.getTargetId())
                        .targetName(r.getTargetName())
                        .levelNum(r.getLevelNum())
                        .levelName(r.getLevelName())
                        .rankNo(r.getRankNo())
                        .score(r.getScore())
                        .conditionDetails(r.getConditionDetails())
                        .upgradeHint(r.getUpgradeHint())
                        .createdAt(LocalDateTime.now())
                        .build()
        ).toList();

        resultRepository.saveAll(finalResults);

        // 更新活动的 lastExecutedAt
        log.info("Batch {} saved with {} results", batchId, finalResults.size());
        return batch;
    }

    /**
     * 解析目标列表：从 insp_submissions 表中按 targetType 去重
     */
    private List<TargetInfo> resolveTargets(EvalCampaign campaign, LocalDate cycleStart, LocalDate cycleEnd) {
        String targetType = campaign.getTargetType();
        LocalDateTime startDt = cycleStart.atStartOfDay();
        LocalDateTime endDt = cycleEnd.plusDays(1).atStartOfDay();

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<
                com.school.management.infrastructure.persistence.inspection.v7.execution.InspSubmissionPO> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        qw.eq("target_type", targetType);
        qw.eq("deleted", 0);
        qw.eq("status", "COMPLETED");
        qw.ge("completed_at", startDt);
        qw.lt("completed_at", endDt);
        qw.select("DISTINCT target_id, target_name");

        return submissionMapper.selectList(qw).stream()
                .map(po -> new TargetInfo(po.getTargetId(), po.getTargetName()))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 评估单个目标
     */
    private EvalResult evaluateTarget(EvalCampaign campaign, List<EvalLevel> levels,
                                       TargetInfo target, LocalDate cycleStart, LocalDate cycleEnd) {
        List<Map<String, Object>> allConditionDetails = new ArrayList<>();
        Integer achievedLevelNum = null;
        String achievedLevelName = null;
        BigDecimal score = null;
        String upgradeHint = null;

        for (EvalLevel level : levels) {
            List<EvalCondition> conditions = level.getConditions() != null ? level.getConditions() : Collections.emptyList();
            List<ConditionResult> condResults = new ArrayList<>();

            for (EvalCondition condition : conditions) {
                ConditionEvaluator evaluator = findEvaluator(condition.getSourceType());
                ConditionResult cr;
                if (evaluator != null) {
                    cr = evaluator.evaluate(condition, target.targetId(), campaign.getTargetType(), cycleStart, cycleEnd);
                } else {
                    cr = ConditionResult.error("未找到评估器: " + condition.getSourceType());
                }
                condResults.add(cr);

                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("conditionId", condition.getId());
                detail.put("sourceType", condition.getSourceType());
                detail.put("metric", condition.getMetric());
                detail.put("operator", condition.getOperator());
                detail.put("threshold", condition.getThreshold());
                detail.put("passed", cr.passed());
                detail.put("actual", cr.actualValue());
                detail.put("description", cr.description() != null ? cr.description() : condition.getDescription());
                allConditionDetails.add(detail);
            }

            // 判断本级别是否满足（AND / OR）
            boolean levelPassed = evaluateLevelLogic(level.getConditionLogic(), condResults);
            if (levelPassed) {
                achievedLevelNum = level.getLevelNum();
                achievedLevelName = level.getLevelName();
                // 综合分：使用实际值的平均分作为排名依据（简化）
                score = computeScore(condResults);
                break;
            } else if (upgradeHint == null) {
                // 记录第一个未达标级别作为升级提示
                upgradeHint = buildUpgradeHint(level, conditions, condResults);
            }
        }

        String conditionDetailsJson = toJson(allConditionDetails);

        return EvalResult.builder()
                .campaignId(campaign.getId())
                .targetType(campaign.getTargetType())
                .targetId(target.targetId())
                .targetName(target.targetName())
                .levelNum(achievedLevelNum)
                .levelName(achievedLevelName)
                .score(score)
                .conditionDetails(conditionDetailsJson)
                .upgradeHint(upgradeHint)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private boolean evaluateLevelLogic(String logic, List<ConditionResult> condResults) {
        if (condResults.isEmpty()) return false;
        if ("OR".equalsIgnoreCase(logic)) {
            return condResults.stream().anyMatch(ConditionResult::passed);
        }
        // 默认 AND
        return condResults.stream().allMatch(ConditionResult::passed);
    }

    private BigDecimal computeScore(List<ConditionResult> condResults) {
        // 简化：通过率百分比作为分数
        long passCount = condResults.stream().filter(ConditionResult::passed).count();
        if (condResults.isEmpty()) return BigDecimal.ZERO;
        return BigDecimal.valueOf(passCount * 100.0 / condResults.size())
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String buildUpgradeHint(EvalLevel level, List<EvalCondition> conditions, List<ConditionResult> condResults) {
        StringBuilder sb = new StringBuilder("要达到【" + level.getLevelName() + "】，需满足：");
        for (int i = 0; i < conditions.size(); i++) {
            if (i < condResults.size() && !condResults.get(i).passed()) {
                EvalCondition c = conditions.get(i);
                ConditionResult cr = condResults.get(i);
                sb.append("\n· ").append(c.getDescription() != null ? c.getDescription() : c.getMetric())
                        .append("（当前: ").append(cr.actualValue()).append("）");
            }
        }
        return sb.toString();
    }

    private ConditionEvaluator findEvaluator(String sourceType) {
        return evaluators.stream()
                .filter(e -> e.supports(sourceType))
                .findFirst()
                .orElse(null);
    }

    private String buildSummary(List<EvalResult> results, List<EvalLevel> levels) {
        Map<String, Object> summary = new LinkedHashMap<>();
        for (EvalLevel level : levels) {
            long count = results.stream()
                    .filter(r -> level.getLevelNum().equals(r.getLevelNum()))
                    .count();
            summary.put("level" + level.getLevelNum() + "Count", count);
            summary.put("level" + level.getLevelNum() + "Name", level.getLevelName());
        }
        long noLevel = results.stream().filter(r -> r.getLevelNum() == null).count();
        summary.put("noLevelCount", noLevel);
        return toJson(summary);
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
