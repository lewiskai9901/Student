package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.Contribution;
import com.school.management.infrastructure.extension.DataResourceDef;
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
        return Stream.concat(relationTypes(),
               Stream.concat(roleScopeBindings(),
               Stream.concat(rolePermissionBindings(), dataResources())));
    }

    /**
     * 教育行业数据资源 scope 声明 (Phase 1 双轨收敛: 从已删的 EducationDataResourceProvider 迁入)。
     * 学生类加年级/班级/专业维度; 成绩/考试跨年级不给 BY_GRADE; 宿舍/招生组织向。
     */
    private Stream<Contribution> dataResources() {
        return Stream.of(
            dr("student",    "ALL", "BY_GRADE", "BY_CLASS", "BY_MAJOR", "SELF", "CUSTOM"),
            dr("attendance", "ALL", "BY_GRADE", "BY_CLASS", "BY_MAJOR", "SELF", "CUSTOM"),
            dr("grade_batch",   "ALL", "BY_CLASS", "SELF", "CUSTOM"),
            dr("student_grade", "ALL", "BY_CLASS", "SELF", "CUSTOM"),
            dr("exam",          "ALL", "BY_CLASS", "SELF", "CUSTOM"),
            dr("exam_batch",    "ALL", "BY_CLASS", "SELF", "CUSTOM"),
            dr("teaching_task", "ALL", "BY_CLASS", "BY_MAJOR", "SELF", "CUSTOM"),
            dr("school_class",  "ALL", "BY_CLASS", "BY_MAJOR", "SELF", "CUSTOM"),
            dr("dormitory",  "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            dr("enrollment", "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM")
        );
    }

    private static Contribution.DataResourceContribution dr(String code, String... scopes) {
        return new Contribution.DataResourceContribution(DataResourceDef.of(code, scopes));
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
            Contribution.RoleScopeBindingContribution.bind("PARENT", "attendance", "SELF"),

            // teacher_preference — 三级 (P6-2 修二极化, V20260516_8)
            Contribution.RoleScopeBindingContribution.bind("TEACHER", "teacher_preference", "SELF"),
            Contribution.RoleScopeBindingContribution.bind("CLASS_TEACHER", "teacher_preference", "SELF"),
            Contribution.RoleScopeBindingContribution.bind("SUBJECT_TEACHER", "teacher_preference", "SELF"),
            Contribution.RoleScopeBindingContribution.bind("DEPT_ADMIN", "teacher_preference", "DEPARTMENT_AND_BELOW"),
            Contribution.RoleScopeBindingContribution.bind("ACADEMIC_DIRECTOR", "teacher_preference", "ALL"),
            Contribution.RoleScopeBindingContribution.bind("SCHOOL_ADMIN", "teacher_preference", "ALL")
        );
    }

    /**
     * 默认功能权限矩阵 — role × permission_code 绑定, 由
     * {@link com.school.management.infrastructure.extension.RolePermissionBindingRegistrar}
     * (@Order 600) INSERT IGNORE 到 role_permissions (admin 在 UI 的调整不被覆盖)。
     *
     * <p>设计原则 (Casbin 缺口修复, 2026-06-12):
     * <ul>
     *   <li><b>最小可用集</b> — 只授角色日常工作流必需的码, view/execute 优先;</li>
     *   <li><b>不授任何 :delete</b> — 删除一律留给 admin 在 UI 显式授予;</li>
     *   <li><b>不授 system:role / system:permission / tenant:*</b> — 权限管理权只属于
     *       TENANT_ADMIN (CoreManifest 声明) 和超管;</li>
     *   <li>STUDENT / PARENT 不绑 — 它们的端点是 PUBLIC/SELF scope, Casbin 不拦,
     *       行级隔离由数据权限层 (SELF scope, 见 roleScopeBindings) 负责。</li>
     * </ul>
     * 码值守护: PluginDeclarationCoverageTest Test 5 — 引用未声明的角色码/权限码会挂构建。
     */
    private Stream<Contribution> rolePermissionBindings() {
        return Stream.of(
            // ─── SCHOOL_ADMIN 学校管理员: 全业务面 view + 业务 edit (无 delete / 无权限管理) ───
            Contribution.RolePermissionBindingContribution.bindAll("SCHOOL_ADMIN",
                "admin:access", "dashboard:view",
                "system:org:view", "system:org:edit", "system:org:create", "system:org:update",
                "system:user:view", "system:user:edit", "system:user:add",
                "system:audit:view", "system:config:view", "system:message:manage",
                "student:info:view", "student:info:edit", "student:info:add",
                "student:class:view", "student:class:edit", "student:class:add",
                "student:cohort:view", "student:cohort:edit",
                "student:attendance:view", "student:attendance:edit",
                "student:warning:view", "student:warning:edit",
                "teacher:profile:view", "teacher:profile:edit",
                "enrollment:view", "enrollment:edit",
                "academic:major:view", "academic:curriculum:view",
                "teaching:task:view", "teaching:schedule:view", "teaching:grade:view", "teaching:exam:view",
                "place:view", "place:edit", "place:add",
                "student:dormitory:view",
                "asset:manage:view", "asset:manage:edit", "asset:borrow:view",
                "insp:platform:view", "insp:template:view", "insp:project:view", "insp:analytics:view",
                "inspection_appeal:view", "inspection_appeal:review",
                "calendar:view", "calendar:edit"),

            // ─── ACADEMIC_DIRECTOR 教务主任: 教学条线全权, 学生/班级只读 ───
            Contribution.RolePermissionBindingContribution.bindAll("ACADEMIC_DIRECTOR",
                "dashboard:view",
                "teaching:task:view", "teaching:task:edit",
                "teaching:schedule:view", "teaching:schedule:edit",
                "teaching:grade:view", "teaching:grade:edit",
                "teaching:exam:view", "teaching:exam:edit",
                "teaching:offering:view", "teaching:offering:edit",
                "teaching:class:view", "teaching:class:edit",
                "teaching:constraint:view", "teaching:constraint:edit",
                "teaching:workflow:view", "teaching:workflow:edit",
                "academic:curriculum:view", "academic:curriculum:edit",
                "academic:major:view", "academic:major:edit",
                "academic:course:view", "academic:course:edit",
                "academic:grade-direction:view", "academic:grade-direction:edit",
                "schedule:policy:view", "schedule:policy:manage",
                "student:info:view", "student:class:view", "student:cohort:view",
                "teacher:profile:view",
                "enrollment:view", "enrollment:edit",
                "calendar:view", "calendar:edit"),

            // ─── DEPT_ADMIN 系部管理员: 本系业务 view + 学生/班级 edit (数据层 DEPARTMENT_AND_BELOW 圈范围) ───
            Contribution.RolePermissionBindingContribution.bindAll("DEPT_ADMIN",
                "dashboard:view",
                "student:info:view", "student:info:edit",
                "student:class:view", "student:class:edit",
                "student:attendance:view", "student:warning:view",
                "teacher:profile:view",
                "teaching:task:view", "teaching:schedule:view", "teaching:grade:view",
                "academic:major:view", "academic:course:view",
                "system:org:view",
                "insp:analytics:view", "inspection_record:view",
                "calendar:view"),

            // ─── GRADE_DIRECTOR 年级主任: 年级面 view + 预警处置 (数据层 BY_GRADE 圈范围) ───
            Contribution.RolePermissionBindingContribution.bindAll("GRADE_DIRECTOR",
                "dashboard:view",
                "student:info:view", "student:class:view",
                "student:attendance:view",
                "student:warning:view", "student:warning:edit",
                "teaching:grade:view", "teaching:schedule:view",
                "insp:analytics:view", "insp:received:view", "inspection_record:view",
                "calendar:view"),

            // ─── CLASS_TEACHER 班主任: 本班学生全面管理 + 检查执行/申诉 (数据层 BY_CLASS 圈范围) ───
            Contribution.RolePermissionBindingContribution.bindAll("CLASS_TEACHER",
                "dashboard:view",
                "student:info:view", "student:info:edit",
                "student:attendance:view", "student:attendance:edit",
                "student:warning:view", "student:warning:edit",
                "student:class:view", "student:myclass:view",
                "teaching:grade:view", "teaching:schedule:view",
                "insp:task:view", "insp:task:execute",
                "insp:submission:view", "insp:submission:create", "insp:submission:execute",
                "insp:received:view",
                "inspection_appeal:create", "inspection_appeal:view",
                "place:view", "calendar:view"),

            // ─── SUBJECT_TEACHER 任课教师: 成绩录入 + 考勤 (数据层 BY_CLASS 圈任课班) ───
            Contribution.RolePermissionBindingContribution.bindAll("SUBJECT_TEACHER",
                "dashboard:view",
                "student:info:view",
                "student:attendance:view", "student:attendance:edit",
                "teaching:grade:view", "teaching:grade:edit",
                "teaching:task:view", "teaching:schedule:view", "teaching:exam:view",
                "place:view", "calendar:view"),

            // ─── COUNSELOR 辅导员: 学生关怀面 (数据层 DEPARTMENT_AND_BELOW 圈范围) ───
            Contribution.RolePermissionBindingContribution.bindAll("COUNSELOR",
                "dashboard:view",
                "student:info:view",
                "student:attendance:view", "student:attendance:edit",
                "student:warning:view", "student:warning:edit",
                "student:class:view",
                "insp:received:view",
                "calendar:view"),

            // ─── DORMITORY_MANAGER 宿管员: 场所 + 住宿分配 + 卫生检查执行 ───
            Contribution.RolePermissionBindingContribution.bindAll("DORMITORY_MANAGER",
                "dashboard:view",
                "place:view", "place:edit",
                "student:info:view",
                "student:dormitory:view", "dormitory:student:assign",
                "insp:task:view", "insp:task:execute",
                "insp:submission:view", "insp:submission:create", "insp:submission:execute",
                "insp:received:view",
                "calendar:view"),

            // ─── INSPECTOR 检查员: 检查执行全链 ───
            Contribution.RolePermissionBindingContribution.bindAll("INSPECTOR",
                "dashboard:view",
                "insp:task:view", "insp:task:execute",
                "insp:submission:view", "insp:submission:create", "insp:submission:execute",
                "insp:received:view",
                "insp:violation:view", "insp:violation:create",
                "insp:result:view",
                "insp:template:view", "insp:plan:view",
                "place:view", "calendar:view"),

            // ─── TEACHER 教师统一角色: 基础只读面 (职责细分靠 teacher_assignments) ───
            Contribution.RolePermissionBindingContribution.bindAll("TEACHER",
                "dashboard:view",
                "student:info:view",
                "teaching:task:view", "teaching:schedule:view", "teaching:grade:view",
                "teacher:profile:view",
                "insp:received:view",
                "place:view", "calendar:view")
        ).flatMap(List::stream);
    }

    private static Contribution.RelationTypeContribution wrap(RelationTypeDef def) {
        return new Contribution.RelationTypeContribution(SOURCE, TIER, def);
    }
}
