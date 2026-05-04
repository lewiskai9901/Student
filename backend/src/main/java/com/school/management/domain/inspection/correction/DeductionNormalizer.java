package com.school.management.domain.inspection.correction;

import com.school.management.domain.inspection.model.execution.SubmissionDetail;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DEDUCTION/TIERED_DEDUCTION/CUMULATIVE: severity = |score| / weight.
 *
 * <p>score 是负值 (扣分). weight 缺失则用 score 绝对值与 10 的比.
 * 满扣 → 1.0, 半扣 → 0.5, 无扣 → 0.
 */
public class DeductionNormalizer implements SeverityNormalizer {
    @Override
    public Double normalize(SubmissionDetail detail, BigDecimal itemWeight) {
        BigDecimal score = detail.getScore();
        if (score == null) return null;
        if (score.signum() >= 0) return 0.0;  // 无扣分

        BigDecimal absScore = score.abs();
        BigDecimal denom = itemWeight;
        if (denom == null || denom.signum() <= 0) {
            BigDecimal di = detail.getItemWeight();
            if (di != null && di.signum() > 0) denom = di;
        }
        if (denom == null || denom.signum() <= 0) denom = BigDecimal.TEN;

        double ratio = absScore.divide(denom, 4, RoundingMode.HALF_UP).doubleValue();
        if (ratio < 0) ratio = 0;
        if (ratio > 1) ratio = 1;
        return ratio;
    }
}
