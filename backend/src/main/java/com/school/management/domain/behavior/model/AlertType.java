package com.school.management.domain.behavior.model;

public enum AlertType {
    FREQUENCY("频率预警"),
    SEVERITY("严重程度预警"),
    TREND("趋势预警");

    private final String displayName;

    AlertType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
