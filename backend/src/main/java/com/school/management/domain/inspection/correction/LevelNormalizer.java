package com.school.management.domain.inspection.correction;

import com.school.management.domain.inspection.model.execution.SubmissionDetail;

import java.math.BigDecimal;

/**
 * LEVEL (A/B/C/D 等): 默认 A=0, B=0.25, C=0.6, D=1.0. E/F 也 1.0.
 *
 * <p>Sprint 2 可读 scoringConfig.levelMap 自定义.
 */
public class LevelNormalizer implements SeverityNormalizer {
    @Override
    public Double normalize(SubmissionDetail detail, BigDecimal itemWeight) {
        String resp = detail.getResponseValue();
        if (resp == null || resp.isBlank()) return null;
        switch (resp.trim().toUpperCase()) {
            case "A": case "优秀": case "EXCELLENT": return 0.0;
            case "B": case "良好": case "GOOD":      return 0.25;
            case "C": case "合格": case "PASS":      return 0.6;
            case "D": case "不合格": case "FAIL":     return 1.0;
            case "E": case "F": case "差":           return 1.0;
            default: return null;
        }
    }
}
