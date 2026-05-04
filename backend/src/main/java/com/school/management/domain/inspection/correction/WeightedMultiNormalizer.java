package com.school.management.domain.inspection.correction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * WEIGHTED_MULTI 多维加权 normalizer.
 *
 * <p>scoringConfig 期望:
 * <pre>
 * {
 *   "dimensions": [
 *     {"key":"hygiene","weight":0.4,"mode":"LEVEL"},
 *     {"key":"safety", "weight":0.6,"mode":"PASS_FAIL"}
 *   ],
 *   "anyDimensionAbove": 0.75,
 *   "weightedTotalAbove": 0.5
 * }
 * </pre>
 *
 * <p>responseValue 期望 JSON: <code>{"hygiene":"D","safety":"FAIL"}</code>.
 *
 * <p>策略:
 * <ol>
 *   <li>对每维度调用对应 sub-mode normalizer 算出 dimSev[]</li>
 *   <li>若任一维度 dimSev ≥ anyDimensionAbove → 返回 max(dimSev) (升级)</li>
 *   <li>否则返回 Σ(weight × dimSev) 加权总分</li>
 * </ol>
 */
public class WeightedMultiNormalizer implements SeverityNormalizer {

    private static final Logger log = LoggerFactory.getLogger(WeightedMultiNormalizer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Double normalize(SubmissionDetail detail, BigDecimal itemWeight) {
        String cfgJson = detail.getScoringConfig();
        String resp = detail.getResponseValue();
        if (cfgJson == null || cfgJson.isBlank() || resp == null || resp.isBlank()) {
            return null;
        }

        JsonNode cfg, respNode;
        try {
            cfg = MAPPER.readTree(cfgJson);
            respNode = MAPPER.readTree(resp.startsWith("{") ? resp : "{}");
        } catch (Exception e) {
            log.debug("WEIGHTED_MULTI parse failed: {}", e.getMessage());
            return null;
        }

        if (!cfg.has("dimensions") || !cfg.get("dimensions").isArray()) return null;
        JsonNode dims = cfg.get("dimensions");

        double anyAbove = cfg.path("anyDimensionAbove").asDouble(Double.MAX_VALUE);

        double weightedSum = 0.0;
        double totalWeight = 0.0;
        double maxDimSev = 0.0;
        boolean any = false;

        for (JsonNode dim : dims) {
            String key = dim.path("key").asText(null);
            double w = dim.path("weight").asDouble(0);
            String mode = dim.path("mode").asText("DIRECT");
            if (key == null) continue;

            JsonNode val = respNode.get(key);
            if (val == null || val.isNull()) continue;

            // 构造一个临时 SubmissionDetail 调对应 normalizer
            SubmissionDetail sub = SubmissionDetail.builder()
                    .responseValue(val.isTextual() ? val.asText() : val.toString())
                    .score(val.isNumber() ? new BigDecimal(val.numberValue().toString()) : null)
                    .scoringMode(parseMode(mode))
                    .build();
            SeverityNormalizer subNorm = SeverityNormalizer.of(parseMode(mode));
            Double s = subNorm.normalize(sub, itemWeight);
            if (s == null) continue;

            any = true;
            weightedSum += w * s;
            totalWeight += w;
            if (s > maxDimSev) maxDimSev = s;
        }

        if (!any || totalWeight <= 0) return null;

        // 任一维度超阈值 → 升级用 max
        if (maxDimSev >= anyAbove) return maxDimSev;

        // 否则用加权平均
        double weighted = weightedSum / totalWeight;
        if (weighted < 0) weighted = 0;
        if (weighted > 1) weighted = 1;
        return weighted;
    }

    private static com.school.management.domain.inspection.model.execution.ScoringMode parseMode(String s) {
        try {
            return com.school.management.domain.inspection.model.execution.ScoringMode.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return com.school.management.domain.inspection.model.execution.ScoringMode.DIRECT;
        }
    }
}
