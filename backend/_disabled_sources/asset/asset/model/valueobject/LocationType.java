package com.school.management.domain.asset.model.valueobject;

import lombok.Getter;

/**
 * 资产位置类型值对象
 */
@Getter
public enum LocationType {
    CLASSROOM("classroom", "教室"),
    DORMITORY("dormitory", "宿舍"),
    OFFICE("office", "办公室"),
    WAREHOUSE("warehouse", "仓库"),
    OTHER("other", "其他");

    private final String code;
    private final String description;

    LocationType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static LocationType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (LocationType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
}
