package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.PermissionProvider;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.school.management.infrastructure.extension.PermissionProvider.PermissionDef.of;

/**
 * 教育行业权限 — 学术/学生/教学/宿舍相关.
 *
 * 原 {@code PermissionConstants} 里的学术/学生/教学权限迁移到此,
 * 业务代码可以继续 import 老常量(将来逐步迁移到 EducationPermissions 常量类).
 */
@Component
public class EducationPermissionProvider implements PermissionProvider {

    @Override
    public String getModuleCode() { return "education"; }

    @Override
    public String getModuleName() { return "教育行业"; }

    @Override
    public List<PermissionDef> getPermissions() {
        return List.of(
            // ─── academic 学术 ───
            of("academic:major:view", "查看专业", ""),
            of("academic:major:edit", "编辑专业", ""),
            of("academic:course:view", "查看课程", ""),
            of("academic:course:edit", "编辑课程", ""),
            of("academic:curriculum:view", "查看培养方案", ""),
            of("academic:curriculum:edit", "编辑培养方案", ""),
            of("academic:grade-direction:view", "查看年级方向", ""),
            of("academic:grade-direction:edit", "编辑年级方向", ""),

            // ─── student 学生 ───
            of("student:info:view", "查看学生信息", ""),
            of("student:info:add", "新增学生", ""),
            of("student:info:edit", "编辑学生", ""),
            of("student:info:delete", "删除学生", ""),
            of("student:info:import", "导入学生", ""),
            of("student:info:export", "导出学生", ""),

            of("student:class:view", "查看班级", ""),
            of("student:class:add", "新增班级", ""),
            of("student:class:edit", "编辑班级", ""),
            of("student:class:delete", "删除班级", ""),

            of("student:department:view", "查看院系", ""),
            of("student:department:add", "新增院系", ""),
            of("student:department:edit", "编辑院系", ""),
            of("student:department:delete", "删除院系", ""),

            of("student:dormitory:view", "查看宿舍", ""),
            of("student:dormitory:add", "新增宿舍", ""),
            of("student:dormitory:edit", "编辑宿舍", ""),
            of("student:dormitory:delete", "删除宿舍", ""),

            of("student:grade:view", "查看成绩", ""),
            of("student:grade:edit", "编辑成绩", ""),
            of("student:grade:delete", "删除成绩", ""),

            // ─── teaching 教学 ───
            of("teaching:classroom:view", "查看教室", ""),
            of("teaching:classroom:list", "教室列表", ""),
            of("teaching:classroom:add", "新增教室", ""),
            of("teaching:classroom:edit", "编辑教室", ""),
            of("teaching:classroom:delete", "删除教室", ""),

            // ─── dormitory 宿舍业务 ───
            of("dormitory:student:assign", "分配学生到宿舍", ""),

            // ─── semester 学期 ───
            of("system:semester:list", "查看学期列表", ""),
            of("system:semester:query", "查询学期", ""),
            of("system:semester:add", "新增学期", ""),
            of("system:semester:edit", "编辑学期", ""),
            of("system:semester:delete", "删除学期", ""),

            // ─── building 楼栋 (归教育,因为和校舍强相关) ───
            of("system:building:view", "查看楼栋", ""),
            of("system:building:add", "新增楼栋", ""),
            of("system:building:edit", "编辑楼栋", ""),
            of("system:building:delete", "删除楼栋", ""),

            // ─── 宿舍楼相关 ───
            of("system:dormitory_building:view", "查看宿舍楼", ""),
            of("system:dormitory_building:edit", "编辑宿舍楼", ""),
            of("system:dormitory_building:assign_manager", "分配宿管员", ""),

            // ═══════════════════════════════════════════════════════════════
            // 合并自 EducationLegacyPermissionProvider — 历史 SQL 种子 + @CasbinAccess 补齐
            // ═══════════════════════════════════════════════════════════════

            // ─── academic 顶级节点 ───
            of("academic", "学术管理", ""),
            of("academic:course", "课程管理", ""),
            of("academic:curriculum", "培养方案管理", ""),
            of("academic:grade-direction", "年级专业方向", ""),
            of("academic:major", "专业管理", ""),

            // ─── student 扩展 ───
            of("student:class", "班级管理", ""),
            of("student:create", "创建学生", ""),
            of("student:delete", "删除学生", ""),
            of("student:department", "学生部门管理", ""),
            of("student:grade", "年级管理", ""),
            of("student:info", "学生信息", ""),
            of("student:manage", "学生管理", ""),
            of("student:update", "更新学生", ""),
            of("student:view", "查看学生", ""),
            of("student:attendance:edit", "编辑学生考勤", ""),
            of("student:attendance:view", "查看学生考勤", ""),
            of("student:cohort:edit", "编辑学生届", ""),
            of("student:cohort:view", "查看学生届", ""),
            of("student:warning:edit", "编辑学业预警", ""),
            of("student:warning:view", "查看学业预警", ""),

            // ─── dormitory 宿舍管理 ───
            of("dormitory:building:view", "楼栋查看", ""),
            of("dormitory:create", "创建宿舍", ""),
            of("dormitory:manage", "宿舍管理", ""),
            of("dormitory:room:view", "宿舍查看", ""),
            of("dormitory:update", "更新宿舍", ""),
            of("dormitory:view", "查看宿舍", ""),

            // ─── enrollment 招生 ───
            of("enrollment", "招生管理", ""),
            of("enrollment:edit", "编辑招生", ""),
            of("enrollment:view", "查看招生", ""),

            // ─── teacher 教师 ───
            of("teacher:profile", "教师档案", ""),
            of("teacher:profile:edit", "编辑教师档案", ""),
            of("teacher:profile:view", "查看教师档案", ""),

            // ─── teaching 教务 ───
            of("teaching:building:list", "教学楼栋列表", ""),
            of("teaching:exam:edit", "编辑考试", ""),
            of("teaching:exam:view", "查看考试", ""),
            of("teaching:grade:edit", "编辑成绩", ""),
            of("teaching:grade:view", "查看成绩", ""),
            of("teaching:manage", "教学管理", ""),
            of("teaching:class:edit", "编辑教学班", ""),
            of("teaching:class:view", "查看教学班", ""),
            of("teaching:constraint:edit", "编辑排课约束", ""),
            of("teaching:constraint:view", "查看排课约束", ""),
            of("teaching:offering:edit", "编辑开课", ""),
            of("teaching:offering:view", "查看开课", ""),
            of("teaching:schedule:edit", "编辑课表", ""),
            of("teaching:schedule:view", "查看课表", ""),
            of("teaching:task:edit", "编辑教学任务", ""),
            of("teaching:task:view", "查看教学任务", ""),
            of("teaching:workflow:edit", "编辑教学流程", ""),
            of("teaching:workflow:view", "查看教学流程", ""),

            // ─── inspection 检查记录(教育特定) ───
            of("inspection_record:view", "查看检查记录", ""),

            // ═══════════════════════════════════════════════════════════════
            // 自 CorePermissionProvider 迁入 (2026-04-21) — 教育场景特有
            // 对应 V20260427_1 migration 的 industry=EDU 修正
            // ═══════════════════════════════════════════════════════════════

            // ─── calendar / schedule 日程 (校历+排班是教育场景特有) ───
            of("calendar", "校历管理", ""),
            of("calendar:edit", "编辑校历", ""),
            of("calendar:view", "查看校历", ""),
            of("schedule:policy:manage", "排班策略管理", ""),
            of("schedule:policy:view", "排班管理", ""),

            // ─── quantification 量化考核 (学生工作场景特有) ───
            of("quantification:check-record:publish", "发布检查记录", ""),
            of("quantification:check-record:review", "审核检查记录", ""),
            of("quantification:config:add", "新增量化配置", ""),
            of("quantification:config:delete", "删除量化配置", ""),
            of("quantification:config:edit", "编辑量化配置", ""),
            of("quantification:config:view", "查看量化配置", "")
        );
    }
}
