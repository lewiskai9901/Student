package com.school.management.domain.organization.model.valueobject;

public enum JobLevel {
    HIGH("高层"),
    MIDDLE("中层"),
    BASE("基层"),
    EXECUTIVE("执行");

    private final String label;

    JobLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
