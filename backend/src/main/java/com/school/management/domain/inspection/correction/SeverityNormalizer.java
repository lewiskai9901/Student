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

    /**
     * 解析"归一化标签" — 用于题目级 ItemRule.baseSeverityMap 按本模式的语义标签匹配.
     *
     * <p>默认实现返回 responseValue (PASS_FAIL/LEVEL 等离散值原样匹配).
     * RiskMatrixNormalizer 覆盖返回 risk level (L/M/H/VH), Threshold 覆盖返回 tier 标签 等.
     *
     * <p>引擎判定时优先用此标签查 baseSeverityMap, 而非 responseValue 原始值,
     * 这样用户对 RISK_MATRIX 等复合模式可以按"高风险→严重"配置, 不必理解坐标(p,i).
     */
    default String resolveLabel(SubmissionDetail detail) {
        return detail.getResponseValue();
    }

    /** 静态分发器: 按 ScoringMode 取对应 normalizer. */
    static SeverityNormalizer of(ScoringMode mode) {
        if (mode == null) return new DeductionNormalizer();
        switch (mode) {
            case PASS_FAIL:        return new PassFailNormalizer();
            case DEDUCTION:
            case CUMULATIVE:       return new DeductionNormalizer();
            case TIERED_DEDUCTION: return new TieredDeductionNormalizer();
            case LEVEL:            return new LevelNormalizer();
            case RATING_SCALE:     return new RatingScaleNormalizer();
            case DIRECT:
            case ADDITION:
            case SCORE_TABLE:
            case THRESHOLD:
            case FORMULA:          return new DirectNormalizer();
            case WEIGHTED_MULTI:   return new WeightedMultiNormalizer();
            case RISK_MATRIX:      return new RiskMatrixNormalizer();
            default:               return new DeductionNormalizer();
        }
    }
}
