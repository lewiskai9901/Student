package com.school.management.domain.inspection.service;

import com.school.management.domain.inspection.model.v7.scoring.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * V7 评分计算领域服务
 *
 * 完整流程:
 * 1. 各 item 按 scoringConfig 计算原始分 (DEDUCTION/ADDITION/FIXED/RESPONSE_MAPPED)
 * 2. 对启用 normalization 的 item，计算归一化系数并调整分数
 * 3. 系数裁剪到 [floorAt, cappedAt] 区间
 * 4. 按 ScoreDimension 聚合各维度分数 (baseScore + SUM)
 * 5. 按维度权重计算加权平均总分 (WEIGHTED_AVG)
 * 6. 按 CalculationRule.priority 顺序执行规则链
 * 7. 裁剪到 [minScore, maxScore]
 * 8. 按 GradeBand 映射等级
 * 9. 返回 ScoreResult
 */
@Service
public class ScoreCalculationDomainService {

    private final FormulaEvaluator formulaEvaluator;
    private final NormalizationCalculator normalizationCalculator;
    private final ObjectMapper objectMapper;

    public ScoreCalculationDomainService(FormulaEvaluator formulaEvaluator) {
        this.formulaEvaluator = formulaEvaluator;
        this.normalizationCalculator = new NormalizationCalculator(formulaEvaluator);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 评分计算入口
     */
    public ScoreResult calculate(ScoringProfile profile,
                                 List<ScoreDimension> dimensions,
                                 List<CalculationRuleV7> rules,
                                 List<GradeBand> gradeBands,
                                 List<ItemScoreInput> itemScoreInputs,
                                 int population) {

        int precision = profile.getPrecisionDigits() != null ? profile.getPrecisionDigits() : 2;

        // Step 1+2+3: 计算每个 item 的分数（含归一化）
        List<ItemScoreOutput> itemOutputs = new ArrayList<>();
        for (ItemScoreInput input : itemScoreInputs) {
            ItemScoreOutput output = calculateItemScore(input, population);
            itemOutputs.add(output);
        }

        // Step 4: 按维度聚合 (baseScore + SUM)
        Map<Long, DimensionScoreResult> dimensionScores = aggregateByDimension(
                dimensions, itemOutputs, precision);

        // Step 5: 计算总分 (WEIGHTED_AVG across dimensions)
        BigDecimal totalScore = calculateTotalScore(dimensions, dimensionScores, precision);

        BigDecimal rawTotal = itemOutputs.stream()
                .map(ItemScoreOutput::getRawScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Step 6: 执行规则链
        List<RuleApplication> ruleApplications = new ArrayList<>();
        List<CalculationRuleV7> sortedRules = rules.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsEnabled()))
                .sorted(Comparator.comparingInt(CalculationRuleV7::getPriority))
                .collect(Collectors.toList());

        BigDecimal adjustedScore = totalScore;
        BigDecimal deductionTotal = BigDecimal.ZERO;
        BigDecimal bonusTotal = BigDecimal.ZERO;

        for (CalculationRuleV7 rule : sortedRules) {
            RuleApplication application = applyRule(rule, adjustedScore, itemOutputs, profile, precision);
            ruleApplications.add(application);
            if (application.isApplied()) {
                BigDecimal adjustment = application.getAdjustment();
                adjustedScore = adjustedScore.add(adjustment);
                if (adjustment.compareTo(BigDecimal.ZERO) < 0) {
                    deductionTotal = deductionTotal.add(adjustment);
                } else {
                    bonusTotal = bonusTotal.add(adjustment);
                }
            }
        }

        // Step 7: 确保分数在 [minScore, maxScore] 范围内
        BigDecimal finalScore = clampScore(adjustedScore, profile);
        finalScore = finalScore.setScale(precision, RoundingMode.HALF_UP);

        // Step 8: 等级映射
        String grade = mapGrade(finalScore, gradeBands, null);
        boolean passed = isPassed(finalScore, dimensions, dimensionScores);

        // Step 9: 构建结果
        return new ScoreResult(
                rawTotal.setScale(precision, RoundingMode.HALF_UP),
                finalScore,
                deductionTotal.setScale(precision, RoundingMode.HALF_UP),
                bonusTotal.setScale(precision, RoundingMode.HALF_UP),
                grade,
                passed,
                dimensionScores,
                itemOutputs,
                ruleApplications
        );
    }

