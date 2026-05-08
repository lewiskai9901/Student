package com.school.management.infrastructure.extension.plugins.core;

/**
 * 平台核心关系码常量 (任何行业都会用到, 不可删除).
 *
 * <p>Phase 2 W2.2: 从已删除的 {@code CoreRelationsPlugin} 提取到独立常量类,
 * 供业务代码引用 (避免裸字符串). 关系类型本身现在通过
 * {@link com.school.management.infrastructure.extension.plugins.core.CoreManifest#contribute()}
 * 声明.
 */
public final class CoreRelations {
    /** 用户属于某组织 (subject=user, resource=org_unit) */
    public static final String MEMBER       = "member";
    /** 组织/场所主管理员 (subject=user) — 单一, transitive over org */
    public static final String ADMIN        = "admin";
    /** 组织副管理员 (subject=user, resource=org_unit) */
    public static final String DEPUTY       = "deputy";
    /** 场所管理者 (subject=user, resource=place) — 派生 viewer over occupants */
    public static final String MANAGES      = "manages";
    /** 场所归属某组织 (subject=place, resource=org_unit) */
    public static final String BELONGS_TO   = "belongs_to";
    /** 用户占用场所 (subject=user, resource=place) — 受 capacity 限制 */
    public static final String OCCUPIES     = "occupies";
    /** 权限临时委托给另一用户 (subject=user, resource=user) */
    public static final String DELEGATED_TO = "delegated_to";
    /** 用户订阅某组织的动态 (subject=user, resource=org_unit) */
    public static final String WATCHES      = "watches";

    private CoreRelations() {}
}
