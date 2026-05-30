package com.school.management.domain.inspection.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.correction.SeverityNormalizer;
import com.school.management.domain.inspection.model.execution.ScoringMode;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务端权威的逐项评分计算器 —— 唯一按评分模式算"单项原始分"的地方.
 *
 * <p>本类实现全部 13 种评分模式:
 * <ul>
 *   <li>确定性: DEDUCTION / ADDITION / CUMULATIVE / DIRECT / PASS_FAIL / LEVEL /
 *       SCORE_TABLE / RATING_SCALE / TIERED_DEDUCTION</li>
 *   <li>复杂 (复用 {@link SeverityNormalizer}): WEIGHTED_MULTI / RISK_MATRIX</li>
 *   <li>阈值 (本类内联实现): THRESHOLD</li>
 *   <li>公式 (复用 {@link FormulaEvaluator}): FORMULA</li>
 * </ul>
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
 *
 * <p><b>复杂模式 severity→分换算语义 (待确认假设)</b>: WEIGHTED_MULTI / RISK_MATRIX 的
 * normalizer 产出 [0,1] severity (越高越严重). cfg 仅含 {@code maxScore} (得分语义) 时,
 * 本类假设按"扣分语义"换算 {@code score = -(severity × maxScore)} (越严重扣越多). 若产品
 * 实际希望得分语义 (越严重得分越低, 即 {@code (1-severity) × maxScore}), 需调整 complex().
 */
@Slf4j
@Service
public class ItemScoreEvaluator {

    private final ObjectMapper om = new ObjectMapper();

    /** 公式求值器 — 可选注入. 缺失时 FORMULA 模式退化为 0 (不崩). */
    private final FormulaEvaluator formulaEvaluator;

    @Autowired
    public ItemScoreEvaluator(FormulaEvaluator formulaEvaluator) {
        this.formulaEvaluator = formulaEvaluator;
    }