    // ========== Item 级别计算 ==========

    private ItemScoreOutput calculateItemScore(ItemScoreInput input, int population) {
        BigDecimal rawScore = BigDecimal.ZERO;
        BigDecimal normFactor = BigDecimal.ONE;
        BigDecimal finalItemScore;

        String scoringMode = input.getScoringMode();
        BigDecimal configScore = input.getConfigScore();
        BigDecimal responseValue = input.getResponseNumericValue();
        int quantity = input.getQuantity();

        if (configScore == null) {
            configScore = BigDecimal.ZERO;
        }

        switch (scoringMode != null ? scoringMode : "DEDUCTION") {
            case "DEDUCTION":
                rawScore = configScore.abs().multiply(BigDecimal.valueOf(quantity)).negate();
                break;
            case "ADDITION":
                rawScore = configScore.abs().multiply(BigDecimal.valueOf(quantity));
                break;
            case "FIXED":
                rawScore = configScore;
                break;
            case "RESPONSE_MAPPED":
                rawScore = responseValue != null ? responseValue : BigDecimal.ZERO;
                break;
            default:
                rawScore = configScore;
                break;
        }

        // 归一化
        NormalizationConfig normConfig = input.getNormalizationConfig();
        if (normConfig != null && normConfig.isEnabled() && population > 0) {
            normFactor = normalizationCalculator.calculate(
                    normConfig.getMode(),
                    population,
                    normConfig.getBaselinePopulation(),
                    normConfig.getFloorAt(),
                    normConfig.getCappedAt(),
                    normConfig.getCustomFormula()
            );
            finalItemScore = rawScore.multiply(normFactor);
        } else {
            finalItemScore = rawScore;
        }

        return new ItemScoreOutput(
                input.getItemCode(),
                input.getDimensionId(),
                rawScore,
                normFactor,
                finalItemScore
        );
    }

    // ========== 维度聚合 (baseScore + SUM) ==========

