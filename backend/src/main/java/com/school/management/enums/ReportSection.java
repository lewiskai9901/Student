package com.school.management.enums;

import lombok.Getter;

/**
 * 报告模块枚举
 */
@Getter
public enum ReportSection {
    FREQUENCY_STATS("FREQUENCY_STATS", "频次统计", "柱状图"),
    SCORE_STATS("SCORE_STATS", "分数统计", "数据卡片"),
    TREND_ANALYSIS("TREND_ANALYSIS", "趋势分析", "折线图"),
    DISTRIBUTION("DISTRIBUTION", "问题分布", "饼图"),
    CLASS_RANKING("CLASS_RANKING", "班级排名", "横向柱状图"),
    STUDENT_RANKING("STUDENT_RANKING", "学生排名", "表格"),
    DEDUCTION_HEATMAP("DEDUCTION_HEATMAP", "扣分热力图", "热力图"),
    CATEGORY_RADAR("CATEGORY_RADAR", "类别雷达图", "雷达图"),
    COMPARE_ANALYSIS("COMPARE_ANALYSIS", "对比分析", "分组柱状图");

    private final String code;
    private final String name;
    private final String chartType;

    ReportSection(String code, String name, String chartType) {
        this.code = code;
        this.name = name;
        this.chartType = chartType;
    }
}
