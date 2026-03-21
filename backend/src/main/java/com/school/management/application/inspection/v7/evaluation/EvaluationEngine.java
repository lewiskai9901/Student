package com.school.management.application.inspection.v7.evaluation;

import com.school.management.domain.event.repository.EntityEventRepository;
import com.school.management.domain.inspection.model.v7.execution.InspSubmission;
import com.school.management.domain.inspection.model.v7.execution.InspTask;
import com.school.management.domain.inspection.model.v7.scoring.EvaluationCondition;
import com.school.management.domain.inspection.model.v7.scoring.EvaluationResult;
import com.school.management.domain.inspection.repository.v7.EvaluationResultRepository;
import com.school.management.domain.inspection.repository.v7.InspSubmissionRepository;
import com.school.management.domain.inspection.repository.v7.InspTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评选引擎 — 对各条件类型进行实际判定
 *
 * 支持的 conditionType：
 *   SCORE_AVG       平均分与 threshold 比较
 *   SCORE_MIN       最低单次分数
 *   SCORE_EVERY     每次分数都满足条件
 *   SCORE_COUNT     达标次数 >= threshold
 *   EVENT_COUNT     特定事件发生次数
 *   EVENT_ROLE      特定角色参与事件 (TODO: 需更详细事件结构)
 *   PREV_EVAL       上期评选结果 (TODO: 需扩展查询接口)
 *   RANK_PERCENTILE 排名百分位 (TODO: 需完整 rank 信息)
 *   TREND_IMPROVE   趋势改善幅度 (TODO: 复杂趋势计算)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationEngine {

    private final InspTaskRepository taskRepository;
    private final InspSubmissionRepository submissionRepository;
    private final EntityEventRepository entityEventRepository;
    private final EvaluationResultRepository resultRepository;

    /**
     * 判定单个条件是否满足
     */
    public boolean evaluateCondition(EvaluationCondition condition,
                                     Long targetId, String targetType,
                                     LocalDate cycleStart, LocalDate cycleEnd,
                                     Long projectId) {
        if (condition == null || condition.getConditionType() == null) {
            return false;
        }
        return switch (condition.getConditionType()) {
            case "SCORE_AVG"   -> evaluateScoreAvg(condition, targetId, cycleStart, cycleEnd, projectId);
            case "SCORE_MIN"   -> evaluateScoreMin(condition, targetId, cycleStart, cycleEnd, projectId);
            case "SCORE_EVERY" -> evaluateScoreEvery(condition, targetId, cycleStart, cycleEnd, projectId);
            case "SCORE_COUNT" -> evaluateScoreCount(condition, targetId, cycleStart, cycleEnd, projectId);
            case "EVENT_COUNT" -> evaluateEventCount(condition, targetId, targetType, cycleStart, cycleEnd);
            case "EVENT_ROLE"  -> evaluateEventRole(condition, targetId, targetType, cycleStart, cycleEnd);
            case "PREV_EVAL"   -> evaluatePrevEval(condition, targetId);
            case "RANK_PERCENTILE" -> {
                // TODO: 需要全量排名数据才能计算百分位，当前先返回 true（不阻断）
                log.debug("RANK_PERCENTILE 条件暂未完整实现，targetId={}", targetId);
                yield true;
            }
            case "TREND_IMPROVE" -> {
                // TODO: 需要跨周期趋势数据，当前先返回 true
                log.debug("TREND_IMPROVE 条件暂未完整实现，targetId={}", targetId);
                yield true;
            }
            default -> {
                log.warn("未知条件类型: {}", condition.getConditionType());
                yield false;
            }
        };
    }

    /**
     * 计算目标在周期内的平均得分（从所有任务的提交记录中汇总）
     */
    public BigDecimal computeAvgScore(Long targetId, LocalDate cycleStart, LocalDate cycleEnd,
                                      Long projectId) {
        List<BigDecimal> scores = getSubmissionScores(targetId, cycleStart, cycleEnd, projectId);
        if (scores.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(scores.size()), 4, RoundingMode.HALF_UP);
    }

    // ========== 私有评估方法 ==========

    private boolean evaluateScoreAvg(EvaluationCondition cond, Long targetId,
                                     LocalDate cycleStart, LocalDate cycleEnd, Long projectId) {
        List<BigDecimal> scores = getSubmissionScores(targetId, cycleStart, cycleEnd, projectId);
        if (scores.isEmpty()) return false;
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(scores.size()), 4, RoundingMode.HALF_UP);
        return compareWithOperator(avg, parseThreshold(cond.getThreshold()), cond.getOperator());
    }

    private boolean evaluateScoreMin(EvaluationCondition cond, Long targetId,
                                     LocalDate cycleStart, LocalDate cycleEnd, Long projectId) {
        List<BigDecimal> scores = getSubmissionScores(targetId, cycleStart, cycleEnd, projectId);
        if (scores.isEmpty()) return false;
        BigDecimal min = scores.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        return compareWithOperator(min, parseThreshold(cond.getThreshold()), cond.getOperator());
    }

    private boolean evaluateScoreEvery(EvaluationCondition cond, Long targetId,
                                       LocalDate cycleStart, LocalDate cycleEnd, Long projectId) {
        List<BigDecimal> scores = getSubmissionScores(targetId, cycleStart, cycleEnd, projectId);
        if (scores.isEmpty()) return false;
        BigDecimal threshold = parseThreshold(cond.getThreshold());
        return scores.stream().allMatch(s -> compareWithOperator(s, threshold, cond.getOperator()));
    }

    private boolean evaluateScoreCount(EvaluationCondition cond, Long targetId,
                                       LocalDate cycleStart, LocalDate cycleEnd, Long projectId) {
        List<BigDecimal> scores = getSubmissionScores(targetId, cycleStart, cycleEnd, projectId);
        if (scores.isEmpty()) return false;
        // 默认达标线：60 分，可从 parameters.passScore 覆盖
        BigDecimal passScore = getParameterDecimal(cond.getParameters(), "passScore", new BigDecimal("60"));
        long passCount = scores.stream()
                .filter(s -> s.compareTo(passScore) >= 0)
                .count();
        BigDecimal threshold = parseThreshold(cond.getThreshold());
        return compareWithOperator(BigDecimal.valueOf(passCount), threshold, cond.getOperator());
    }

    private boolean evaluateEventCount(EvaluationCondition cond, Long targetId,
                                       String targetType, LocalDate cycleStart, LocalDate cycleEnd) {
        // 从 EntityEventRepository 查询事件数量
        String eventType = getParameterString(cond.getParameters(), "eventType", null);
        List<com.school.management.domain.event.model.EntityEvent> events =
                entityEventRepository.findBySubject(targetType, targetId, 10000);
        long count = events.stream()
                .filter(e -> isWithinCycle(e.getOccurredAt() != null
                        ? e.getOccurredAt().toLocalDate()
                        : e.getCreatedAt().toLocalDate(), cycleStart, cycleEnd))
                .filter(e -> eventType == null || eventType.equals(e.getEventType()))
                .count();
        return compareWithOperator(BigDecimal.valueOf(count),
                parseThreshold(cond.getThreshold()), cond.getOperator());
    }

    private boolean evaluateEventRole(EvaluationCondition cond, Long targetId,
                                      String targetType, LocalDate cycleStart, LocalDate cycleEnd) {
        // TODO: 需要更详细的事件结构（角色字段）才能正确判断
        // 当前简化实现：查询事件数量 > 0 即认为满足
        log.debug("EVENT_ROLE 条件使用简化实现，targetId={}", targetId);
        String eventType = getParameterString(cond.getParameters(), "eventType", null);
        List<com.school.management.domain.event.model.EntityEvent> events =
                entityEventRepository.findBySubject(targetType, targetId, 10000);
        long count = events.stream()
                .filter(e -> isWithinCycle(e.getOccurredAt() != null
                        ? e.getOccurredAt().toLocalDate()
                        : e.getCreatedAt().toLocalDate(), cycleStart, cycleEnd))
                .filter(e -> eventType == null || eventType.equals(e.getEventType()))
                .count();
        return count > 0;
    }

    private boolean evaluatePrevEval(EvaluationCondition cond, Long targetId) {
        // 查询最近一次评选结果 level
        // TODO: EvaluationResultRepository 目前仅支持按 ruleId+cycleDate 查询，
        //       完整实现需扩展仓储接口。当前简化处理。
        log.debug("PREV_EVAL 条件暂未完整实现，targetId={}", targetId);
        return true;
    }

    // ========== 工具方法 ==========

    /**
     * 获取周期内某目标的所有 finalScore 列表
     */
    private List<BigDecimal> getSubmissionScores(Long targetId, LocalDate cycleStart,
                                                  LocalDate cycleEnd, Long projectId) {
        // 获取项目在周期内的所有任务
        List<InspTask> tasks = taskRepository.findByProjectIdAndTaskDateBetween(
                projectId, cycleStart, cycleEnd);

        return tasks.stream()
                .flatMap(task -> submissionRepository.findByTaskId(task.getId()).stream())
                .filter(s -> targetId.equals(s.getTargetId()))
                .map(InspSubmission::getFinalScore)
                .filter(score -> score != null)
                .collect(Collectors.toList());
    }

    /**
     * 比较 value operator threshold
     */
    private boolean compareWithOperator(BigDecimal value, BigDecimal threshold, String operator) {
        if (value == null || threshold == null || operator == null) return false;
        int cmp = value.compareTo(threshold);
        return switch (operator) {
            case "GTE" -> cmp >= 0;
            case "GT"  -> cmp > 0;
            case "LTE" -> cmp <= 0;
            case "LT"  -> cmp < 0;
            case "EQ"  -> cmp == 0;
            case "NEQ" -> cmp != 0;
            default -> false;
        };
    }

    private boolean isWithinCycle(LocalDate date, LocalDate start, LocalDate end) {
        return date != null && !date.isBefore(start) && !date.isAfter(end);
    }

    private BigDecimal parseThreshold(String threshold) {
        if (threshold == null || threshold.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(threshold);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getParameterDecimal(Map<String, Object> params, String key, BigDecimal defaultVal) {
        if (params == null || !params.containsKey(key)) return defaultVal;
        try {
            return new BigDecimal(params.get(key).toString());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private String getParameterString(Map<String, Object> params, String key, String defaultVal) {
        if (params == null || !params.containsKey(key)) return defaultVal;
        Object val = params.get(key);
        return val != null ? val.toString() : defaultVal;
    }
}
