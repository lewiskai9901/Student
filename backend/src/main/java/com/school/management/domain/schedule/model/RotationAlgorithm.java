package com.school.management.domain.schedule.model;

public enum RotationAlgorithm {
    ROUND_ROBIN("轮询"),
    RANDOM("随机"),
    LOAD_BALANCED("工作量均衡");

    private final String label;

    RotationAlgorithm(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
