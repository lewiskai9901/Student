package com.school.management.domain.inspection.correction;

/**
 * 项目级整改策略 (V20260524_7 重构).
 *
 * <p>新字段:
 * <ul>
 *   <li>{@code enabled} — 总开关 (取代旧 OFF strictness)</li>
 *   <li>{@code strictnessAdjustment} — 整体严格度调档 (-2~+2, 默认 0)</li>
 *   <li>{@code autoCreateLevel} — 自动建单门槛 (HIGH/MEDIUM/LOW/NONE)</li>
 * </ul>
 *
 * <p>旧 strictness (STRICT/NORMAL/LENIENT/OFF) 保留兼容输入, 内部按映射规则转换:
 * STRICT → adj=+1 auto=LOW; NORMAL → adj=0 auto=NONE; LENIENT → adj=-1 auto=NONE; OFF → enabled=false.
 */
public record ProjectCorrectivePolicy(
        boolean enabled,
        int strictnessAdjustment,
        Severity autoCreateLevel,
        SeverityThresholds thresholds,
        DeadlinePresets deadlines
) {

    public static ProjectCorrectivePolicy normalDefault() {
        return new ProjectCorrectivePolicy(
                true, 0, Severity.NONE,
                SeverityThresholds.NORMAL, DeadlinePresets.DEFAULT);
    }

    public boolean isOff() { return !enabled; }

    /**
     * 按 strictnessAdjustment 升降一个 Severity 档.
     * adj=+1: NONE→LOW, LOW→MEDIUM, MEDIUM→HIGH, HIGH→HIGH (封顶)
     * adj=-1: HIGH→MEDIUM, MEDIUM→LOW, LOW→NONE, NONE→NONE (封底)
     */
    public Severity shiftBy(Severity base) {
        if (base == null) return Severity.NONE;
        if (strictnessAdjustment == 0) return base;
        Severity result = base;
        if (strictnessAdjustment > 0) {
            for (int i = 0; i < strictnessAdjustment; i++) result = result.escalateOne();
        } else {
            for (int i = 0; i < -strictnessAdjustment; i++) result = deescalateOne(result);
        }
        return result;
    }

    private static Severity deescalateOne(Severity s) {
        switch (s) {
            case HIGH:   return Severity.MEDIUM;
            case MEDIUM: return Severity.LOW;
            case LOW:    return Severity.NONE;
            default:     return Severity.NONE;
        }
    }

    /** 该 severity 是否触发自动建单. */
    public boolean shouldAutoCreate(Severity sev) {
        if (sev == null || sev == Severity.NONE) return false;
        if (autoCreateLevel == null || autoCreateLevel == Severity.NONE) return false;
        // sev >= autoCreateLevel
        return sev.ordinal() >= autoCreateLevel.ordinal();
    }

    /** 兼容旧 strictness 字符串构造 (DB 仍保留 corrective_strictness 列). */
    public static ProjectCorrectivePolicy legacyFromStrictness(String strictness) {
        if (strictness == null) return normalDefault();
        switch (strictness.toUpperCase()) {
            case "STRICT":
                return new ProjectCorrectivePolicy(true, 1, Severity.LOW,
                        SeverityThresholds.NORMAL, DeadlinePresets.DEFAULT);
            case "LENIENT":
                return new ProjectCorrectivePolicy(true, -1, Severity.NONE,
                        SeverityThresholds.NORMAL, DeadlinePresets.DEFAULT);
            case "OFF":
                return new ProjectCorrectivePolicy(false, 0, Severity.NONE,
                        SeverityThresholds.NORMAL, DeadlinePresets.DEFAULT);
            case "NORMAL":
            default:
                return normalDefault();
        }
    }
}
