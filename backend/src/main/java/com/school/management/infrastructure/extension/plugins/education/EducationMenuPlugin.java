package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.MenuContributionPlugin;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.school.management.infrastructure.extension.MenuContributionPlugin.MenuItemDef.of;
import static com.school.management.infrastructure.extension.plugins.education.constants.EducationPermissions.*;

/**
 * 教育行业菜单插件 — 学术/学生/教务/检查 等教育特有菜单.
 *
 * 换行业(如医疗) = 换这个 plugin.禁用 EDU 插件 → 教育菜单全消失.
 */
@Component
public class EducationMenuPlugin implements MenuContributionPlugin {

    @Override public String getDomainCode() { return "education"; }

    @Override
    public List<MenuItemDef> getMenus() {
        return List.of(
            // ─── 组织管理 ───
            of("/organization", "组织管理", "building-2", 2).children(List.of(
                of("/organization/units", "组织架构", "network", 1)
            )),

            // ─── 学术管理 ───
            of("/academic", "学术管理", "graduation-cap", 3).children(List.of(
                of("/academic/majors",   "专业",     "book-open",   1)
                    .requiredPermissions(List.of(ACADEMIC_MAJOR_VIEW)),
                of("/academic/courses",  "课程",     "book",        2)
                    .requiredPermissions(List.of(ACADEMIC_COURSE_VIEW)),
                of("/academic/curriculum","培养方案","list-tree",   3)
                    .requiredPermissions(List.of(ACADEMIC_CURRICULUM_VIEW))
            )),

            // ─── 学生管理 ───
            of("/student", "学生管理", "users", 4).children(List.of(
                of("/student/list",  "学生花名册",  "user-check", 1)
                    .requiredPermissions(List.of(STUDENT_INFO_VIEW)),
                of("/student/class", "班级管理",    "users-round",2)
                    .requiredPermissions(List.of(STUDENT_CLASS_VIEW)),
                of("/student/grade", "成绩管理",    "award",      3)
                    .requiredPermissions(List.of(STUDENT_GRADE_VIEW))
            )),

            // ─── 场所管理 ───
            of("/place", "场所管理", "map-pin", 5).children(List.of(
                of("/place/management", "场所管理", "map", 1)
            )),

            // ─── 宿舍管理 ───
            of("/dormitory", "宿舍管理", "bed-double", 5).children(List.of(
                of("/dormitory/overview",   "宿舍总览", "layout-grid", 1)
                    .requiredPermissions(List.of(STUDENT_DORMITORY_VIEW)),
                of("/dormitory/occupants",  "住宿管理", "user-check",  2)
                    .requiredPermissions(List.of(DORMITORY_STUDENT_ASSIGN))
            )),

            // ─── 检查平台 ───
            of("/inspection", "检查平台", "clipboard-check", 12).children(List.of(
                of("/inspection/dashboard","检查平台总览", "layout-dashboard", 0),
                of("/inspection/config",   "检查配置",     "settings",         1),
                of("/inspection/projects", "检查项目",     "folder-search",    2),
                of("/inspection/tasks",    "我的任务",     "list-todo",        3),
                of("/inspection/tasks/review",     "任务审核",   "check-square", 4),
                // 角色驱动 IA 新增 5 项
                of("/inspection/my-record",        "我的成绩单", "badge-check",  5),
                of("/inspection/my-corrective",    "我的整改",   "wrench",       6),
                of("/inspection/tasks/review-risk","待审风险池", "list-checks",  7),
                of("/inspection/governance",       "治理工作台", "shield-check", 8),
                of("/inspection/analytics","分析报表",     "bar-chart-3",      9),
                of("/inspection/export",   "导出中心",     "download",        10),
                of("/inspection/corrective",       "整改管理",   "hammer",       10),
                of("/inspection/appeals/my",       "我的申诉",   "scale",        10),
                of("/inspection/appeals/review",   "申诉审核",   "gavel",        10),
                // 配置/资源 — 评分体系
                of("/inspection/library",         "检查项库",     "library",      11),
                of("/inspection/grade-schemes",   "等级方案",     "award",        12),
                of("/inspection/scoring-profiles","评分方案",     "calculator",   13),
                of("/inspection/scoring",         "评分配置",     "sliders",      14),
                of("/inspection/issue-categories","问题分类",     "tags",         15),
                of("/inspection/compliance",      "合规标准",     "shield",       16),
                of("/inspection/holiday-calendar","假日日历",     "calendar",     17),
                of("/inspection/report-templates","报表模板",     "file-text",    18),
                // 监控/数据
                of("/inspection/results",         "检查结果",     "trophy",       19),
                of("/inspection/observations",    "评分观察",     "eye",          20),
                of("/inspection/rankings",        "评级排名",     "trending-up",  21),
                of("/inspection/ratings",         "评级排名榜",   "trophy",       22),
                of("/inspection/alerts",          "预警看板",     "bell",         23),
                of("/inspection/audit-trail",     "审计日志",     "file-search",  24),
                of("/inspection/big-screen",      "数据大屏",     "monitor",      25),
                of("/inspection/about-me",        "关于我的检查", "user-circle",  26),
                // 集成/扩展
                of("/inspection/notification-rules","通知规则",   "mail",         27),
                of("/inspection/webhooks",        "Webhook",     "link-2",       28),
                of("/inspection/knowledge",       "知识库",       "book-open",    29),
                of("/inspection/nfc-tags",        "NFC 标签",    "scan-line",    30),
                of("/inspection/iot-sensors",     "IoT 传感器",  "cpu",          31),
                of("/inspection/admin/reassign-departed","离职重派","user-x",     90)
            )),

            // ─── 教务管理 ───
            of("/teaching", "教务管理", "school", 20).children(List.of(
                of("/teaching/schedule", "课程表", "calendar-days", 1),
                of("/teaching/exam",     "考试",   "calendar-clock",2),
                of("/teaching/offering", "开课",   "book-copy",     3)
                    .requiredPermissions(List.of(TEACHING_CLASSROOM_VIEW))
            ))
        );
    }
}
