package com.school.management.domain.scoring.model.valueobject;

import lombok.Getter;

/**
 * UI组件类型
 */
@Getter
public enum ComponentType {

    /**
     * 数值输入框
     */
    NUMBER("number", "数值输入"),

    /**
     * 下拉选择
     */
    SELECT("select", "下拉选择"),

    /**
     * 复选框
     */
    CHECKBOX("checkbox", "复选框"),

    /**
     * 滑块
     */
    SLIDER("slider", "滑块"),

    /**
     * 星级评分
     */
    STAR("star", "星级评分"),

    /**
     * 等级选择
     */
    GRADE("grade", "等级选择"),

    /**
     * 文本域
     */
    TEXTAREA("textarea", "文本域");

    private final String code;
    private final String name;

    ComponentType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ComponentType fromCode(String code) {
        for (ComponentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return NUMBER;
    }
}
