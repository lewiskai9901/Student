package com.school.management.domain.behavior.model;

public enum BehaviorCategory {
    HYGIENE("卫生"),
    DISCIPLINE("纪律"),
    SAFETY("安全"),
    ATTENDANCE("考勤"),
    ACADEMIC("学业"),
    OTHER("其他");

    private final String displayName;

    BehaviorCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
