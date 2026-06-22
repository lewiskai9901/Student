package com.school.management.infrastructure.extension.plugins.education.infrastructure.access;

import com.school.management.infrastructure.extension.RecordRelationResolver;
import com.school.management.infrastructure.extension.ScopeContext;
import com.school.management.infrastructure.extension.SqlFragment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * R3c PROVIDER 样板:「任课老师」关系解析器 —— "老师任课的学生"。
 *
 * <p>核心系统不懂"任课"逻辑;它藏在教务的 {@code teacher_assignments}(老师↔所教组织)。
 * 本 resolver 返回参数化子查询:当前老师所教的组织(班级)下的全部学生记录 id。引擎包成
 * {@code s.id IN (<本子查询>)} 下推数据库,走 teacher_assignments / user_student 索引。
 *
 * <p>注册见 {@code EducationManifest.resourceRelations()}:
 * {@code ResourceRelationDef.provider("student","taught_by","任课老师","USER","teachingStudentResolver")}。
 */
@Component("teachingStudentResolver")
public class TeachingStudentResolver implements RecordRelationResolver {

    @Override
    public SqlFragment subquery(ScopeContext ctx) {
        // 老师(:me) → teacher_assignments 其所教组织(班级) → access_relations 该班 member 学生 → user_student.id
        // (学生班级归属在 access_relations, user_student 无 org_unit_id 列 — 故跨 teacher_assignments + 主体图组合)
        return SqlFragment.of(
            "SELECT us.id FROM user_student us " +
            "JOIN access_relations ar ON ar.subject_id = us.user_id AND ar.relation = 'member' " +
            "  AND ar.resource_type = 'org_unit' AND ar.subject_type = 'user' AND ar.deleted = 0 " +
            "JOIN teacher_assignments ta ON ta.org_unit_id = ar.resource_id " +
            "  AND ta.teacher_id = :me AND ta.is_current = 1 AND ta.deleted = 0",
            Map.of("me", ctx.userId())
        );
    }
}
