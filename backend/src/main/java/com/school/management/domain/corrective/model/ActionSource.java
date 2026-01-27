package com.school.management.domain.corrective.model;

public enum ActionSource {
    INSPECTION("检查"),
    APPEAL("申诉"),
    MANUAL("手动创建");

    private final String displayName;

    ActionSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
