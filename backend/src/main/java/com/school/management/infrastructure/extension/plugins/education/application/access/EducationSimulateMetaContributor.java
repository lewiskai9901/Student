package com.school.management.infrastructure.extension.plugins.education.application.access;

import com.school.management.application.access.SimulateModuleMetaContributor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 教育模块的数据权限模拟元数据 — 原硬编码在核心 DataPermissionSimulateController
 * 的 switch (school_class→classes), student 模块当时因核心无注册点而被砍掉模拟能力;
 * 2026-06-12 改本贡献点登记, 一并恢复 student 模拟。EDU 禁用时 bean 不存在,
 * 模拟器对这些模块优雅降级 ("此模块未实现模拟")。
 *
 * <p>口径对齐真实 {@code @DataPermission} 执行 (贡献错口径会让模拟结果误导管理员):
 * <ul>
 *   <li><b>school_class</b>: {@code classes} 是 org_units 上的 VIEW —
 *       {@code id}=班级自身 org_unit, {@code org_unit_id}=父年级。SchoolClassMapper
 *       已是 {@code orgUnitField="id"}, 模拟同口径取 id (旧硬编码用 org_unit_id 是错的,
 *       会把 DEPARTMENT 模拟成"下一级")。无 created_by → SELF 不支持。</li>
 *   <li><b>student</b>: 学生归属 = access_relations member 关系 (subject=user_student.user_id,
 *       resource=班级 org), 与 DddStudentMapper viaMembership=true/membershipSubjectColumn=user_id
 *       一致 → membership 模型, 匹配列 user_id。user_student 无 created_by → SELF 模拟不支持
 *       (真实 SELF 走 user_id 短路, 模拟器 MVP 未覆盖)。</li>
 * </ul>
 */
@Component
public class EducationSimulateMetaContributor implements SimulateModuleMetaContributor {

    @Override
    public Map<String, SimulateModuleMeta> contribute() {
        SimulateModuleMeta schoolClass =
                SimulateModuleMeta.orgColumn("classes", "class_name", "id", true, false);
        SimulateModuleMeta student =
                SimulateModuleMeta.membership("user_student", "name", "user_id", true, false);
        return Map.of(
                "school_class", schoolClass,
                "class", schoolClass,
                "student", student
        );
    }
}
