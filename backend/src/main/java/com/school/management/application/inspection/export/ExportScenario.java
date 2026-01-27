package com.school.management.application.inspection.export;

import lombok.Getter;

@Getter
public enum ExportScenario {
    DEDUCTION_DETAIL("扣分明细导出", "导出实时扣分明细记录"),
    RATING_REPORT("评级报表导出", "导出班级评级报表"),
    STATISTICS_REPORT("统计报表导出", "导出检查统计分析报表");

    private final String name;
    private final String description;

    ExportScenario(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
