package com.school.management.enums;

import lombok.Getter;

/**
 * 分析时间范围枚举
 */
@Getter
public enum AnalysisTimeRange {
    LAST_7_DAYS("LAST_7_DAYS", "最近7天"),
    LAST_30_DAYS("LAST_30_DAYS", "最近30天"),
    THIS_MONTH("THIS_MONTH", "本月"),
    THIS_SEMESTER("THIS_SEMESTER", "本学期"),
    THIS_YEAR("THIS_YEAR", "本年度");

    private final String code;
    private final String name;

    AnalysisTimeRange(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
