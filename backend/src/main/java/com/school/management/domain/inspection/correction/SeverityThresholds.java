package com.school.management.domain.inspection.correction;

/**
 * 严重度阈值: severity ∈ [0,1] → Severity 等级映射.
 *
 * <p>满足 sev >= high → HIGH; sev >= medium → MEDIUM; sev >= low → LOW; 否则 NONE.
 */
public record SeverityThresholds(double high, double medium, double low) {

    public static final SeverityThresholds STRICT  = new SeverityThresholds(0.5, 0.3, 0.1);
    public static final SeverityThresholds NORMAL  = new SeverityThresholds(0.8, 0.5, 0.3);
    public static final SeverityThresholds LENIENT = new SeverityThresholds(0.9, 0.7, 0.5);

    public static SeverityThresholds fromStrictness(String strictness) {
        if (strictness == null) return NORMAL;
        switch (strictness.toUpperCase()) {
            case "STRICT":  return STRICT;
            case "LENIENT": return LENIENT;
            case "NORMAL":  return NORMAL;
            default:        return NORMAL;
        }
    }

    public Severity classify(double sev) {
        if (sev >= high)   return Severity.HIGH;
        if (sev >= medium) return Severity.MEDIUM;
        if (sev >= low)    return Severity.LOW;
        return Severity.NONE;
    }
}
