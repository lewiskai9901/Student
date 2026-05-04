package com.school.management.domain.inspection.correction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * TIERED_DEDUCTION 阶梯扣分 normalizer.
 *
 * <p>scoringConfig 期望:
 * <pre>
 * {"tiers":[{"upTo":2,"label":"轻微"},{"upTo":5,"label":"一般"},{"upTo":10,"label":"严重"}]}
 * </pre>
 * <p>用最高 tier 上限作为分母 (而非 itemWeight). 落到不同 tier → 不同 severity.
 */
public class TieredDeductionNormalizer implements SeverityNormalizer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Double normalize(SubmissionDetail detail, BigDecimal itemWeight) {
        BigDecimal score = detail.getScore();
        if (score == null) return null;
        if (score.signum() >= 0) return 0.0;

        BigDecimal abs = score.abs();
        BigDecimal denom = resolveMaxTier(detail.getScoringConfig());
        if (denom == null || denom.signum() <= 0) {
            // fallback 到普通 DEDUCTION
            return new DeductionNormalizer().normalize(detail, itemWeight);
        }

        double ratio = abs.divide(denom, 4, RoundingMode.HALF_UP).doubleValue();
        if (ratio < 0) ratio = 0;
        if (ratio > 1) ratio = 1;
        return ratio;
    }

    private static BigDecimal resolveMaxTier(String cfgJson) {
        if (cfgJson == null || cfgJson.isBlank()) return null;
        try {
            JsonNode cfg = MAPPER.readTree(cfgJson);
            if (!cfg.has("tiers") || !cfg.get("tiers").isArray()) return null;
            BigDecimal max = null;
            for (JsonNode t : cfg.get("tiers")) {
                if (t.has("upTo") && t.get("upTo").isNumber()) {
                    BigDecimal v = new BigDecimal(t.get("upTo").asText());
                    if (max == null || v.compareTo(max) > 0) max = v;
                }
            }
            return max;
        } catch (Exception e) {
            return null;
        }
    }
}
