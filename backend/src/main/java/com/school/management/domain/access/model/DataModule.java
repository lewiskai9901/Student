package com.school.management.domain.access.model;

/**
 * 数据模块枚举
 * 与DDD领域对齐的模块定义
 */
public enum DataModule {
    // ========== Organization Domain - 组织管理领域 ==========
    /**
     * 组织架构管理
     */
    ORG_UNIT("org_unit", "组织架构", "organization"),

    /**
     * 学生信息管理
     */
    STUDENT("student", "学生信息", "organization"),

    /**
     * 宿舍管理
     */
    DORMITORY("dormitory", "宿舍管理", "organization"),

    /**
     * 教室管理
     */
    CLASSROOM("classroom", "教室管理", "organization"),

    // ========== Inspection Domain - 量化检查领域 ==========
    /**
     * 检查模板管理
     */
    INSPECTION_TEMPLATE("inspection_template", "检查模板", "inspection"),

    /**
     * 检查记录管理
     */
    INSPECTION_RECORD("inspection_record", "检查记录", "inspection"),

    /**
     * 申诉管理
     */
    APPEAL("appeal", "申诉管理", "inspection"),

    // ========== Evaluation Domain - 评价领域 ==========
    /**
     * 评价管理
     */
    RATING("rating", "评价管理", "evaluation"),

    // ========== Task Domain - 任务领域 ==========
    /**
     * 任务管理
     */
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

    /**
     * 根据code查找DataModule
     */
    public static DataModule fromCode(String code) {
        for (DataModule module : values()) {
            if (module.code.equals(code)) {
                return module;
            }
        }
        throw new IllegalArgumentException("Unknown DataModule code: " + code);
    }
}
