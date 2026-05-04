package com.school.management.domain.inspection.correction;

import com.school.management.domain.inspection.model.execution.SubmissionDetail;

import java.math.BigDecimal;

/**
 * RATING_SCALE (1-5 星等): severity = (max - rating) / (max - 1).
 *
 * <p>默认 max=5. 5 星=0, 1 星=1.0.
 */
public class RatingScaleNormalizer implements SeverityNormalizer {
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

        if (rating < MIN) rating = MIN;
        if (rating > DEFAULT_MAX) rating = DEFAULT_MAX;
        double sev = (DEFAULT_MAX - rating) / (DEFAULT_MAX - MIN);
        if (sev < 0) sev = 0;
        if (sev > 1) sev = 1;
        return sev;
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
