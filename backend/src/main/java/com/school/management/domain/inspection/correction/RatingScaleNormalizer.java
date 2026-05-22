package com.school.management.domain.inspection.correction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * RATING_SCALE (n 星等): severity = (max - rating) / (max - 1).
 *
 * <p>P2#12: max 优先从 scoringConfig.max 读取 (支持 1-10 等非 5 分制),
 * 缺失或解析失败时兜底 {@link #DEFAULT_MAX}=5. 满分=0, 1 星=1.0.
 */
public class RatingScaleNormalizer implements SeverityNormalizer {
    private static final Logger log = LoggerFactory.getLogger(RatingScaleNormalizer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final double DEFAULT_MAX = 5.0;
    private static final double MIN = 1.0;

    @Override
    public Double normalize(SubmissionDetail detail, BigDecimal itemWeight) {
        Double rating = parseDouble(detail.getResponseValue());
        if (rating == null) {
            BigDecimal s = detail.getScore();
            if (s != null) rating = s.doubleValue();
        }
        if (rating == null) return null;

        double max = resolveMax(detail.getScoringConfig());

        if (rating < MIN) rating = MIN;
        if (rating > max) rating = max;
        // max==MIN 退化保护: 单一刻度无法区分严重度, 一律返回 0
        if (max <= MIN) return 0.0;
        double sev = (max - rating) / (max - MIN);
        if (sev < 0) sev = 0;
        if (sev > 1) sev = 1;
        return sev;
    }

    /** P2#12: 从 scoringConfig JSON 读 max, 失败兜底 DEFAULT_MAX. */
    private static double resolveMax(String scoringConfig) {
        if (scoringConfig == null || scoringConfig.isBlank()) return DEFAULT_MAX;
        try {
            JsonNode cfg = MAPPER.readTree(scoringConfig);
            double m = cfg.path("max").asDouble(DEFAULT_MAX);
            return m > MIN ? m : DEFAULT_MAX;
        } catch (Exception e) {
            log.debug("RATING_SCALE scoringConfig parse failed, fallback max={}: {}",
                    DEFAULT_MAX, e.getMessage());
            return DEFAULT_MAX;
        }
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Double.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
