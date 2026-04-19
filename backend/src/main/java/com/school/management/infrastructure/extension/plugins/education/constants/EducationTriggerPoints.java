package com.school.management.infrastructure.extension.plugins.education.constants;

/**
 * 教育行业触发点常量 — 学籍/教学/宿舍/考勤 等教育特有业务事件.
 *
 * 按业务域分组组织.卸载 education plugin 时,这些触发点会全部禁用.
 */
public final class EducationTriggerPoints {
    private EducationTriggerPoints() {}

    // ─── 宿舍 Dormitory ───
    // 已废弃: DORM_CHECKIN / DORM_CHECKOUT
    // 改用通用核心 CorePlaceTriggerPoints.PLACE_OCCUPIED / PLACE_VACATED,
    // 宿舍语义由 DormitoryMessagingPlugin 通过订阅转译 (DORM_CHECKIN_EVT / DORM_CHECKOUT_EVT).

    // ─── 成绩 Grade ───
    /** 成绩录入提交 */
    public static final String GRADE_SUBMITTED        = "GRADE_SUBMITTED";
    /** 成绩审核通过 */
    public static final String GRADE_APPROVED         = "GRADE_APPROVED";
    /** 成绩公示(班级级别) */
    public static final String GRADE_PUBLISHED        = "GRADE_PUBLISHED";
    /** 成绩发放(个人级别,定向通知学生/家长) */
    public static final String GRADE_PUBLISHED_PERSONAL = "GRADE_PUBLISHED_PERSONAL";

    // ─── 考试 Exam ───
    /** 考试安排发布 */
    public static final String EXAM_PUBLISHED = "EXAM_PUBLISHED";

    // ─── 课程表 Schedule ───
    /** 课程表发布 */
    public static final String SCHEDULE_PUBLISHED = "SCHEDULE_PUBLISHED";

    // ─── 招生 / 报到 Enrollment ───
    /** 录取确认 */
    public static final String ENROLLMENT_ADMITTED   = "ENROLLMENT_ADMITTED";
    /** 新生报到注册 */
    public static final String ENROLLMENT_REGISTERED = "ENROLLMENT_REGISTERED";

    // ─── 学籍 Student ───
    /** 学生入学注册(新学籍建立) */
    public static final String STUDENT_ENROLLED       = "STUDENT_ENROLLED";
    /** 学生学籍状态变更(休学/复学/毕业等) */
    public static final String STUDENT_STATUS_CHANGED = "STUDENT_STATUS_CHANGED";

    // ─── 考勤 Attendance ───
    /** 考勤记录(到/迟/缺/请假) */
    public static final String ATTENDANCE_RECORDED = "ATTENDANCE_RECORDED";
}
