package com.school.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 申诉类型枚举
 * 1=分数异议, 2=事实异议, 3=程序异议
 */
@Getter
@AllArgsConstructor
public enum AppealType {
    SCORE_DISPUTE(1, "分数异议"),
    FACT_DISPUTE(2, "事实异议"),
    PROCEDURE_DISPUTE(3, "程序异议"),
    OTHER(4, "其他");

    private final Integer code;
    private final String desc;

    public static AppealType fromCode(Integer code) {
        if (code == null) return null;
        for (AppealType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static String getDescByCode(Integer code) {
        AppealType type = fromCode(code);
        return type != null ? type.getDesc() : "未知";
    }
}
