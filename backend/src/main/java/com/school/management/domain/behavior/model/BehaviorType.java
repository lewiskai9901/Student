package com.school.management.domain.behavior.model;

public enum BehaviorType {
    VIOLATION("违规"),
    COMMENDATION("表扬");

    private final String displayName;

    BehaviorType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
