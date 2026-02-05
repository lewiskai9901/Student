package com.school.management.infrastructure.access;

import java.lang.annotation.*;

/**
 * 数据权限注解 (V5)
 * 标注在Mapper方法或Service方法上，声明该方法需要数据权限过滤
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 数据模块代码
     * 对应 DataModule 枚举的code值
     */
    String module();

    /**
     * 主表别名（用于SQL拼接）
     * 默认为空，使用主表名作为别名
     */
    String tableAlias() default "";

    /**
     * 是否启用数据权限过滤
     * 默认启用
     */
    boolean enabled() default true;

    /**
     * 组织单元字段名
     * 用于过滤所属组织
     */
    String orgUnitField() default "org_unit_id";

    /**
     * 班级字段名
     * 用于过滤所属班级
     */
    String classField() default "class_id";

    /**
     * 创建者字段名
     * 用于SELF范围过滤
     */
    String creatorField() default "created_by";

    /**
     * 是否忽略超级管理员
     * 默认true，超级管理员不受数据权限限制
     */
    boolean ignoreSuperAdmin() default true;
}
