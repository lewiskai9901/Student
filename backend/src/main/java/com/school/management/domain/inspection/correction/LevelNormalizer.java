package com.school.management.domain.inspection.correction;

import com.school.management.domain.inspection.model.execution.SubmissionDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * LEVEL (A/B/C/D 等): 默认 A=0, B=0.25, C=0.6, D=1.0. E/F 也 1.0.
 *
 * <p>Sprint 2 可读 scoringConfig.levelMap 自定义.
 */
public class LevelNormalizer implements SeverityNormalizer {

    private static final Logger log = LoggerFactory.getLogger(LevelNormalizer.class);

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
            default:
                // P2#13: 未知等级不静默跳过 — 记 warn 暴露脏数据 / 未配置的等级值
                log.warn("LEVEL normalize: 未知等级 '{}' (detailId={}, itemCode={}), 该项不参与整改判定",
                        resp, detail.getId(), detail.getItemCode());
                return null;
        }
    }
}
