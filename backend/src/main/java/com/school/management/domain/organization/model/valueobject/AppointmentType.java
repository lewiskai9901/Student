package com.school.management.domain.organization.model.valueobject;

public enum AppointmentType {
    FORMAL("正式任命"),
    ACTING("代理"),
    CONCURRENT("兼任"),
    PROBATION("试用");

    private final String label;

    AppointmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
