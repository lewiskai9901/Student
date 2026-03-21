package com.school.management.domain.evaluation.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.evaluation.model.EvalCondition;
import com.school.management.domain.evaluation.model.EvalResult;
import com.school.management.domain.evaluation.repository.EvalResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 基于评选历史的条件评估器
 * sourceType = HISTORY
 *
 * source_config: { "campaignId": null }  (null = 本活动)
 * 指标: PREV_LEVEL / CONSECUTIVE / RANK_PERCENTILE / TREND
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryEvaluator implements ConditionEvaluator {

    private final EvalResultRepository evalResultRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String sourceType) {
        return "HISTORY".equals(sourceType);
    }

    @Override
    public ConditionResult evaluate(EvalCondition condition, Long targetId, String targetType,
                                    LocalDate cycleStart, LocalDate cycleEnd) {
        try {
            JsonNode config = objectMapper.readTree(condition.getSourceConfig());
            Long campaignId = config.has("campaignId") && !config.get("campaignId").isNull()
                    ? config.get("campaignId").asLong()
                    : null;

            // campaignId null 时需要从 condition 的 levelId 反查 campaignId，
            // 这里先用0表示"占位"，实际在 EvaluationEngine 中会传入正确的 campaignId
            if (campaignId == null) {
                // 无法确定 campaignId，返回 error（Engine 层会在调用前设置）
                return ConditionResult.error("HISTORY 条件缺少 campaignId，请在 sourceConfig 中指定");
            }

            String metric = condition.getMetric();
            String operator = condition.getOperator();
            String thresholdStr = condition.getThreshold();

            // 获取历史结果（最近10期）
            List<EvalResult> history = evalResultRepository.findRecentByTarget(campaignId, targetId, 10);

            return switch (metric) {
                case "PREV_LEVEL" -> {
                    // 上期级别编号与阈值比较
                    if (history.isEmpty()) {
                        yield ConditionResult.fail("无历史", "没有历史评选记录");
                    }
                    EvalResult last = history.get(0);
                    Integer levelNum = last.getLevelNum();
                    if (levelNum == null) {
                        yield ConditionResult.fail("未达级别", "上期未达任何级别");
                    }
                    BigDecimal thresh = new BigDecimal(thresholdStr);
                    boolean passed = compare(BigDecimal.valueOf(levelNum), operator, thresh);
                    yield new ConditionResult(passed, String.valueOf(levelNum),
                            "上期级别 " + levelNum + " " + operator + " " + thresholdStr);
                }
                case "CONSECUTIVE" -> {
                    // 连续达标期数（level_num <= threshold，级别越小越高）
                    BigDecimal thresh = new BigDecimal(thresholdStr);
                    int consecutive = 0;
                    for (EvalResult r : history) {
                        if (r.getLevelNum() != null) {
                            consecutive++;
                        } else {
                            break;
                        }
                    }
                    boolean passed = compare(BigDecimal.valueOf(consecutive), operator, thresh);
                    yield new ConditionResult(passed, String.valueOf(consecutive),
                            "连续达标期数 " + consecutive + " " + operator + " " + thresholdStr);
                }
                case "RANK_PERCENTILE" -> {
                    if (history.isEmpty()) {
                        yield ConditionResult.fail("无历史", "没有历史评选记录");
                    }
                    EvalResult last = history.get(0);
                    Integer rankNo = last.getRankNo();
                    if (rankNo == null) {
                        yield ConditionResult.fail("未排名", "上期未参与排名");
                    }
                    // 简化：直接用排名作为百分位近似（需要知道总数，暂用排名本身）
                    BigDecimal thresh = new BigDecimal(thresholdStr);
                    boolean passed = compare(BigDecimal.valueOf(rankNo), operator, thresh);
                    yield new ConditionResult(passed, String.valueOf(rankNo),
                            "上期排名 " + rankNo + " " + operator + " " + thresholdStr);
                }
                case "TREND" -> {
                    if (history.size() < 2) {
                        yield ConditionResult.fail("数据不足", "需要至少2期历史数据");
                    }
                    EvalResult latest = history.get(0);
                    EvalResult prev = history.get(1);
                    if (latest.getScore() == null || prev.getScore() == null) {
                        yield ConditionResult.fail("无分数", "历史记录缺少综合分");
                    }
                    BigDecimal diff = latest.getScore().subtract(prev.getScore());
                    BigDecimal thresh = new BigDecimal(thresholdStr);
                    boolean passed = compare(diff, operator, thresh);
                    yield new ConditionResult(passed, diff.toPlainString(),
                            "分数趋势 " + diff.toPlainString() + " " + operator + " " + thresholdStr);
                }
                default -> ConditionResult.error("不支持的历史指标: " + metric);
            };

        } catch (Exception e) {
            log.error("HistoryEvaluator error: {}", e.getMessage(), e);
            return ConditionResult.error("评估异常: " + e.getMessage());
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
}
