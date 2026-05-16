package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.Contribution;
import com.school.management.infrastructure.extension.PluginPackage;
import com.school.management.infrastructure.extension.RelationTypeDef;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 教育行业插件包.
 *
 * 包含:
 *  - 用户类型: Student / Teacher / Parent / Counselor
 *  - 组织类型: School / Department / Grade / Class
 *  - 场所类型: Dormitory / Classroom
 *  - 关系: teaches / mentor_of
 *    (Phase 2 W2.2: EducationRelationsPlugin 已删, 直接在 contribute() 声明)
 *    (Phase 3 W3.2: guardian_of 上移到 COMMON_EXT.family_of, 本插件不再声明)
 *    (Phase 3 W3.3: advisor_of 合并到 CORE.admin + metadata.role=ADVISOR, 本插件不再声明)
 *  - 预置角色: CLASS_TEACHER / SUBJECT_TEACHER / GRADE_DIRECTOR 等
 *  - 业务消息: 入学 / 成绩 / 入住 / 考勤 (后续阶段加)
 *
 * 业务代码引用关系码请用 {@link EducationRelations} 常量.
 */
@Component
public class EducationManifest implements PluginPackage {

    private static final String SOURCE = "EducationPlugin";
    private static final String TIER = "DOMAIN";

    @Override
    public String getIndustryCode() { return "EDU"; }

    @Override
    public String getIndustryName() { return "教育行业"; }

    @Override
    public List<String> getDependsOn() { return List.of("CORE"); }

    /** EDU 要求 CORE >=1.0.0 <2.0.0 (兼容 1.x 核心, 2.0 需要 EDU 升级) */
    @Override
    public java.util.Map<String, String> getDependsOnWithVersion() {
        return java.util.Map.of("CORE", ">=1.0.0 <2.0.0");
    }

    @Override
    public boolean owns(Class<?> pluginClass) {
        // 包路径约定: .plugins.education 或 .plugins.education.* → 属于 EDU
        String pkg = pluginClass.getPackageName();
        return pkg.contains(".plugins.education");
    }

    @Override
    public Stream<Contribution> contribute() {
        return Stream.concat(relationTypes(), roleScopeBindings());
    }

    private Stream<Contribution> relationTypes() {
        return Stream.of(
            // Phase 3 W3.2: guardian_of 已上移到 COMMON_EXT.family_of, 本插件不再声明.
            // 教师任课: 按班级数量限制(例: 班级最多 10 个任课老师)
            wrap(RelationTypeDef.of(EducationRelations.TEACHES, "user", "org_unit", "任课",
                "ASSOCIATION", "教师任教班级,绑定课程和学期")
                .withMaxBySubtype(Map.of("CLASS", 10))),

            // Phase 3 W3.3: advisor_of 已合并到 CORE.admin (写 metadata.role='ADVISOR'/'CLASS_TEACHER').
            wrap(RelationTypeDef.of(EducationRelations.MENTOR_OF, "user", "user", "导师",
                "ASSOCIATION", "导师指导学生"))
        );
    }

    /**
     * P5-1: EDU 插件声明默认 role × resource × scope 绑定.
     *
     * 启动期 ContributionDispatcher → RoleScopeBindingRegistrar UPSERT 到
     * role_data_scopes (INSERT IGNORE — admin 手动调整不被覆盖).
     *
     * 之前这些绑定靠 V20260516_3 migration 手写, 现在搬到代码声明,
     * 新部署不需要那个 migration 也能跑通.
     */
    private Stream<Contribution> roleScopeBindings() {
        return Stream.of(
            // SCHOOL_ADMIN — 全校 ALL on student/class/dashboard
            Contribution.RoleScopeBindingContribution.bind("SCHOOL_ADMIN", "student", "ALL"),
            Contribution.RoleScopeBindingContribution.bind("SCHOOL_ADMIN", "school_class", "ALL"),
            Contribution.RoleScopeBindingContribution.bind("SCHOOL_ADMIN", "dashboard", "ALL"),
            Contribution.RoleScopeBindingContribution.bind("SCHOOL_ADMIN", "teaching_task", "ALL"),

            // ACADEMIC_DIRECTOR — 教务全权 ALL on teaching/academic
            Contribution.RoleScopeBindingContribution.bind("ACADEMIC_DIRECTOR", "teaching_task", "ALL"),
            Contribution.RoleScopeBindingContribution.bind("ACADEMIC_DIRECTOR", "student", "DEPARTMENT_AND_BELOW"),
            Contribution.RoleScopeBindingContribution.bind("ACADEMIC_DIRECTOR", "school_class", "DEPARTMENT_AND_BELOW"),

            // DEPT_ADMIN — 系部及以下
            Contribution.RoleScopeBindingContribution.bind("DEPT_ADMIN", "student", "DEPARTMENT_AND_BELOW"),
            Contribution.RoleScopeBindingContribution.bind("DEPT_ADMIN", "school_class", "DEPARTMENT_AND_BELOW"),
            Contribution.RoleScopeBindingContribution.bind("DEPT_ADMIN", "dashboard", "DEPARTMENT_AND_BELOW"),

            // GRADE_DIRECTOR — 按年级反查 (P2 后 Resolver 真工作)
            Contribution.RoleScopeBindingContribution.bind("GRADE_DIRECTOR", "student", "BY_GRADE"),
            Contribution.RoleScopeBindingContribution.bind("GRADE_DIRECTOR", "school_class", "BY_GRADE"),

            // CLASS_TEACHER — 按班级反查
            Contribution.RoleScopeBindingContribution.bind("CLASS_TEACHER", "student", "BY_CLASS"),
            Contribution.RoleScopeBindingContribution.bind("CLASS_TEACHER", "attendance", "BY_CLASS"),
            Contribution.RoleScopeBindingContribution.bind("CLASS_TEACHER", "student_grade", "BY_CLASS"),

            // SUBJECT_TEACHER — 按班级反查 (任课的班级)
            Contribution.RoleScopeBindingContribution.bind("SUBJECT_TEACHER", "student", "BY_CLASS"),
            Contribution.RoleScopeBindingContribution.bind("SUBJECT_TEACHER", "teaching_task", "ALL"),

            // COUNSELOR — 部门范围 (P2 后改 BY_CLASS)
            Contribution.RoleScopeBindingContribution.bind("COUNSELOR", "student", "DEPARTMENT_AND_BELOW"),

            // STUDENT — 末端 SELF
            Contribution.RoleScopeBindingContribution.bind("STUDENT", "student", "SELF"),
            Contribution.RoleScopeBindingContribution.bind("STUDENT", "attendance", "SELF"),
            Contribution.RoleScopeBindingContribution.bind("STUDENT", "student_grade", "SELF"),

            // PARENT — 末端 SELF
            Contribution.RoleScopeBindingContribution.bind("PARENT", "student", "SELF"),
            Contribution.RoleScopeBindingContribution.bind("PARENT", "attendance", "SELF")
        );
    }

    private static Contribution.RelationTypeContribution wrap(RelationTypeDef def) {
        return new Contribution.RelationTypeContribution(SOURCE, TIER, def);
    }
}
