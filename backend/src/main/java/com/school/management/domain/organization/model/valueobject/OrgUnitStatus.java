package com.school.management.domain.organization.model.valueobject;

public enum OrgUnitStatus {
    DRAFT("草稿"),
    ACTIVE("正常"),
    FROZEN("冻结"),
    MERGING("合并中"),
    DISSOLVED("已撤销");

    private final String label;

    OrgUnitStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
