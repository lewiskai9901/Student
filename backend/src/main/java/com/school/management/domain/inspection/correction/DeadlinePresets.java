package com.school.management.domain.inspection.correction;

/** 各 severity 默认 deadline 天数 (项目级可覆盖). */
public record DeadlinePresets(int high, int medium, int low) {
    public static final DeadlinePresets DEFAULT = new DeadlinePresets(3, 7, 14);

    public int forSeverity(Severity sev) {
        switch (sev) {
            case HIGH:   return high;
            case MEDIUM: return medium;
            case LOW:    return low;
            default:     return 0;
        }
    }
}
