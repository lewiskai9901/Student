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

            // ─── 检查平台 (P0+P1+P2+P3 重构后, 36 → 18 菜单) ───
            of("/inspection", "检查平台", "clipboard-check", 12).children(List.of(
                // 工作台 (角色 hub)
                of("/inspection/dashboard",        "检查平台总览", "layout-dashboard",  0),
                of("/inspection/governance",       "治理工作台",   "shield-check",      1),
                // 我的工作 (按角色)
                of("/inspection/tasks",            "我的任务",     "list-todo",         5),
                of("/inspection/my-record",        "我的成绩单",   "badge-check",       6),
                of("/inspection/my-corrective",    "我的整改",     "wrench",            7),
                of("/inspection/appeals/my",       "我的申诉",     "scale",             8),
                of("/inspection/received/inspections", "我被检查的记录", "clipboard-list", 11),
                of("/inspection/received/trends",      "检查趋势",       "trending-up",    12),
                of("/inspection/received/recurring",   "高频问题",       "alert-triangle", 13),
                of("/inspection/tasks/review-risk","待审风险池",   "list-checks",       9),
                of("/inspection/appeals/review",   "申诉审核",     "gavel",            10),
                // 配置中心
                of("/inspection/projects",         "检查项目",     "folder-search",    20),
                of("/inspection/config",           "检查配置",     "settings",         21),
                of("/inspection/scoring-profiles", "评分方案",     "calculator",       22),
                of("/inspection/grade-schemes",    "等级方案",     "award",            23),
                of("/inspection/issue-categories", "问题分类",     "tags",             24),
                // 数据 / 治理
                of("/inspection/analytics",        "分析报表",     "bar-chart-3",      30),
                of("/inspection/corrective",       "整改管理",     "hammer",           31),
                of("/inspection/alerts",           "预警看板",     "bell",             32),
                of("/inspection/export",           "导出中心",     "download",         33),
                // 高级 (低频)
                of("/inspection/audit-trail",      "审计日志",     "file-search",      40),
                of("/inspection/admin/reassign-departed", "离职重派", "user-x",        99)
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
