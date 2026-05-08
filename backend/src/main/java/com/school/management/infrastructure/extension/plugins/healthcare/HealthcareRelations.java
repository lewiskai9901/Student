package com.school.management.infrastructure.extension.plugins.healthcare;

/**
 * 医疗场景关系码常量, 业务代码引用.
 *
 * <p>Phase 2 W2.2: 从已删除的 {@code HealthcareRelationsPlugin} 提取到独立常量类.
 * 关系类型本身现在通过
 * {@link com.school.management.infrastructure.extension.plugins.healthcare.HealthcareManifest#contribute()}
 * 声明.
 */
public final class HealthcareRelations {
    // Phase 3 W3.2: FAMILY_OF 已删, 业务请改用 CommonExtRelations.FAMILY_OF.
    /** 主治医师 (subject=医师, resource=病人) */
    public static final String ATTENDING_OF = "attending_of";
    /** 责任护士 (subject=护士, resource=病人) */
    public static final String NURSE_OF     = "nurse_of";
    /** 病人或医护所在病区 (subject=user, resource=org_unit 病区, transitive) */
    public static final String IN_WARD      = "in_ward";

    private HealthcareRelations() {}
}
