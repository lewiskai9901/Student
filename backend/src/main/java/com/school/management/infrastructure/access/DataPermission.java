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

    // 注: membershipSubjectColumn 已于 Tier2 收官迁入 resource_relations 注册表
    // (owner_org SUBJECT_GRAPH 行的 subject_column, 经 ResourceRelationDef.withSubjectColumn 声明)。
    // 全部锚定语义 (org/creator/viaMembership/subject 列) 现唯一归注册表; 本注解只剩
    // module(身份) + tableAlias(查询级形态) + enabled(开关)。
}
