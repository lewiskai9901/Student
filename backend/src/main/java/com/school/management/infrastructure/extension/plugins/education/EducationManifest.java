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
 *  - 关系: guardian_of / teaches / advisor_of / mentor_of
 *    (Phase 2 W2.2: EducationRelationsPlugin 已删, 直接在 contribute() 声明)
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
        return Stream.of(
            wrap(RelationTypeDef.of(EducationRelations.GUARDIAN_OF, "user", "user", "监护",
                "ASSOCIATION",
                "家长监护学生 (subject=家长, resource=学生). " +
                "消息扇出时用 BY_RELATION(guardian_of, inward) 查学生的家长")),

            // 教师任课: 按班级数量限制(例: 班级最多 10 个任课老师)
            wrap(RelationTypeDef.of(EducationRelations.TEACHES, "user", "org_unit", "任课",
                "ASSOCIATION", "教师任教班级,绑定课程和学期")
                .withMaxBySubtype(Map.of("CLASS", 10))),

            // 辅导员: 班级 1 人 (班主任),年级可多人 (年级主任团队),其他组织不限
            wrap(RelationTypeDef.of(EducationRelations.ADVISOR_OF, "user", "org_unit", "辅导员",
                "OWNERSHIP", "辅导员负责年级或班级").transitive()
                .withMaxBySubtype(Map.of("CLASS", 1, "GRADE", 3))),

            wrap(RelationTypeDef.of(EducationRelations.MENTOR_OF, "user", "user", "导师",
                "ASSOCIATION", "导师指导学生"))
        );
    }

    private static Contribution.RelationTypeContribution wrap(RelationTypeDef def) {
        return new Contribution.RelationTypeContribution(SOURCE, TIER, def);
    }
}
