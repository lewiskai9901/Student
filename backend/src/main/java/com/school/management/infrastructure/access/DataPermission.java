package com.school.management.infrastructure.access;

import java.lang.annotation.*;

/**
 * 数据权限注解 (V6 - refactored)
 * 标注在Mapper方法或类上，声明该方法需要数据权限过滤
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 数据模块代码
     * 对应 data_modules 表的 module_code
     */
    String module();

    /**
     * 主表别名（用于SQL拼接）
     */
    String tableAlias() default "";

    /**
     * 是否启用数据权限过滤
     */
    boolean enabled() default true;

    /**
     * 组织单元字段名
     */
    String orgUnitField() default "org_unit_id";

    /**
     * 创建者字段名
     */
    String creatorField() default "created_by";

    /**
     * 资源类型（access_relations 查询用）
     * 非空时，使用 access_relations 子查询做行级过滤
     */
    String resourceType() default "";

    /**
     * 是否按"成员归属"(membership)子查询过滤。
     *
     * <p>用于"主表的行本身就是 access_relations 的 subject(用户)"的场景,
     * 典型是 {@code users} 表 —— 用户的组织归属不再是 users 自己的列,
     * 而是 {@code access_relations} 里一条 {@code relation='member',
     * resource_type='org_unit', subject_type='user'} 的关系。
     *
     * <p>为 true 时,DataPermissionInterceptor 注入:
     * <pre>
     * {alias}.id IN (
     *   SELECT ar.subject_id FROM access_relations ar
     *   WHERE ar.relation='member' AND ar.resource_type='org_unit'
     *     AND ar.subject_type='user' AND ar.deleted=0 AND ar.tenant_id=?
     *     AND ar.resource_id IN (&lt;scope org ids&gt;))
     * </pre>
     * 而非常规 {@code resourceType} 路径的 {@code id IN (SELECT ar.resource_id ...)}
     * (后者用于"主表行是关系的 resource"的场景,如 student 记录)。
     *
     * <p>scope 解析(DEPARTMENT / DEPARTMENT_AND_BELOW / CUSTOM ...)沿用同一套
     * scoped-role 逻辑,只是把"组织根"换成对 member 关系 resource_id 的过滤。
     */
    boolean viaMembership() default false;
}
