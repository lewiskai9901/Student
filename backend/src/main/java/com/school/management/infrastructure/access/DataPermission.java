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
}
