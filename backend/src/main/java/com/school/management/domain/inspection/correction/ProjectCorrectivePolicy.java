package com.school.management.domain.inspection.correction;

/**
 * 项目级整改策略 (从 insp_projects 加载).
 *
 * <p>strictness=OFF 时引擎完全不建议, 由检查员手动决定.
 */
public record ProjectCorrectivePolicy(
        String strictness,           // STRICT/NORMAL/LENIENT/OFF
        SeverityThresholds thresholds,
        DeadlinePresets deadlines
) {
    public static ProjectCorrectivePolicy normalDefault() {
        return new ProjectCorrectivePolicy("NORMAL", SeverityThresholds.NORMAL, DeadlinePresets.DEFAULT);
    }

    public boolean isOff() {
        return "OFF".equalsIgnoreCase(strictness);
    }
}
