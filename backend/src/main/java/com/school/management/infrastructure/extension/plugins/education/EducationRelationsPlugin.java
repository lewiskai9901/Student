package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.RelationTypePlugin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.school.management.infrastructure.extension.RelationTypePlugin.RelationTypeDef.of;

/**
 * 教育行业关系插件.
 *
 * 声明教育场景特有的关系类型:
 *  - guardian_of: 家长监护学生 (注: 医院/养老也用这个关系,未来可拆到 CommonExtPlugin)
 *  - teaches:     教师任课班级
 *  - advisor_of:  辅导员负责年级
 *  - mentor_of:   导师指导学生
 *
 * 业务代码引用时应使用 {@link EducationRelations} 常量,避免裸字符串.
 */
@Component
public class EducationRelationsPlugin implements RelationTypePlugin {

    @Override
    public String getSourceName() { return "EducationPlugin"; }

    @Override
    public String getTier() { return "DOMAIN"; }

    @Override
    public List<RelationTypeDef> getRelationTypes() {
        return List.of(
            of(EducationRelations.GUARDIAN_OF, "user", "user", "监护",
               "ASSOCIATION",
               "家长监护学生 (subject=家长, resource=学生). " +
               "消息扇出时用 BY_RELATION(guardian_of, inward) 查学生的家长"),

            // 教师任课: 按班级数量限制(例: 班级最多 10 个任课老师)
            of(EducationRelations.TEACHES, "user", "org_unit", "任课",
               "ASSOCIATION", "教师任教班级,绑定课程和学期")
                .withMaxBySubtype(Map.of("CLASS", 10)),

            // 辅导员: 班级 1 人 (班主任),年级可多人 (年级主任团队),其他组织不限
            of(EducationRelations.ADVISOR_OF, "user", "org_unit", "辅导员",
               "OWNERSHIP", "辅导员负责年级或班级").transitive()
                .withMaxBySubtype(Map.of("CLASS", 1, "GRADE", 3)),

            of(EducationRelations.MENTOR_OF, "user", "user", "导师",
               "ASSOCIATION", "导师指导学生")
        );
    }

    /** 教育场景关系码常量,业务代码引用 */
    public static final class EducationRelations {
        public static final String GUARDIAN_OF = "guardian_of";
        public static final String TEACHES     = "teaches";
        public static final String ADVISOR_OF  = "advisor_of";
        public static final String MENTOR_OF   = "mentor_of";
        private EducationRelations() {}
    }
}
