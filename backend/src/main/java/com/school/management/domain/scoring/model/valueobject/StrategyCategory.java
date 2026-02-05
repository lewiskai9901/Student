package com.school.management.domain.scoring.model.valueobject;

import lombok.Getter;

/**
 * 计分策略分类
 */
@Getter
public enum StrategyCategory {

    /**
     * 基础策略
     */
    BASIC("basic", "基础策略"),

    /**
     * 等级策略
     */
    GRADE("grade", "等级策略"),

    /**
     * 高级策略
     */
    ADVANCED("advanced", "高级策略"),

    /**
     * 时间策略
     */
    TIME("time", "时间策略");

    private final String code;
    private final String name;

    StrategyCategory(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static StrategyCategory fromCode(String code) {
        for (StrategyCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return BASIC;
    }
}
