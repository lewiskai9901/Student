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
        // 注意: 组织管理(/organization)/场所管理(/place)/检查平台(/inspection) 是通用核心模块,
        // 已迁至 CoreMenuPlugin (industry=CORE)。此处仅保留教育行业特有菜单。
        return List.of(
            // ─── 学术管理 ───
            of("/academic", "学术管理", "graduation-cap", 3).children(List.of(
                of("/academic/majors",   "专业",     "book-open",   1)
                    .requiredPermissions(List.of(ACADEMIC_MAJOR_VIEW)),
                of("/academic/courses",  "课程",     "book",        2)
                    .requiredPermissions(List.of(ACADEMIC_COURSE_VIEW)),
                of("/academic/curriculum","培养方案","list-tree",   3)
                    .requiredPermissions(List.of(ACADEMIC_CURRICULUM_VIEW)),
                // 学期管理 — 从 CoreMenuPlugin /system 迁入 (教育特有, 路由仍 /system/semesters)
                of("/system/semesters", "学期管理", "calendar", 4)
                    .requiredPermissions(List.of("system:config:view"))
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

            // ─── 宿舍管理 ───
            of("/dormitory", "宿舍管理", "bed-double", 5).children(List.of(
                of("/dormitory/overview",   "宿舍总览", "layout-grid", 1)
                    .requiredPermissions(List.of(STUDENT_DORMITORY_VIEW)),
                of("/dormitory/occupants",  "住宿管理", "user-check",  2)
                    .requiredPermissions(List.of(DORMITORY_STUDENT_ASSIGN))
            )),

            // ─── 教务管理 ───
            of("/teaching", "教务管理", "school", 20).children(List.of(
                of("/teaching/schedule", "课程表", "calendar-days", 1),
                of("/teaching/exam",     "考试",   "calendar-clock",2),
                of("/teaching/offering", "开课",   "book-copy",     3)
                    .requiredPermissions(List.of(TEACHING_CLASSROOM_VIEW)),
                // 教师档案 — 从 CoreMenuPlugin /system 迁入 (教育特有, 路由仍 /system/teachers)
                of("/system/teachers", "教师档案", "users-round", 4)
                    .requiredPermissions(List.of("system:admin"))
            ))
        );
    }
}
