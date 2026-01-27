package com.school.management.domain.access.model;

/**
 * 数据模块枚举
 * 定义系统中可配置数据权限的业务模块
 */
public enum DataModule {

    // ==================== Organization Domain ====================
    ORG_UNIT("org_unit", "组织架构", "organization"),
    STUDENT("student", "学生信息", "organization"),
    DORMITORY("dormitory", "宿舍管理", "organization"),
    CLASSROOM("classroom", "教室管理", "organization"),

    // ==================== Inspection Domain ====================
    INSPECTION_TEMPLATE("inspection_template", "检查模板", "inspection"),
    INSPECTION_RECORD("inspection_record", "检查记录", "inspection"),
    APPEAL("appeal", "申诉管理", "inspection"),

    // ==================== Evaluation Domain ====================
    RATING("rating", "评价管理", "evaluation"),

    // ==================== Task Domain ====================
    TASK("task", "任务管理", "task");

    private final String code;
    private final String name;
    private final String domain;

    DataModule(String code, String name, String domain) {
        this.code = code;
        this.name = name;
        this.domain = domain;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDomain() {
        return domain;
    }

    public static DataModule fromCode(String code) {
        for (DataModule module : values()) {
            if (module.code.equals(code)) {
                return module;
            }
        }
        throw new IllegalArgumentException("Unknown DataModule code: " + code);
    }
}
