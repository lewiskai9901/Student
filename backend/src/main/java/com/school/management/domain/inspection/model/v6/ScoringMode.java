package com.school.management.domain.inspection.model.v6;

/**
 * V6打分模式枚举
 */
public enum ScoringMode {
    /** 扣分制 - 累计扣分 */
    DEDUCTION("扣分制"),

    /** 加分制 - 累计加分 */
    ADDITION("加分制"),

    /** 百分制 - 基准分减扣分加加分 */
    BASE_SCORE("百分制"),

    /** 评分制 - 直接打分(1-N) */
    RATING("评分制"),

    /** 评级制 - A/B/C/D等级 */
    GRADE("评级制"),

    /** 通过制 - 通过/不通过 */
    PASS_FAIL("通过制"),

    /** 清单制 - 逐项勾选 */
    CHECKLIST("清单制"),

    /** 混合模式 - 不同检查项使用不同方式 */
    HYBRID("混合模式");

    private final String description;

    ScoringMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
