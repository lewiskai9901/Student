package com.school.management.domain.inspection.correction;

import com.school.management.domain.inspection.model.execution.SubmissionDetail;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DIRECT/ADDITION/SCORE_TABLE/THRESHOLD/FORMULA: 把 score 视为得分,
 * severity = 1 - score/weight. 得分越低 → severity 越高.
 *
 * <p>weight 缺失则用 100 作为默认满分基准.
 */
public class DirectNormalizer implements SeverityNormalizer {
    @Override
    public Double normalize(SubmissionDetail detail, BigDecimal itemWeight) {
        BigDecimal score = detail.getScore();
        if (score == null) return null;

        BigDecimal denom = itemWeight;
        if (denom == null || denom.signum() <= 0) {
            BigDecimal di = detail.getItemWeight();
            if (di != null && di.signum() > 0) denom = di;
        }
        if (denom == null || denom.signum() <= 0) {
            denom = BigDecimal.valueOf(100);  // 默认百分制
        }

        // 负分直接 1.0 (扣到负的视为最严重)
        if (score.signum() < 0) return 1.0;

        double ratio = score.divide(denom, 4, RoundingMode.HALF_UP).doubleValue();
        double sev = 1.0 - ratio;
        if (sev < 0) sev = 0;
        if (sev > 1) sev = 1;
        return sev;
    }
}
