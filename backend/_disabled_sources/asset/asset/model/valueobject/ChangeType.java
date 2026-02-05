package com.school.management.domain.asset.model.valueobject;

import lombok.Getter;

/**
 * 资产变更类型值对象
 */
@Getter
public enum ChangeType {
    CREATE("CREATE", "新增"),
    UPDATE("UPDATE", "修改"),
    TRANSFER("TRANSFER", "调拨"),
    REPAIR("REPAIR", "维修"),
    SCRAP("SCRAP", "报废");

    private final String code;
    private final String description;

    ChangeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ChangeType fromCode(String code) {
        for (ChangeType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown change type code: " + code);
    }
}
