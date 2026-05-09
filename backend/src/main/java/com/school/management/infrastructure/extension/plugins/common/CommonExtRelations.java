package com.school.management.infrastructure.extension.plugins.common;

/**
 * 跨行业通用扩展关系码常量 (tier=COMMON_EXT).
 *
 * <p>Phase 3 W3.2: 把跨行业的人际关系从行业插件上移到这里, 避免不同行业插件
 * 重复声明同一概念 (如 EDU.guardian_of 概念合并为本类的 family_of).
 */
public final class CommonExtRelations {
    /** 家属/家长 (subject=user, resource=user) — 跨学校(家长↔学生)/医院(家属↔病人)/养老 通用 */
    public static final String FAMILY_OF         = "family_of";
    /** 紧急联系人 (subject=user, resource=user) — 入院/事故/失联通用 */
    public static final String EMERGENCY_CONTACT = "emergency_contact";

    private CommonExtRelations() {}
}
