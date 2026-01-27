package com.school.management.domain.organization.model;

/**
 * Organization unit type enumeration.
 * Supports both academic units and functional departments.
 */
public enum OrgUnitType {

    // ========== 教学单位 ==========
    /**
     * School level - the root of organization hierarchy
     */
    SCHOOL("学校", 1, true),

    /**
     * College/Faculty level (系部)
     */
    COLLEGE("系部", 2, true),

    /**
     * Department level (专业)
     */
    DEPARTMENT("专业", 3, true),

    /**
     * Teaching group level (班级)
     */
    TEACHING_GROUP("班级", 4, true),

    // ========== 职能部门 ==========
    /**
     * Student Affairs Office (学工处)
     */
    STUDENT_AFFAIRS("学工处", 2, false),

    /**
     * Academic Affairs Office (教务处)
     */
    ACADEMIC_AFFAIRS("教务处", 2, false),

    /**
     * Logistics Department (后勤处)
     */
    LOGISTICS("后勤处", 2, false),

    /**
     * Finance Department (财务处)
     */
    FINANCE("财务处", 2, false),

    /**
     * General Office (校办)
     */
    GENERAL_OFFICE("校办", 2, false),

    /**
     * Human Resources (人事处)
     */
    HR("人事处", 2, false);

    private final String displayName;
    private final int level;
    private final boolean academic;  // 是否为教学单位

    OrgUnitType(String displayName, int level, boolean academic) {
        this.displayName = displayName;
        this.level = level;
        this.academic = academic;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    public boolean isAcademic() {
        return academic;
    }

    public boolean isFunctional() {
        return !academic;
    }

    /**
     * Checks if this type can be a child of the given parent type.
     * Updated to support functional departments under SCHOOL.
     */
    public boolean canBeChildOf(OrgUnitType parentType) {
        if (parentType == null) {
            return this == SCHOOL;
        }

        // 学校可以有教学单位和职能部门作为子级
        if (parentType == SCHOOL) {
            return this.level == 2;  // 系部级别或职能部门
        }

        // 教学单位遵循层级关系
        if (this.academic && parentType.academic) {
            return this.level == parentType.level + 1;
        }

        // 职能部门不能有子级（或可以有下属科室，暂不支持）
        return false;
    }
}
