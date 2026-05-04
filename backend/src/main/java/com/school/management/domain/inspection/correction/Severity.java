package com.school.management.domain.inspection.correction;

/**
 * 整改严重度等级.
 *
 * <p>由引擎根据 normalize 后的 [0,1] severity 分数 + 项目阈值映射得出.
 * NONE = 不需要整改; LOW/MEDIUM/HIGH = 需整改, 区分 deadline + 受理人级别.
 */
public enum Severity {
    NONE,
    LOW,
    MEDIUM,
    HIGH;

    /** 升级一级 (用于复发场景). NONE→LOW, LOW→MEDIUM, MEDIUM→HIGH, HIGH→HIGH. */
    public Severity escalateOne() {
        switch (this) {
            case NONE:   return LOW;
            case LOW:    return MEDIUM;
            case MEDIUM: return HIGH;
            default:     return HIGH;
        }
    }

    public boolean requiresCorrection() {
        return this != NONE;
    }
}