    /** 无 FormulaEvaluator 构造 (单测 / FORMULA 不可用场景). */
    public ItemScoreEvaluator() {
        this(null);
    }

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
                case TIERED_DEDUCTION:
                    return tieredDeduction(cfg, responseValue);
                case WEIGHTED_MULTI:
                case RISK_MATRIX:
                    return complex(mode, cfg, responseValue, scoringConfigJson);
                case THRESHOLD:
                    return threshold(cfg, responseValue);
                case FORMULA:
                    return formula(cfg, responseValue);
                default:
                    return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.debug("scoreItem 计算失败 mode={} response={}: {}", mode, responseValue, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // ==================== DEDUCTION / ADDITION ====================
    // 直接取值语义 (产品确认): responseValue 是检查员在区间内直接拨的"那个数", 即为最终分.
    // 前端 TaskExecutionView: DEDUCTION 拨负数 (区间 [-max,0]), ADDITION 拨正数 (区间 [0,max]).
    // DEDUCTION → -abs(值) (恒为负); ADDITION → +abs(值) (恒为正). 空响应 → 0. 与 cfg 无关.

    private BigDecimal deductionOrAddition(JsonNode cfg, String responseValue, boolean negate) {
        BigDecimal value = parseNumeric(responseValue);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal abs = value.abs();
        return negate ? abs.negate() : abs;
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

    // ==================== TIERED_DEDUCTION ====================
    // responseValue = 档位 label; 在 cfg.tiers 数组里按 label 查 score.
    // 前端 ItemEditor 校验 tier.score = Math.min(0, score) → 已是负数或 0, 原样返回.
    // 元素结构 {label, score}; 查不到 / 空响应 → 0.

    private BigDecimal tieredDeduction(JsonNode cfg, String responseValue) {
        if (responseValue == null || responseValue.isBlank()) {
            return BigDecimal.ZERO;
        }
        String target = responseValue.trim();
        JsonNode tiers = cfg.get("tiers");
        if (tiers == null || !tiers.isArray()) {
            return BigDecimal.ZERO;
        }
        for (JsonNode el : tiers) {
            JsonNode labelNode = el.get("label");
            if (labelNode != null && target.equals(labelNode.asText())) {
                JsonNode scoreNode = el.get("score");
                if (scoreNode == null || scoreNode.isNull()) {
                    return BigDecimal.ZERO;
                }
                BigDecimal v = toBigDecimal(scoreNode.asText());
                return v == null ? BigDecimal.ZERO : v;
            }
        }
        return BigDecimal.ZERO;
    }

    // ==================== WEIGHTED_MULTI / RISK_MATRIX (复用 SeverityNormalizer) ====================
    // 构造最小 SubmissionDetail 喂 normalizer 得 [0,1] severity, 再换算成分.
    //
    // <p><b>两种换算语义 (产品已定 P2.1):</b>
    // <ul>
    //   <li>WEIGHTED_MULTI = <b>得分语义</b> (正分): {@code score = round((1 - severity) × maxScore)}.
    //       多维加权综合评估越好得分越高, 完美(severity 0)拿满分 maxScore, 最差(severity 1)得 0.</li>
    //   <li>RISK_MATRIX = <b>扣分语义</b> (负分): {@code score = round(-(severity × maxScore))}.
    //       风险矩阵越严重扣越多, 低风险(severity 0)不扣, 极高风险(severity 1)扣满 maxScore.</li>
    // </ul>
    // maxScore 缺省 100. round 到整数与 ratingScale 风格一致.

    private BigDecimal complex(ScoringMode mode, JsonNode cfg, String responseValue, String rawConfigJson) {
        if (responseValue == null || responseValue.isBlank()) {
            return BigDecimal.ZERO;
        }
        SubmissionDetail detail = SubmissionDetail.builder()
                .scoringMode(mode)
                .responseValue(responseValue)
                .scoringConfig(rawConfigJson)
                .build();
        Double severity = SeverityNormalizer.of(mode).normalize(detail, null);
        if (severity == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal maxScore = firstNumeric(cfg, "maxScore");
        if (maxScore == null) {
            maxScore = new BigDecimal("100");
        }
        if (mode == ScoringMode.WEIGHTED_MULTI) {
            // 得分语义: 越好得分越高 = (1 - severity) × maxScore (正).
            return BigDecimal.valueOf(1.0 - severity)
                    .multiply(maxScore)
                    .setScale(0, RoundingMode.HALF_UP);
        }
        // RISK_MATRIX: 扣分语义, 越严重扣越多 (负).
        return BigDecimal.valueOf(severity)
                .multiply(maxScore)
                .negate()
                .setScale(0, RoundingMode.HALF_UP);
    }

    // ==================== THRESHOLD ====================
    // responseValue = 实测值 (数值); cfg.thresholds = [{upTo, score}] 升序.
    // 实测值 <= upTo 落该档取 score; 超过所有 upTo 用无 upTo 的兜底档.
    // 非数值 / 无配置 → 0. score 通常配为负 (扣分), 原样返回.

    private BigDecimal threshold(JsonNode cfg, String responseValue) {
        BigDecimal measured = parseNumeric(responseValue);
        if (measured == null) {
            return BigDecimal.ZERO;
        }
        JsonNode tiers = cfg.has("thresholds") ? cfg.get("thresholds")
                : cfg.has("tiers") ? cfg.get("tiers")
                : null;
        if (tiers == null || !tiers.isArray()) {
            return BigDecimal.ZERO;
        }
        JsonNode catchAll = null;
        for (JsonNode el : tiers) {
            JsonNode upToNode = el.get("upTo");
            if (upToNode == null || upToNode.isNull()) {
                catchAll = el; // 无上限 → 兜底档
                continue;
            }
            BigDecimal upTo = toBigDecimal(upToNode.asText());
            if (upTo != null && measured.compareTo(upTo) <= 0) {
                return tierScore(el);
            }
        }
        return catchAll == null ? BigDecimal.ZERO : tierScore(catchAll);
    }

    private BigDecimal tierScore(JsonNode tier) {
        JsonNode scoreNode = tier.get("score");
        if (scoreNode == null || scoreNode.isNull()) {
            return BigDecimal.ZERO;
        }
        BigDecimal v = toBigDecimal(scoreNode.asText());
        return v == null ? BigDecimal.ZERO : v;
    }

    // ==================== FORMULA (复用 FormulaEvaluator) ====================
    // cfg.formula = JS 表达式; 变量 value/score/response 均绑定为 responseValue 数值化结果.
    // 结果按 cfg.minScore/maxScore 钳制. 公式缺失 / 求值器未注入 / 异常 → 0.

    private BigDecimal formula(JsonNode cfg, String responseValue) {
        if (formulaEvaluator == null) {
            return BigDecimal.ZERO;
        }
        JsonNode formulaNode = cfg.get("formula");
        if (formulaNode == null || formulaNode.isNull() || formulaNode.asText().isBlank()) {
            return BigDecimal.ZERO;
        }
        String expr = formulaNode.asText();
        BigDecimal value = parseNumeric(responseValue);
        double v = value == null ? 0d : value.doubleValue();
        Map<String, Object> vars = new HashMap<>();
        vars.put("value", v);
        vars.put("score", v);
        vars.put("response", v);
        BigDecimal result;
        try {
            result = formulaEvaluator.evaluate(expr, vars);
        } catch (Exception e) {
            log.debug("FORMULA 求值失败 expr={} response={}: {}", expr, responseValue, e.getMessage());
            return BigDecimal.ZERO;
        }
        if (result == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal min = firstNumeric(cfg, "minScore");
        BigDecimal max = firstNumeric(cfg, "maxScore");
        if (min != null && result.compareTo(min) < 0) {
            result = min;
        }
        if (max != null && result.compareTo(max) > 0) {
            result = max;
        }
        return result;
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
