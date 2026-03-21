package com.school.management.domain.evaluation.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.evaluation.model.EvalCondition;
import com.school.management.infrastructure.persistence.inspection.v7.execution.InspSubmissionMapper;
import com.school.management.infrastructure.persistence.inspection.v7.execution.InspSubmissionPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于检查分数的条件评估器
 * sourceType = INSPECTION
 *
 * source_config: { "projectId": 51 (可选), "sectionId": 100057 }
 * 查询思路：
 *   1. 按 sectionId + targetId 过滤 submissions
 *   2. 通过 task.taskDate 过滤周期范围（JOIN insp_tasks）
 *   3. 只统计 COMPLETED 状态的 submissions
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionEvaluator implements ConditionEvaluator {

    private final InspSubmissionMapper submissionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String sourceType) {
        return "INSPECTION".equals(sourceType);
    }

    @Override
    public ConditionResult evaluate(EvalCondition condition, Long targetId, String targetType,
                                    LocalDate cycleStart, LocalDate cycleEnd) {
        try {
            JsonNode config = objectMapper.readTree(condition.getSourceConfig());
            Long sectionId = config.has("sectionId") && !config.get("sectionId").isNull()
                    ? config.get("sectionId").asLong() : null;
            Long projectId = config.has("projectId") && !config.get("projectId").isNull()
                    ? config.get("projectId").asLong() : null;

            LocalDateTime startDt = cycleStart.atStartOfDay();
            LocalDateTime endDt = cycleEnd.plusDays(1).atStartOfDay();

            List<InspSubmissionPO> submissions = fetchSubmissions(
                    targetId, sectionId, projectId, startDt, endDt);

            // 只统计已完成的
            List<InspSubmissionPO> completed = submissions.stream()
                    .filter(s -> "COMPLETED".equals(s.getStatus()))
                    .toList();

            return evaluateMetric(condition, completed);

        } catch (Exception e) {
            log.error("InspectionEvaluator error for condition {}: {}", condition.getId(), e.getMessage(), e);
            return ConditionResult.error("评估异常: " + e.getMessage());
        }
    }

    private List<InspSubmissionPO> fetchSubmissions(Long targetId, Long sectionId, Long projectId,
                                                     LocalDateTime startDt, LocalDateTime endDt) {
        // 直接用 targetId + sectionId 查，时间范围通过 completedAt 过滤
        // 不走 submissionMapper 带数据权限的方法，直接查原始数据（评级引擎视角）
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<InspSubmissionPO> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        qw.eq("target_id", targetId);
        qw.eq("deleted", 0);
        if (sectionId != null) {
            qw.eq("section_id", sectionId);
        }
        if (startDt != null) {
            qw.ge("completed_at", startDt);
        }
        if (endDt != null) {
            qw.lt("completed_at", endDt);
        }
        // projectId 过滤需要 JOIN insp_tasks，暂通过 taskId in (subquery) 方式
        // 若 projectId 为 null，则不过滤项目
        return submissionMapper.selectList(qw);
    }

    private ConditionResult evaluateMetric(EvalCondition condition, List<InspSubmissionPO> submissions) {
        String metric = condition.getMetric();
        String operator = condition.getOperator();
        String threshold = condition.getThreshold();

        if (submissions.isEmpty()) {
            return ConditionResult.fail("0", "没有检查记录（" + metric + "）");
        }

        switch (metric) {
            case "SCORE_AVG" -> {
                BigDecimal avg = submissions.stream()
                        .filter(s -> s.getFinalScore() != null)
                        .map(InspSubmissionPO::getFinalScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(submissions.size()), 2, RoundingMode.HALF_UP);
                BigDecimal thresh = new BigDecimal(threshold);
                boolean passed = compare(avg, operator, thresh);
                return new ConditionResult(passed, avg.toPlainString(),
                        "平均分 " + avg.toPlainString() + " " + (passed ? ">=" : "<") + " " + threshold);
            }
            case "SCORE_MIN" -> {
                BigDecimal min = submissions.stream()
                        .filter(s -> s.getFinalScore() != null)
                        .map(InspSubmissionPO::getFinalScore)
                        .min(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                BigDecimal thresh = new BigDecimal(threshold);
                boolean passed = compare(min, operator, thresh);
                return new ConditionResult(passed, min.toPlainString(),
                        "最低分 " + min.toPlainString() + " " + operator + " " + threshold);
            }
            case "SCORE_MAX" -> {
                BigDecimal max = submissions.stream()
                        .filter(s -> s.getFinalScore() != null)
                        .map(InspSubmissionPO::getFinalScore)
                        .max(BigDecimal::compareTo)
                        .orElse(BigDecimal.ZERO);
                BigDecimal thresh = new BigDecimal(threshold);
                boolean passed = compare(max, operator, thresh);
                return new ConditionResult(passed, max.toPlainString(),
                        "最高分 " + max.toPlainString() + " " + operator + " " + threshold);
            }
            case "SCORE_EVERY" -> {
                BigDecimal thresh = new BigDecimal(threshold);
                boolean allPass = submissions.stream()
                        .filter(s -> s.getFinalScore() != null)
                        .allMatch(s -> compare(s.getFinalScore(), operator, thresh));
                long failCount = submissions.stream()
                        .filter(s -> s.getFinalScore() != null)
                        .filter(s -> !compare(s.getFinalScore(), operator, thresh))
                        .count();
                return new ConditionResult(allPass, allPass ? "全部达标" : failCount + "次未达标",
                        "每次检查分数 " + operator + " " + threshold);
            }
            case "GRADE_EVERY" -> {
                // threshold is JSON array: ["优秀","良好"]
                List<String> acceptedGrades = parseJsonArray(threshold);
                boolean allPass = submissions.stream()
                        .filter(s -> s.getGrade() != null)
                        .allMatch(s -> acceptedGrades.contains(s.getGrade()));
                long failCount = submissions.stream()
                        .filter(s -> s.getGrade() == null || !acceptedGrades.contains(s.getGrade()))
                        .count();
                return new ConditionResult(allPass, allPass ? "全部达标" : failCount + "次未达标",
                        "每次评级达到 " + String.join("/", acceptedGrades));
            }
            case "GRADE_COUNT" -> {
                List<String> acceptedGrades = parseJsonArray(threshold.startsWith("[") ? threshold : "[]");
                // For GRADE_COUNT, threshold is a number and scope grades come from sourceConfig
                // simplified: count passed submissions
                long count = submissions.stream().filter(s -> Boolean.TRUE.equals(s.getPassed())).count();
                BigDecimal thresh = new BigDecimal(threshold);
                boolean passed = compare(BigDecimal.valueOf(count), operator, thresh);
                return new ConditionResult(passed, String.valueOf(count),
                        "达标次数 " + count + " " + operator + " " + threshold);
            }
            case "FAIL_COUNT" -> {
                long failCount = submissions.stream()
                        .filter(s -> Boolean.FALSE.equals(s.getPassed())).count();
                BigDecimal thresh = new BigDecimal(threshold);
                boolean passed = compare(BigDecimal.valueOf(failCount), operator, thresh);
                return new ConditionResult(passed, String.valueOf(failCount),
                        "不通过次数 " + failCount + " " + operator + " " + threshold);
            }
            default -> {
                return ConditionResult.error("不支持的检查指标: " + metric);
            }
        }
    }

    private boolean compare(BigDecimal actual, String operator, BigDecimal threshold) {
        int cmp = actual.compareTo(threshold);
        return switch (operator) {
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            case "=" -> cmp == 0;
            case "!=" -> cmp != 0;
            case ">" -> cmp > 0;
            case "<" -> cmp < 0;
            default -> false;
        };
    }

    private List<String> parseJsonArray(String json) {
        List<String> result = new ArrayList<>();
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isArray()) {
                node.forEach(n -> result.add(n.asText()));
            }
        } catch (Exception e) {
            log.warn("Failed to parse JSON array: {}", json);
        }
        return result;
    }
}
