package com.school.management.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * 标记在Service方法上，自动进行数据范围过滤
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    /**
     * 部门表别名
     */
    String deptAlias() default "";

    /**
     * 班级表别名
     */
    String classAlias() default "";

    /**
     * 用户表别名
     */
    String userAlias() default "";

    /**
     * 模块编码,用于查询角色的数据权限配置
     * 可选值: student, class, dormitory, check_record, task
     */
    String module() default "student";
}
