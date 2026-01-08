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

    // ==================== 综合测评权限 ====================
    public static final String EVALUATION_BEHAVIOR_CREATE = "evaluation:behavior:create";
    public static final String EVALUATION_BEHAVIOR_DELETE = "evaluation:behavior:delete";
    public static final String EVALUATION_BEHAVIOR_LIST = "evaluation:behavior:list";
    public static final String EVALUATION_BEHAVIOR_UPDATE = "evaluation:behavior:update";
    public static final String EVALUATION_CONFIG_LIST = "evaluation:config:list";
    public static final String EVALUATION_CONFIG_UPDATE = "evaluation:config:update";
    public static final String EVALUATION_COURSE_CREATE = "evaluation:course:create";
    public static final String EVALUATION_COURSE_DELETE = "evaluation:course:delete";
    public static final String EVALUATION_COURSE_IMPORT = "evaluation:course:import";
    public static final String EVALUATION_COURSE_LIST = "evaluation:course:list";
    public static final String EVALUATION_COURSE_UPDATE = "evaluation:course:update";
    public static final String EVALUATION_HONOR_APPLY = "evaluation:honor:apply";
    public static final String EVALUATION_HONOR_CLASS_REVIEW = "evaluation:honor:class-review";
    public static final String EVALUATION_HONOR_CREATE = "evaluation:honor:create";
    public static final String EVALUATION_HONOR_DELETE = "evaluation:honor:delete";
    public static final String EVALUATION_HONOR_DEPT_REVIEW = "evaluation:honor:dept-review";
    public static final String EVALUATION_HONOR_EXPORT = "evaluation:honor:export";
    public static final String EVALUATION_HONOR_LIST = "evaluation:honor:list";
    public static final String EVALUATION_HONOR_SCHOOL_REVIEW = "evaluation:honor:school-review";
    public static final String EVALUATION_HONOR_UPDATE = "evaluation:honor:update";
    public static final String EVALUATION_PERIOD_CREATE = "evaluation:period:create";
    public static final String EVALUATION_PERIOD_DELETE = "evaluation:period:delete";
    public static final String EVALUATION_PERIOD_LIST = "evaluation:period:list";
    public static final String EVALUATION_PERIOD_LOCK = "evaluation:period:lock";
    public static final String EVALUATION_PERIOD_MANAGE = "evaluation:period:manage";
    public static final String EVALUATION_PERIOD_UPDATE = "evaluation:period:update";
    public static final String EVALUATION_RESULT_CALCULATE = "evaluation:result:calculate";
    public static final String EVALUATION_RESULT_EXPORT = "evaluation:result:export";
    public static final String EVALUATION_RESULT_LIST = "evaluation:result:list";
    public static final String EVALUATION_RESULT_MY = "evaluation:result:my";
    public static final String EVALUATION_RESULT_SYNC = "evaluation:result:sync";
    public static final String EVALUATION_SCORE_DELETE = "evaluation:score:delete";
    public static final String EVALUATION_SCORE_EXPORT = "evaluation:score:export";
    public static final String EVALUATION_SCORE_IMPORT = "evaluation:score:import";
    public static final String EVALUATION_SCORE_INPUT = "evaluation:score:input";
    public static final String EVALUATION_SCORE_LIST = "evaluation:score:list";
    public static final String EVALUATION_SCORE_LOCK = "evaluation:score:lock";
    public static final String EVALUATION_SCORE_MY = "evaluation:score:my";
    public static final String EVALUATION_SCORE_UPDATE = "evaluation:score:update";

    // ==================== 年级方向权限 ====================
    public static final String GRADE_DIRECTION_ADD = "grade:direction:add";
    public static final String GRADE_DIRECTION_DELETE = "grade:direction:delete";
    public static final String GRADE_DIRECTION_EDIT = "grade:direction:edit";
    public static final String GRADE_DIRECTION_INFO = "grade:direction:info";
    public static final String GRADE_DIRECTION_LIST = "grade:direction:list";

    // ==================== 专业权限 ====================
    public static final String MAJOR_ADD = "major:add";
    public static final String MAJOR_DELETE = "major:delete";
    public static final String MAJOR_DIRECTION_ADD = "major:direction:add";
    public static final String MAJOR_DIRECTION_DELETE = "major:direction:delete";
    public static final String MAJOR_DIRECTION_EDIT = "major:direction:edit";
    public static final String MAJOR_DIRECTION_INFO = "major:direction:info";
    public static final String MAJOR_DIRECTION_LIST = "major:direction:list";
    public static final String MAJOR_EDIT = "major:edit";
    public static final String MAJOR_INFO = "major:info";
    public static final String MAJOR_LIST = "major:list";

    // ==================== 量化考核权限 ====================
    public static final String QUANTIFICATION_APPEAL_CREATE = "quantification:appeal:create";
    public static final String QUANTIFICATION_APPEAL_LIST = "quantification:appeal:list";
    public static final String QUANTIFICATION_APPEAL_MANAGE = "quantification:appeal:manage";
    public static final String QUANTIFICATION_APPEAL_REVIEW = "quantification:appeal:review";
    public static final String QUANTIFICATION_APPEAL_STATISTICS = "quantification:appeal:statistics";
    public static final String QUANTIFICATION_APPEAL_VIEW = "quantification:appeal:view";
    public static final String QUANTIFICATION_APPEAL_WITHDRAW = "quantification:appeal:withdraw";
    public static final String QUANTIFICATION_CHECK_ADD = "quantification:check:add";
    public static final String QUANTIFICATION_CHECK_DELETE = "quantification:check:delete";
    public static final String QUANTIFICATION_CHECK_DETAIL = "quantification:check:detail";
    public static final String QUANTIFICATION_CHECK_EDIT = "quantification:check:edit";
    public static final String QUANTIFICATION_CHECK_LIST = "quantification:check:list";
    public static final String QUANTIFICATION_CHECK_PUBLISH = "quantification:check:publish";
    public static final String QUANTIFICATION_CHECK_REVIEW = "quantification:check:review";
    public static final String QUANTIFICATION_CHECK_SCORE = "quantification:check:score";
    public static final String QUANTIFICATION_CHECK_SUBMIT = "quantification:check:submit";
    public static final String QUANTIFICATION_CHECK_VIEW = "quantification:check:view";
    public static final String QUANTIFICATION_CHECK_RECORD_PUBLISH = "quantification:check-record:publish";
    public static final String QUANTIFICATION_CHECK_RECORD_REVIEW = "quantification:check-record:review";
    public static final String QUANTIFICATION_CONFIG_ADD = "quantification:config:add";
    public static final String QUANTIFICATION_CONFIG_DELETE = "quantification:config:delete";
    public static final String QUANTIFICATION_CONFIG_EDIT = "quantification:config:edit";
    public static final String QUANTIFICATION_CONFIG_VIEW = "quantification:config:view";
    public static final String QUANTIFICATION_DICTIONARY_CATEGORY = "quantification:dictionary:category";
    public static final String QUANTIFICATION_DICTIONARY_ITEM = "quantification:dictionary:item";
    public static final String QUANTIFICATION_GRADE_ADD = "quantification:grade:add";
    public static final String QUANTIFICATION_GRADE_DELETE = "quantification:grade:delete";
    public static final String QUANTIFICATION_GRADE_EDIT = "quantification:grade:edit";
    public static final String QUANTIFICATION_GRADE_VIEW = "quantification:grade:view";
    public static final String QUANTIFICATION_PLAN_ADD = "quantification:plan:add";
    public static final String QUANTIFICATION_PLAN_DELETE = "quantification:plan:delete";
    public static final String QUANTIFICATION_PLAN_EDIT = "quantification:plan:edit";
    public static final String QUANTIFICATION_PLAN_MANAGE = "quantification:plan:manage";
    public static final String QUANTIFICATION_PLAN_VIEW = "quantification:plan:view";
    public static final String QUANTIFICATION_RECORD_ARCHIVE = "quantification:record:archive";
    public static final String QUANTIFICATION_RECORD_CREATE = "quantification:record:create";
    public static final String QUANTIFICATION_RECORD_DELETE = "quantification:record:delete";
    public static final String QUANTIFICATION_RECORD_EDIT = "quantification:record:edit";
    public static final String QUANTIFICATION_RECORD_VIEW = "quantification:record:view";
    public static final String QUANTIFICATION_STATISTICS_EDIT = "quantification:statistics:edit";
    public static final String QUANTIFICATION_STATISTICS_VIEW = "quantification:statistics:view";
    public static final String QUANTIFICATION_TEMPLATE_ADD = "quantification:template:add";
    public static final String QUANTIFICATION_TEMPLATE_DELETE = "quantification:template:delete";
    public static final String QUANTIFICATION_TEMPLATE_EDIT = "quantification:template:edit";
    public static final String QUANTIFICATION_TEMPLATE_VIEW = "quantification:template:view";
    public static final String QUANTIFICATION_WEIGHT_CONFIG = "quantification:weight:config";
    public static final String QUANTIFICATION_WEIGHT_CONFIG_SET = "quantification:weight-config:set";
    public static final String QUANTIFICATION_WEIGHT_CONFIG_VIEW = "quantification:weight-config:view";

    // ==================== 学生管理权限 ====================
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
