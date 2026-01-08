package com.school.management.enums;

import lombok.Getter;

/**
 * 数据权限范围枚举
 */
@Getter
public enum DataScope {
    ALL(1, "全部数据"),
    DEPARTMENT(2, "本部门数据"),
    GRADE(3, "本年级数据"),
    CLASS(4, "本班级数据"),
    SELF(5, "仅本人数据");

    private final Integer code;
    private final String name;

    DataScope(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static DataScope fromCode(Integer code) {
        if (code == null) {
            return ALL;
        }
        for (DataScope scope : values()) {
            if (scope.getCode().equals(code)) {
                return scope;
            }
        }
        return ALL;
    }
}
