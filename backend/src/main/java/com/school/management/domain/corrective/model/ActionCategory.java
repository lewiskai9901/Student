package com.school.management.domain.corrective.model;

public enum ActionCategory {
    HYGIENE("卫生"),
    DISCIPLINE("纪律"),
    SAFETY("安全"),
    OTHER("其他");

    private final String displayName;

    ActionCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
