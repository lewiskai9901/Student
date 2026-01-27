package com.school.management.domain.asset.model.valueobject;

import lombok.Getter;

/**
 * 归还状况
 */
@Getter
public enum ReturnCondition {
    GOOD("good", "完好"),
    DAMAGED("damaged", "损坏"),
    LOST("lost", "丢失");

    private final String code;
    private final String description;

    ReturnCondition(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ReturnCondition fromCode(String code) {
        if (code == null) return null;
        for (ReturnCondition condition : values()) {
            if (condition.code.equals(code)) {
                return condition;
            }
        }
        throw new IllegalArgumentException("Unknown ReturnCondition code: " + code);
    }
}