    private Map<Long, DimensionScoreResult> aggregateByDimension(
            List<ScoreDimension> dimensions,
            List<ItemScoreOutput> itemOutputs,
            int precision) {

        Map<Long, DimensionScoreResult> results = new LinkedHashMap<>();

        for (ScoreDimension dim : dimensions) {
            Long dimId = dim.getId();
            List<ItemScoreOutput> dimItems = itemOutputs.stream()
                    .filter(o -> dimId.equals(o.getDimensionId()))
                    .collect(Collectors.toList());

            BigDecimal baseScore = dim.getBaseScore() != null ? dim.getBaseScore() : BigDecimal.ZERO;
            BigDecimal itemSum = dimItems.stream()
                    .map(ItemScoreOutput::getFinalScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dimScore = baseScore.add(itemSum);

            dimScore = dimScore.setScale(precision, RoundingMode.HALF_UP);

            boolean dimPassed = dim.getPassThreshold() == null
                    || dimScore.compareTo(dim.getPassThreshold()) >= 0;

            results.put(dimId, new DimensionScoreResult(
                    dim.getDimensionCode(),
                    dim.getDimensionName(),
                    dim.getWeight(),
                    dimScore,
                    null,
                    dimPassed
            ));
        }

        return results;
    }

    // ========== 总分计算 (WEIGHTED_AVG across dimensions) ==========

    private BigDecimal calculateTotalScore(List<ScoreDimension> dimensions,
                                           Map<Long, DimensionScoreResult> dimensionScores,
                                           int precision) {
        if (dimensionScores.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal weightedSum = BigDecimal.ZERO;
        for (ScoreDimension dim : dimensions) {
            DimensionScoreResult dsr = dimensionScores.get(dim.getId());
            if (dsr == null) continue;
            BigDecimal w = BigDecimal.valueOf(dim.getWeight() != null ? dim.getWeight() : 1);
            weightedSum = weightedSum.add(dsr.getScore().multiply(w));
            totalWeight = totalWeight.add(w);
        }
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return weightedSum.divide(totalWeight, precision + 4, RoundingMode.HALF_UP);
    }

    // ========== 规则链 ==========

    private RuleApplication applyRule(CalculationRuleV7 rule,
                                      BigDecimal currentScore,
                                      List<ItemScoreOutput> itemOutputs,
                                      ScoringProfile profile,
                                      int precision) {
        String configJson = rule.getConfig();
        JsonNode config;
        try {
            config = configJson != null ? objectMapper.readTree(configJson) : objectMapper.createObjectNode();
        } catch (Exception e) {
            return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), false, BigDecimal.ZERO, "配置解析失败: " + e.getMessage());
        }

        switch (rule.getRuleType()) {
            case VETO: {
                JsonNode vetoItems = config.get("vetoItems");
                BigDecimal vetoScore = getDecimal(config, "vetoScore", BigDecimal.ZERO);
                if (vetoItems != null && vetoItems.isArray()) {
                    Set<String> vetoSet = new HashSet<>();
                    vetoItems.forEach(n -> vetoSet.add(n.asText()));
                    boolean triggered = itemOutputs.stream()
                            .anyMatch(o -> vetoSet.contains(o.getItemCode())
                                    && o.getFinalScore().compareTo(BigDecimal.ZERO) != 0);
                    if (triggered) {
                        BigDecimal adj = vetoScore.subtract(currentScore);
                        return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), true, adj, "一票否决触发");
                    }
                }
                return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), false, BigDecimal.ZERO, null);
            }

            case BONUS: {
                JsonNode bonusItems = config.get("bonusItems");
                BigDecimal bonusScore = getDecimal(config, "bonusScore", BigDecimal.ZERO);
                if (bonusItems != null && bonusItems.isArray()) {
                    Set<String> bonusSet = new HashSet<>();
                    bonusItems.forEach(n -> bonusSet.add(n.asText()));
                    long count = itemOutputs.stream()
                            .filter(o -> bonusSet.contains(o.getItemCode())
                                    && o.getFinalScore().compareTo(BigDecimal.ZERO) > 0)
                            .count();
                    if (count > 0) {
                        BigDecimal adj = bonusScore.multiply(BigDecimal.valueOf(count));
                        return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), true, adj, "奖励加分 x" + count);
                    }
                }
                return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), false, BigDecimal.ZERO, null);
            }

            case PROGRESSIVE: {
                JsonNode thresholds = config.get("thresholds");
                if (thresholds != null && thresholds.isArray()) {
                    long deductionCount = itemOutputs.stream()
                            .filter(o -> o.getFinalScore().compareTo(BigDecimal.ZERO) < 0)
                            .count();

                    List<JsonNode> sorted = new ArrayList<>();
                    thresholds.forEach(sorted::add);
                    sorted.sort(Comparator.comparingInt(n -> n.get("count").asInt()));

                    BigDecimal penalty = BigDecimal.ZERO;
                    for (JsonNode t : sorted) {
                        int count = t.get("count").asInt();
                        BigDecimal p = new BigDecimal(t.get("penalty").asText());
                        if (deductionCount >= count) {
                            penalty = p;
                        }
                    }
                    if (penalty.compareTo(BigDecimal.ZERO) != 0) {
                        BigDecimal adj = penalty.negate();
                        return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), true, adj, "累进扣分: " + deductionCount + " 项违规");
                    }
                }
                return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), false, BigDecimal.ZERO, null);
            }

            case CUSTOM: {
                String formula = config.has("formula") ? config.get("formula").asText() : null;
                if (formula != null && !formula.isBlank()) {
                    Map<String, Object> vars = new HashMap<>();
                    vars.put("score", currentScore);
                    BigDecimal result = formulaEvaluator.evaluate(formula, vars);
                    BigDecimal adj = result.subtract(currentScore);
                    return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), true, adj, "自定义公式");
                }
                return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), false, BigDecimal.ZERO, null);
            }

            default:
                return new RuleApplication(rule.getRuleCode(), rule.getRuleType(), false, BigDecimal.ZERO, null);
        }
    }

    // ========== 等级映射 ==========

    private String mapGrade(BigDecimal score, List<GradeBand> gradeBands, Long dimensionId) {
        if (gradeBands == null || gradeBands.isEmpty()) {
            return null;
        }

        return gradeBands.stream()
                .filter(gb -> (dimensionId == null && gb.getDimensionId() == null)
                        || (dimensionId != null && dimensionId.equals(gb.getDimensionId())))
                .filter(gb -> score.compareTo(gb.getMinScore()) >= 0
                        && score.compareTo(gb.getMaxScore()) <= 0)
                .findFirst()
                .map(GradeBand::getGradeCode)
                .orElse(null);
    }

    private boolean isPassed(BigDecimal finalScore,
                             List<ScoreDimension> dimensions,
                             Map<Long, DimensionScoreResult> dimensionScores) {
        for (DimensionScoreResult dsr : dimensionScores.values()) {
            if (!dsr.isPassed()) {
                return false;
            }
        }
        return true;
    }

    private BigDecimal clampScore(BigDecimal score, ScoringProfile profile) {
        BigDecimal min = profile.getMinScore() != null ? profile.getMinScore() : BigDecimal.ZERO;
        BigDecimal max = profile.getMaxScore() != null ? profile.getMaxScore() : new BigDecimal("100");

        if (score.compareTo(min) < 0) {
            score = min;
        }
        if (score.compareTo(max) > 0) {
            score = max;
        }
        return score;
    }

    private BigDecimal getDecimal(JsonNode node, String field, BigDecimal defaultValue) {
        if (node.has(field) && !node.get(field).isNull()) {
            return new BigDecimal(node.get(field).asText());
        }
        return defaultValue != null ? defaultValue : BigDecimal.ZERO;
    }

    // ========== 内部数据类 ==========

    /**
     * Item 评分输入
     */
    public static class ItemScoreInput {
        private final String itemCode;
        private final Long dimensionId;
        private final String scoringMode;    // DEDUCTION, ADDITION, FIXED, RESPONSE_MAPPED
        private final BigDecimal configScore;
        private final BigDecimal responseNumericValue;
        private final int quantity;
        private final NormalizationConfig normalizationConfig;

        public ItemScoreInput(String itemCode, Long dimensionId, String scoringMode,
                              BigDecimal configScore, BigDecimal responseNumericValue,
                              int quantity, NormalizationConfig normalizationConfig) {
            this.itemCode = itemCode;
            this.dimensionId = dimensionId;
            this.scoringMode = scoringMode;
            this.configScore = configScore;
            this.responseNumericValue = responseNumericValue;
            this.quantity = quantity;
            this.normalizationConfig = normalizationConfig;
        }

        public String getItemCode() { return itemCode; }
        public Long getDimensionId() { return dimensionId; }
        public String getScoringMode() { return scoringMode; }
        public BigDecimal getConfigScore() { return configScore; }
        public BigDecimal getResponseNumericValue() { return responseNumericValue; }
        public int getQuantity() { return quantity; }
        public NormalizationConfig getNormalizationConfig() { return normalizationConfig; }
    }

    /**
     * 归一化配置（从 scoringConfig JSON 解析）
     */
    public static class NormalizationConfig {
        private final boolean enabled;
        private final NormalizationMode mode;
        private final int baselinePopulation;
        private final BigDecimal cappedAt;
        private final BigDecimal floorAt;
        private final String customFormula;

        public NormalizationConfig(boolean enabled, NormalizationMode mode,
                                   int baselinePopulation, BigDecimal cappedAt,
                                   BigDecimal floorAt, String customFormula) {
            this.enabled = enabled;
            this.mode = mode;
            this.baselinePopulation = baselinePopulation;
            this.cappedAt = cappedAt;
            this.floorAt = floorAt;
            this.customFormula = customFormula;
        }

        public boolean isEnabled() { return enabled; }
        public NormalizationMode getMode() { return mode; }
        public int getBaselinePopulation() { return baselinePopulation; }
        public BigDecimal getCappedAt() { return cappedAt; }
        public BigDecimal getFloorAt() { return floorAt; }
        public String getCustomFormula() { return customFormula; }
    }

    /**
     * Item 评分输出
     */
    public static class ItemScoreOutput {
        private final String itemCode;
        private final Long dimensionId;
        private final BigDecimal rawScore;
        private final BigDecimal normFactor;
        private final BigDecimal finalScore;

        public ItemScoreOutput(String itemCode, Long dimensionId,
                               BigDecimal rawScore, BigDecimal normFactor,
                               BigDecimal finalScore) {
            this.itemCode = itemCode;
            this.dimensionId = dimensionId;
            this.rawScore = rawScore;
            this.normFactor = normFactor;
            this.finalScore = finalScore;
        }

        public String getItemCode() { return itemCode; }
        public Long getDimensionId() { return dimensionId; }
        public BigDecimal getRawScore() { return rawScore; }
        public BigDecimal getNormFactor() { return normFactor; }
        public BigDecimal getFinalScore() { return finalScore; }
    }

    /**
     * 维度评分结果
     */
    public static class DimensionScoreResult {
        private final String dimensionCode;
        private final String dimensionName;
        private final Integer weight;
        private final BigDecimal score;
        private final String grade;
        private final boolean passed;

        public DimensionScoreResult(String dimensionCode, String dimensionName,
                                    Integer weight, BigDecimal score,
                                    String grade, boolean passed) {
            this.dimensionCode = dimensionCode;
            this.dimensionName = dimensionName;
            this.weight = weight;
            this.score = score;
            this.grade = grade;
            this.passed = passed;
        }

        public String getDimensionCode() { return dimensionCode; }
        public String getDimensionName() { return dimensionName; }
        public Integer getWeight() { return weight; }
        public BigDecimal getScore() { return score; }
        public String getGrade() { return grade; }
        public boolean isPassed() { return passed; }
    }

    /**
     * 规则应用记录
     */
    public static class RuleApplication {
        private final String ruleCode;
        private final RuleType ruleType;
        private final boolean applied;
        private final BigDecimal adjustment;
        private final String message;

        public RuleApplication(String ruleCode, RuleType ruleType,
                               boolean applied, BigDecimal adjustment, String message) {
            this.ruleCode = ruleCode;
            this.ruleType = ruleType;
            this.applied = applied;
            this.adjustment = adjustment;
            this.message = message;
        }

        public String getRuleCode() { return ruleCode; }
        public RuleType getRuleType() { return ruleType; }
        public boolean isApplied() { return applied; }
        public BigDecimal getAdjustment() { return adjustment; }
        public String getMessage() { return message; }
    }

    /**
     * 完整评分结果
     */
    public static class ScoreResult {
        private final BigDecimal rawScore;
        private final BigDecimal finalScore;
        private final BigDecimal deductionTotal;
        private final BigDecimal bonusTotal;
        private final String grade;
        private final boolean passed;
        private final Map<Long, DimensionScoreResult> dimensionScores;
        private final List<ItemScoreOutput> itemOutputs;
        private final List<RuleApplication> ruleApplications;

        public ScoreResult(BigDecimal rawScore, BigDecimal finalScore,
                           BigDecimal deductionTotal, BigDecimal bonusTotal,
                           String grade, boolean passed,
                           Map<Long, DimensionScoreResult> dimensionScores,
                           List<ItemScoreOutput> itemOutputs,
                           List<RuleApplication> ruleApplications) {
            this.rawScore = rawScore;
            this.finalScore = finalScore;
            this.deductionTotal = deductionTotal;
            this.bonusTotal = bonusTotal;
            this.grade = grade;
            this.passed = passed;
            this.dimensionScores = dimensionScores;
            this.itemOutputs = itemOutputs;
            this.ruleApplications = ruleApplications;
        }

        public BigDecimal getRawScore() { return rawScore; }
        public BigDecimal getFinalScore() { return finalScore; }
        public BigDecimal getDeductionTotal() { return deductionTotal; }
        public BigDecimal getBonusTotal() { return bonusTotal; }
        public String getGrade() { return grade; }
        public boolean isPassed() { return passed; }
        public Map<Long, DimensionScoreResult> getDimensionScores() { return dimensionScores; }
        public List<ItemScoreOutput> getItemOutputs() { return itemOutputs; }
        public List<RuleApplication> getRuleApplications() { return ruleApplications; }
    }
}
