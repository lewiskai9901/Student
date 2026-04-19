package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.RolePresetPlugin;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 教育行业预置角色.
 *
 * 之前这些角色在 SQL migration (V20260414_4) 里硬编码 INSERT,
 * 现改由插件声明,支持:
 *   - 启动自动 UPSERT
 *   - 卸载插件时统一禁用/清理
 *   - is_plugin_preset=1 admin 不可删
 */
@Component
public class EducationRolePresetPlugin implements RolePresetPlugin {

    @Override
    public List<RolePresetDef> getPresets() {
        return List.of(
            // ─── 管理类 ───
            RolePresetDef.of("SCHOOL_ADMIN", "学校管理员",
                "管理学校所有业务数据,部门/教师/学生/场所等", 10),

            RolePresetDef.of("ACADEMIC_DIRECTOR", "教务主任",
                "教务条线最高负责,审核成绩/考试/课表发布", 15),

            RolePresetDef.of("GRADE_DIRECTOR", "年级主任",
                "负责某年级下所有班级,跨班管理", 20),

            // ─── 教师类 ───
            RolePresetDef.of("CLASS_TEACHER", "班主任",
                "负责班级的全方位管理,拥有本班学生完整数据", 25),

            RolePresetDef.of("SUBJECT_TEACHER", "任课教师",
                "负责某课程的教学,管理课程下的成绩/考勤", 25),

            RolePresetDef.of("COUNSELOR", "辅导员",
                "跨班级辅导,心理/思政等,按分配班级范围工作", 25),

            // ─── 业务支撑 ───
            RolePresetDef.of("DORMITORY_MANAGER", "宿管员",
                "管理宿舍入住/退出/调换/卫生检查", 30),

            RolePresetDef.of("INSPECTOR", "检查员",
                "执行各类检查任务(卫生/安全/纪律)", 30),

            // ─── 教师 & 系部管理(旧版种子认领) ───
            RolePresetDef.of("TEACHER", "教师",
                "教师统一权限角色,职责按 teacher_assignments.role_type 细分", 25),

            RolePresetDef.of("DEPT_ADMIN", "系部管理员",
                "系/院级管理员角色,可查看本组织及下级数据", 15),

            // ─── 末端 ───
            RolePresetDef.of("STUDENT", "学生",
                "学生基本角色,只读自己相关数据", 40),

            RolePresetDef.of("PARENT", "家长",
                "接收子女相关通知,只读权限", 40)
        );
    }
}
