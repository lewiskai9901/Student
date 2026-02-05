package com.school.management.domain.scoring.model.valueobject;

import lombok.Getter;

/**
 * 值类型
 */
@Getter
public enum ValueType {

    /**
     * 数值
     */
    NUMBER("number", "数值"),

    /**
     * 字符串
     */
    STRING("string", "字符串"),

    /**
     * 布尔值
     */
    BOOLEAN("boolean", "布尔值"),

    /**
     * 数组
     */
    ARRAY("array", "数组"),

    /**
     * 对象
     */
    OBJECT("object", "对象"),

    /**
     * 任意类型
     */
    ANY("any", "任意类型");

    private final String code;
    private final String name;

    ValueType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ValueType fromCode(String code) {
        for (ValueType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return NUMBER;
    }
}
