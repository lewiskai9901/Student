package com.school.management.domain.inspection.correction;

import com.school.management.domain.inspection.model.execution.SubmissionDetail;

import java.math.BigDecimal;

/** PASS_FAIL: FAIL→1.0, PASS→0.0, 空/N/A→null. */
public class PassFailNormalizer implements SeverityNormalizer {
    @Override
    public Double normalize(SubmissionDetail detail, BigDecimal itemWeight) {
        String resp = detail.getResponseValue();
        if (resp == null || resp.isBlank()) return null;
        if ("FAIL".equalsIgnoreCase(resp) || "NO".equalsIgnoreCase(resp)
            || "0".equals(resp) || "false".equalsIgnoreCase(resp)) {
            return 1.0;
        }
        if ("PASS".equalsIgnoreCase(resp) || "YES".equalsIgnoreCase(resp)
            || "1".equals(resp) || "true".equalsIgnoreCase(resp)) {
            return 0.0;
        }
        if ("N/A".equalsIgnoreCase(resp) || "NA".equalsIgnoreCase(resp)) return null;
        return null;
    }
}
