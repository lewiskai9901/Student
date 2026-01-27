package com.school.management.domain.corrective.model;

public enum ActionSeverity {
    MINOR("轻微"),
    MODERATE("一般"),
    SEVERE("严重"),
    CRITICAL("紧急");

    private final String displayName;

    ActionSeverity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
