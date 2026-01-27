package com.school.management.domain.schedule.model;

public enum PolicyType {
    DAILY("每日"),
    WEEKLY("每周"),
    CUSTOM("自定义");

    private final String label;

    PolicyType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
