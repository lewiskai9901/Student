package com.school.management.infrastructure.extension.plugins.education.constants;

/**
 * 教育行业权限常量 — 与 EducationPermissionProvider 对齐.
 *
 * 业务代码引用:
 * <pre>
 *   {@code @CasbinAccess(EducationPermissions.STUDENT_INFO_VIEW)}
 * </pre>
 *
 * 卸载教育插件时,对这些权限的引用会因 class missing 而编译失败(契约绑定).
 */
public final class EducationPermissions {
    private EducationPermissions() {}

    // ─── academic 学术 ───
    public static final String ACADEMIC_MAJOR_VIEW             = "academic:major:view";
    public static final String ACADEMIC_MAJOR_EDIT             = "academic:major:edit";
    public static final String ACADEMIC_COURSE_VIEW            = "academic:course:view";
    public static final String ACADEMIC_COURSE_EDIT            = "academic:course:edit";
    public static final String ACADEMIC_CURRICULUM_VIEW        = "academic:curriculum:view";
    public static final String ACADEMIC_CURRICULUM_EDIT        = "academic:curriculum:edit";
    public static final String ACADEMIC_GRADE_DIRECTION_VIEW   = "academic:grade-direction:view";
    public static final String ACADEMIC_GRADE_DIRECTION_EDIT   = "academic:grade-direction:edit";

    // ─── student:info 学生基本信息 ───
    public static final String STUDENT_INFO_VIEW   = "student:info:view";
    public static final String STUDENT_INFO_ADD    = "student:info:add";
    public static final String STUDENT_INFO_EDIT   = "student:info:edit";
    public static final String STUDENT_INFO_DELETE = "student:info:delete";
    public static final String STUDENT_INFO_IMPORT = "student:info:import";
    public static final String STUDENT_INFO_EXPORT = "student:info:export";

    // ─── student:class 班级 ───
    public static final String STUDENT_CLASS_VIEW   = "student:class:view";
    public static final String STUDENT_CLASS_ADD    = "student:class:add";
    public static final String STUDENT_CLASS_EDIT   = "student:class:edit";
    public static final String STUDENT_CLASS_DELETE = "student:class:delete";

    // ─── student:department 院系 ───
    public static final String STUDENT_DEPARTMENT_VIEW   = "student:department:view";
    public static final String STUDENT_DEPARTMENT_ADD    = "student:department:add";
    public static final String STUDENT_DEPARTMENT_EDIT   = "student:department:edit";
    public static final String STUDENT_DEPARTMENT_DELETE = "student:department:delete";

    // ─── student:dormitory 宿舍(老版,暂保留) ───
    public static final String STUDENT_DORMITORY_VIEW   = "student:dormitory:view";
    public static final String STUDENT_DORMITORY_ADD    = "student:dormitory:add";
    public static final String STUDENT_DORMITORY_EDIT   = "student:dormitory:edit";
    public static final String STUDENT_DORMITORY_DELETE = "student:dormitory:delete";

    // ─── student:grade 成绩 ───
    public static final String STUDENT_GRADE_VIEW   = "student:grade:view";
    public static final String STUDENT_GRADE_EDIT   = "student:grade:edit";
    public static final String STUDENT_GRADE_DELETE = "student:grade:delete";

    // ─── teaching 教学 ───
    public static final String TEACHING_CLASSROOM_VIEW   = "teaching:classroom:view";
    public static final String TEACHING_CLASSROOM_LIST   = "teaching:classroom:list";
    public static final String TEACHING_CLASSROOM_ADD    = "teaching:classroom:add";
    public static final String TEACHING_CLASSROOM_EDIT   = "teaching:classroom:edit";
    public static final String TEACHING_CLASSROOM_DELETE = "teaching:classroom:delete";

    // ─── dormitory 宿舍业务 ───
    public static final String DORMITORY_STUDENT_ASSIGN = "dormitory:student:assign";

    // ─── semester 学期 ───
    public static final String SYSTEM_SEMESTER_LIST   = "system:semester:list";
    public static final String SYSTEM_SEMESTER_QUERY  = "system:semester:query";
    public static final String SYSTEM_SEMESTER_ADD    = "system:semester:add";
    public static final String SYSTEM_SEMESTER_EDIT   = "system:semester:edit";
    public static final String SYSTEM_SEMESTER_DELETE = "system:semester:delete";

    // ─── building 楼栋 ───
    public static final String SYSTEM_BUILDING_VIEW   = "system:building:view";
    public static final String SYSTEM_BUILDING_ADD    = "system:building:add";
    public static final String SYSTEM_BUILDING_EDIT   = "system:building:edit";
    public static final String SYSTEM_BUILDING_DELETE = "system:building:delete";

    // ─── dormitory_building 宿舍楼 ───
    public static final String SYSTEM_DORMITORY_BUILDING_VIEW           = "system:dormitory_building:view";
    public static final String SYSTEM_DORMITORY_BUILDING_EDIT           = "system:dormitory_building:edit";
    public static final String SYSTEM_DORMITORY_BUILDING_ASSIGN_MANAGER = "system:dormitory_building:assign_manager";
}
