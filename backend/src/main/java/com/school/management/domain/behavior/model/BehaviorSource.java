package com.school.management.domain.behavior.model;

public enum BehaviorSource {
    INSPECTION("量化检查"),
    TEACHER_REPORT("教师上报"),
    SELF_REPORT("自主上报");

    private final String displayName;

    BehaviorSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
