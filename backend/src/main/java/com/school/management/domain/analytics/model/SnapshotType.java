package com.school.management.domain.analytics.model;

public enum SnapshotType {
    CLASS_RANKING("班级排名"),
    DEPARTMENT_TREND("系部趋势"),
    INSPECTOR_WORKLOAD("检查员工作量"),
    VIOLATION_DISTRIBUTION("违规分布");

    private final String label;

    SnapshotType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
