package com.school.management.domain.inspection.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.execution.ScoringMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 服务端权威的逐项评分计算器 —— 唯一按评分模式算"单项原始分"的地方.
 *
 * <p>本类实现 8 种<b>确定性</b>评分模式: DEDUCTION / ADDITION / CUMULATIVE / DIRECT /
 * PASS_FAIL / LEVEL / SCORE_TABLE / RATING_SCALE. 复杂模式
 * (WEIGHTED_MULTI / RISK_MATRIX / THRESHOLD / FORMULA / TIERED_DEDUCTION)
 * 为后续任务, 当前先返回 0.
 *
 * <p>契约依据:
 * <ul>
 *   <li>前端 {@code TaskExecutionView.vue} 各 handler 的 responseValue 写入约定</li>
 *   <li>前端 {@code ItemEditor.vue#serializeScoringConfig} 的 scoringConfig 字段名</li>
 *   <li>后端 {@code ScoreAggregationService.parseConfigScore}: score → configScore → baseScore</li>
 * </ul>
 *
 * <p>Jackson Long→string 全局策略下, scoringConfig 里的数值可能是 JSON string 也可能是
 * number, 统一用 {@code node.asText()} 再 {@code new BigDecimal} 容错解析.
 */
@Slf4j
@Service
public class ItemScoreEvaluator {

    private final ObjectMapper om = new ObjectMapper();

    /**
     * 按评分模式算单项原始分 (扣分为负). 无法判定 / 缺配置 → 0.
     *
     * @param mode             ScoringMode 枚举
     * @param responseValue    检查员原始响应字符串
     * @param scoringConfigJson item 的 scoringConfig JSON (可空)
     * @return 单项原始分, 永不为 null
     */
    public BigDecimal scoreItem(ScoringMode mode, String responseValue, String scoringConfigJson) {
        if (mode == null) {
            return BigDecimal.ZERO;
        }
        JsonNode cfg = parseConfig(scoringConfigJson);
        try {
            switch (mode) {
                case DEDUCTION:
                    return deductionOrAddition(cfg, responseValue, true);
                case ADDITION:
                    return deductionOrAddition(cfg, responseValue, false);
                case CUMULATIVE:
                    return cumulative(cfg, responseValue);
                case DIRECT:
                    return direct(responseValue);
                case PASS_FAIL:
                    return passFail(cfg, responseValue);
                case LEVEL:
                case SCORE_TABLE:
                    return lookupByLabel(cfg, responseValue);
                case RATING_SCALE:
                    return ratingScale(cfg, responseValue);
                // 复杂模式: 后续任务实现, 当前先返回 0
                case WEIGHTED_MULTI:
                case RISK_MATRIX:
                case THRESHOLD:
                case FORMULA:
                case TIERED_DEDUCTION:
                default:
                    return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.debug("scoreItem 计算失败 mode={} response={}: {}", mode, responseValue, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // ==================== DEDUCTION / ADDITION ====================
    // responseValue = 数量; configScore 从 score/configScore/baseScore 取;
    // 分 = |configScore| * 数量; DEDUCTION 取负.

    private BigDecimal deductionOrAddition(JsonNode cfg, String responseValue, boolean negate) {
        BigDecimal quantity = parseNumeric(responseValue);
        if (quantity == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal configScore = parseConfigScore(cfg).abs();
        BigDecimal result = configScore.multiply(quantity);
        return negate ? result.negate() : result;
    }

    // ==================== CUMULATIVE ====================
    // responseValue = 次数; 分 = scorePerUnit * 次数.
    // 前端 ItemEditor.vue 实际序列化字段名为 scorePerCount, TaskExecutionView 读 scorePerUnit,
    // 两者都容错.

    private BigDecimal cumulative(JsonNode cfg, String responseValue) {
        BigDecimal count = parseNumeric(responseValue);
        if (count == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal perUnit = firstNumeric(cfg, "scorePerUnit", "scorePerCount", "score", "configScore", "baseScore");
        if (perUnit == null) {
            return BigDecimal.ZERO;
        }
        return perUnit.multiply(count);
    }

    // ==================== DIRECT ====================
    // responseValue 即分.

    private BigDecimal direct(String responseValue) {
        BigDecimal v = parseNumeric(responseValue);
        return v == null ? BigDecimal.ZERO : v;
    }

    // ==================== PASS_FAIL ====================
    // responseValue = 'PASS'/'FAIL'; PASS → passScore(默认 0), FAIL → failScore(默认 -5).
    // 空响应 → 0.

    private BigDecimal passFail(JsonNode cfg, String responseValue) {
        if (responseValue == null || responseValue.isBlank()) {
            return BigDecimal.ZERO;
        }
        String v = responseValue.trim().toUpperCase();
        if ("PASS".equals(v)) {
            BigDecimal pass = firstNumeric(cfg, "passScore");
            return pass == null ? BigDecimal.ZERO : pass;
        }
        if ("FAIL".equals(v)) {
            BigDecimal fail = firstNumeric(cfg, "failScore");
            return fail == null ? new BigDecimal("-5") : fail;
        }
        return BigDecimal.ZERO;
    }

    // ==================== LEVEL / SCORE_TABLE ====================
    // responseValue = 等级 label; 在等级数组里按 label 查 score.
    // LEVEL 数组字段名 levels, SCORE_TABLE 数组字段名 options (ItemEditor.vue 实证);
    // 元素结构 {label, score}. 两个字段名都尝试.

    private BigDecimal lookupByLabel(JsonNode cfg, String responseValue) {
        if (responseValue == null || responseValue.isBlank()) {
            return BigDecimal.ZERO;
        }
        String target = responseValue.trim();
        JsonNode arr = cfg.has("levels") ? cfg.get("levels")
                : cfg.has("options") ? cfg.get("options")
                : null;
        if (arr == null || !arr.isArray()) {
            return BigDecimal.ZERO;
        }
        for (JsonNode el : arr) {
            JsonNode labelNode = el.get("label");
            if (labelNode != null && target.equals(labelNode.asText())) {
                JsonNode scoreNode = el.get("score");
                if (scoreNode == null || scoreNode.isNull()) {
                    return BigDecimal.ZERO;
                }
                return toBigDecimal(scoreNode.asText());
            }
        }
        return BigDecimal.ZERO;
    }

    // ==================== RATING_SCALE ====================
    // responseValue = 星数; 分 = round(stars/maxStars*maxScore, HALF_UP).
    // maxStars = maxStars ?? maxRating ?? 5; maxScore = maxScore ?? 100.

    private BigDecimal ratingScale(JsonNode cfg, String responseValue) {
        BigDecimal stars = parseNumeric(responseValue);
        if (stars == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal maxStars = firstNumeric(cfg, "maxStars", "maxRating");
        if (maxStars == null) {
            maxStars = new BigDecimal("5");
        }
        if (maxStars.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal maxScore = firstNumeric(cfg, "maxScore");
        if (maxScore == null) {
            maxScore = new BigDecimal("100");
        }
        return stars.multiply(maxScore)
                .divide(maxStars, 0, RoundingMode.HALF_UP);
    }

    // ==================== helpers ====================

    private JsonNode parseConfig(String json) {
        if (json == null || json.isBlank()) {
            return om.createObjectNode();
        }
        try {
            return om.readTree(json);
        } catch (Exception e) {
            log.debug("解析 scoringConfig 失败: {}", e.getMessage());
            return om.createObjectNode();
        }
    }

    /** 与 ScoreAggregationService.parseConfigScore 约定一致: score → configScore → baseScore. */
    private BigDecimal parseConfigScore(JsonNode cfg) {
        BigDecimal v = firstNumeric(cfg, "score", "configScore", "baseScore");
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 按字段名顺序取第一个可解析为数值的字段, 数值可能是 JSON string 或 number. */
    private BigDecimal firstNumeric(JsonNode cfg, String... keys) {
        for (String key : keys) {
            JsonNode node = cfg.get(key);
            if (node != null && !node.isNull()) {
                BigDecimal v = toBigDecimal(node.asText());
                if (v != null) {
                    return v;
                }
            }
        }
        return null;
    }

    /** 解析 responseValue 为数值, 不可解析返回 null. */
    private BigDecimal parseNumeric(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return toBigDecimal(value.trim());
    }

    private BigDecimal toBigDecimal(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
