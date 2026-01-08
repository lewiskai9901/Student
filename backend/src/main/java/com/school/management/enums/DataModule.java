package com.school.management.enums;

import lombok.Getter;

/**
 * 数据模块枚举
 */
@Getter
public enum DataModule {
    STUDENT("student", "学生管理"),
    CLASS("class", "班级管理"),
    DORMITORY("dormitory", "宿舍管理"),
    CHECK_RECORD("check_record", "量化检查"),
    APPEAL("appeal", "申诉管理"),
    EVALUATION("evaluation", "综合测评");

    private final String code;
    private final String name;

    DataModule(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static DataModule fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DataModule module : values()) {
            if (module.getCode().equals(code)) {
                return module;
            }
        }
        return null;
    }
}
