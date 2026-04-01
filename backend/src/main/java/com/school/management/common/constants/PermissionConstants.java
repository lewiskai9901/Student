package com.school.management.common.constants;

/**
 * 权限常量类
 * 统一管理系统中所有权限字符串，避免硬编码
 *
 * @author system
 * @version 1.0.0
 * @since 2024-12-31
 */
public final class PermissionConstants {

    private PermissionConstants() {
        throw new IllegalStateException("常量类不允许实例化");
    }

    // ==================== 宿舍管理权限 ====================
    public static final String DORMITORY_STUDENT_ASSIGN = "dormitory:student:assign";

    // ==================== 学术管理权限 (academic domain) ====================
    public static final String ACADEMIC_MAJOR_VIEW = "academic:major:view";
    public static final String ACADEMIC_MAJOR_EDIT = "academic:major:edit";
    public static final String ACADEMIC_COURSE_VIEW = "academic:course:view";
    public static final String ACADEMIC_COURSE_EDIT = "academic:course:edit";
    public static final String ACADEMIC_CURRICULUM_VIEW = "academic:curriculum:view";
    public static final String ACADEMIC_CURRICULUM_EDIT = "academic:curriculum:edit";
    public static final String ACADEMIC_GRADE_DIRECTION_VIEW = "academic:grade-direction:view";
    public static final String ACADEMIC_GRADE_DIRECTION_EDIT = "academic:grade-direction:edit";

    // ==================== 学生管理权限 ====================
    public static final String STUDENT_GRADE_VIEW = "student:grade:view";
    public static final String STUDENT_GRADE_EDIT = "student:grade:edit";
    public static final String STUDENT_GRADE_DELETE = "student:grade:delete";
    public static final String STUDENT_CLASS_ADD = "student:class:add";
    public static final String STUDENT_CLASS_DELETE = "student:class:delete";
    public static final String STUDENT_CLASS_EDIT = "student:class:edit";
    public static final String STUDENT_CLASS_VIEW = "student:class:view";
    public static final String STUDENT_DEPARTMENT_ADD = "student:department:add";
    public static final String STUDENT_DEPARTMENT_DELETE = "student:department:delete";
    public static final String STUDENT_DEPARTMENT_EDIT = "student:department:edit";
    public static final String STUDENT_DEPARTMENT_VIEW = "student:department:view";
    public static final String STUDENT_DORMITORY_ADD = "student:dormitory:add";
    public static final String STUDENT_DORMITORY_DELETE = "student:dormitory:delete";
    public static final String STUDENT_DORMITORY_EDIT = "student:dormitory:edit";
    public static final String STUDENT_DORMITORY_VIEW = "student:dormitory:view";
    public static final String STUDENT_INFO_ADD = "student:info:add";
    public static final String STUDENT_INFO_DELETE = "student:info:delete";
    public static final String STUDENT_INFO_EDIT = "student:info:edit";
    public static final String STUDENT_INFO_EXPORT = "student:info:export";
    public static final String STUDENT_INFO_IMPORT = "student:info:import";
    public static final String STUDENT_INFO_VIEW = "student:info:view";

    // ==================== 系统管理权限 ====================
    public static final String SYSTEM_ADMIN = "system:admin";
    public static final String SYSTEM_ANNOUNCEMENT_ADD = "system:announcement:add";
    public static final String SYSTEM_ANNOUNCEMENT_DELETE = "system:announcement:delete";
    public static final String SYSTEM_ANNOUNCEMENT_EDIT = "system:announcement:edit";
    public static final String SYSTEM_ANNOUNCEMENT_VIEW = "system:announcement:view";
    public static final String SYSTEM_BUILDING_ADD = "system:building:add";
    public static final String SYSTEM_BUILDING_DELETE = "system:building:delete";
    public static final String SYSTEM_BUILDING_EDIT = "system:building:edit";
    public static final String SYSTEM_BUILDING_VIEW = "system:building:view";
    public static final String SYSTEM_CONFIG_ADD = "system:config:add";
    public static final String SYSTEM_CONFIG_DELETE = "system:config:delete";
    public static final String SYSTEM_CONFIG_EDIT = "system:config:edit";
    public static final String SYSTEM_CONFIG_VIEW = "system:config:view";
    public static final String SYSTEM_DORMITORY_BUILDING_ASSIGN_MANAGER = "system:dormitory_building:assign_manager";
    public static final String SYSTEM_DORMITORY_BUILDING_EDIT = "system:dormitory_building:edit";
    public static final String SYSTEM_DORMITORY_BUILDING_VIEW = "system:dormitory_building:view";
    public static final String SYSTEM_OPERLOG_CLEAR = "system:operlog:clear";
    public static final String SYSTEM_OPERLOG_DELETE = "system:operlog:delete";
    public static final String SYSTEM_OPERLOG_VIEW = "system:operlog:view";
    public static final String SYSTEM_PERMISSION_ADD = "system:permission:add";
    public static final String SYSTEM_PERMISSION_DELETE = "system:permission:delete";
    public static final String SYSTEM_PERMISSION_EDIT = "system:permission:edit";
    public static final String SYSTEM_PERMISSION_VIEW = "system:permission:view";
    public static final String SYSTEM_ROLE_ADD = "system:role:add";
    public static final String SYSTEM_ROLE_DELETE = "system:role:delete";
    public static final String SYSTEM_ROLE_EDIT = "system:role:edit";
    public static final String SYSTEM_ROLE_VIEW = "system:role:view";
    public static final String SYSTEM_SEMESTER_ADD = "system:semester:add";
    public static final String SYSTEM_SEMESTER_DELETE = "system:semester:delete";
    public static final String SYSTEM_SEMESTER_EDIT = "system:semester:edit";
    public static final String SYSTEM_SEMESTER_LIST = "system:semester:list";
    public static final String SYSTEM_SEMESTER_QUERY = "system:semester:query";
    public static final String SYSTEM_USER_ADD = "system:user:add";
    public static final String SYSTEM_USER_DELETE = "system:user:delete";
    public static final String SYSTEM_USER_EDIT = "system:user:edit";
    public static final String SYSTEM_USER_VIEW = "system:user:view";

    // ==================== 任务权限 ====================
    public static final String TASK_APPROVE = "task:approve";
    public static final String TASK_CREATE = "task:create";
    public static final String TASK_EXECUTE = "task:execute";
    public static final String TASK_MANAGE = "task:manage";

    // ==================== 教学管理权限 ====================
    public static final String TEACHING_CLASSROOM_ADD = "teaching:classroom:add";
    public static final String TEACHING_CLASSROOM_DELETE = "teaching:classroom:delete";
    public static final String TEACHING_CLASSROOM_EDIT = "teaching:classroom:edit";
    public static final String TEACHING_CLASSROOM_LIST = "teaching:classroom:list";
    public static final String TEACHING_CLASSROOM_VIEW = "teaching:classroom:view";

    // ==================== 微信权限 ====================
    public static final String WECHAT_PUSH_SEND = "wechat:push:send";
    public static final String WECHAT_PUSH_VIEW = "wechat:push:view";

    // ==================== 工作流权限 ====================
    public static final String WORKFLOW_CREATE = "workflow:create";
    public static final String WORKFLOW_DELETE = "workflow:delete";
    public static final String WORKFLOW_DEPLOY = "workflow:deploy";
    public static final String WORKFLOW_UPDATE = "workflow:update";
    public static final String WORKFLOW_VIEW = "workflow:view";
}
