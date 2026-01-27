package com.school.management.domain.organization.model;

import lombok.Getter;

/**
 * 组织类型枚举
 */
@Getter
public enum UnitType {

    // ========== 教学单位类型 ==========
    SCHOOL("school", "学校", UnitCategory.ACADEMIC),
    DEPARTMENT("department", "系部", UnitCategory.ACADEMIC),
    MAJOR("major", "专业", UnitCategory.ACADEMIC),
    GRADE("grade", "年级", UnitCategory.ACADEMIC),
    CLASS("class", "班级", UnitCategory.ACADEMIC),

    // ========== 职能部门类型 ==========
    STUDENT_AFFAIRS("student_affairs", "学工处", UnitCategory.FUNCTIONAL),
    ACADEMIC_AFFAIRS("academic_affairs", "教务处", UnitCategory.FUNCTIONAL),
    LOGISTICS("logistics", "后勤处", UnitCategory.FUNCTIONAL),
    FINANCE("finance", "财务处", UnitCategory.FUNCTIONAL),
    GENERAL_OFFICE("general_office", "校办", UnitCategory.FUNCTIONAL),
    HR("hr", "人事处", UnitCategory.FUNCTIONAL),

    // ========== 行政单位类型 ==========
    ADMIN_DEPT("admin_dept", "行政部门", UnitCategory.ADMINISTRATIVE);

    private final String code;
    private final String name;
    private final UnitCategory category;

    UnitType(String code, String name, UnitCategory category) {
        this.code = code;
        this.name = name;
        this.category = category;
    }

    public static UnitType fromCode(String code) {
        if (code == null) return DEPARTMENT;
        for (UnitType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        // 兼容旧数据
        switch (code.toUpperCase()) {
            case "SCHOOL": return SCHOOL;
            case "COLLEGE":
            case "DEPARTMENT": return DEPARTMENT;
            case "TEACHING_GROUP": return CLASS;
            default: return DEPARTMENT;
        }
    }

    public boolean isAcademic() {
        return this.category == UnitCategory.ACADEMIC;
    }

    public boolean isFunctional() {
        return this.category == UnitCategory.FUNCTIONAL;
    }
}
