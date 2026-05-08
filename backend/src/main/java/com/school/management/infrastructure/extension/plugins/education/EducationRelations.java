package com.school.management.infrastructure.extension.plugins.education;

/**
 * 教育场景关系码常量, 业务代码引用.
 *
 * <p>Phase 2 W2.2: 从已删除的 {@code EducationRelationsPlugin} 提取到独立常量类.
 * 关系类型本身现在通过
 * {@link com.school.management.infrastructure.extension.plugins.education.EducationManifest#contribute()}
 * 声明.
 */
public final class EducationRelations {
    // Phase 3 W3.2: GUARDIAN_OF 已删, 业务请改用 CommonExtRelations.FAMILY_OF.
    /** 教师任教班级 (subject=教师, resource=org_unit class) */
    public static final String TEACHES     = "teaches";
    /** 辅导员负责年级或班级 (subject=user, resource=org_unit) */
    public static final String ADVISOR_OF  = "advisor_of";
    /** 导师指导学生 (subject=导师, resource=学生) */
    public static final String MENTOR_OF   = "mentor_of";

    private EducationRelations() {}
}
