package com.school.management.domain.inspection.correction;

import com.school.management.domain.inspection.model.execution.ScoringMode;
import com.school.management.domain.inspection.model.execution.SubmissionDetail;

/**
 * 把 13 种 ScoringMode 的异构输出标准化为统一的 [0,1] severity 信号.
 *
 * <p>0 = 完美 (无需整改), 1 = 最严重. 返回 null = 该响应不参与判定 (例如 N/A).
 * 不同 mode 的 normalizer 通过 {@link #of(ScoringMode)} 静态工厂分发.
 */
public interface SeverityNormalizer {

    /**
     * @param detail 提交明细 (含 responseValue / score / scoringConfig)
     * @param itemWeight 项目权重 (用于扣分占比计算), 可为 null
     * @return [0,1] severity, 或 null 表示跳过
     */
    Double normalize(SubmissionDetail detail, java.math.BigDecimal itemWeight);

    /** 静态分发器: 按 ScoringMode 取对应 normalizer. */
    static SeverityNormalizer of(ScoringMode mode) {
        if (mode == null) return new DeductionNormalizer();
        switch (mode) {
            case PASS_FAIL:        return new PassFailNormalizer();
            case DEDUCTION:
            case TIERED_DEDUCTION:
            case CUMULATIVE:       return new DeductionNormalizer();
            case LEVEL:            return new LevelNormalizer();
            case RATING_SCALE:     return new RatingScaleNormalizer();
            case DIRECT:
            case ADDITION:
            case SCORE_TABLE:
            case THRESHOLD:
            case FORMULA:          return new DirectNormalizer();
            // 复杂 mode 在 Sprint 2 引入: WEIGHTED_MULTI / RISK_MATRIX
            case WEIGHTED_MULTI:
            case RISK_MATRIX:      return new DeductionNormalizer();  // 暂用扣分降级
            default:               return new DeductionNormalizer();
        }
    }
}
