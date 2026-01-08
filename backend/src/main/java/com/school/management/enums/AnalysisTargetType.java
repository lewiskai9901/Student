package com.school.management.enums;

import lombok.Getter;

/**
 * 分析目标类型枚举
 */
@Getter
public enum AnalysisTargetType {
    TEMPLATE("TEMPLATE", "按检查模板"),
    CATEGORY("CATEGORY", "按检查类别"),
    DEDUCTION_ITEM("DEDUCTION_ITEM", "按扣分项"),
    SINGLE_CHECK("SINGLE_CHECK", "按单次检查"),
    ORGANIZATION("ORGANIZATION", "按组织");

    private final String code;
    private final String name;

    AnalysisTargetType(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
