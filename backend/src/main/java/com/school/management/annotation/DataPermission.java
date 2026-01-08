package com.school.management.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * 标注在Service方法上，AOP会自动根据当前用户权限过滤查询条件
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 模块编码: student, class, dormitory, check_record, appeal, evaluation
     */
    String module();

    /**
     * 班级字段名(用于设置classIds过滤)
     */
    String classField() default "classIds";

    /**
     * 部门字段名(用于设置departmentIds过滤)
     */
    String deptField() default "departmentIds";

    /**
     * 是否检查单条数据访问权限
     */
    boolean checkSingleAccess() default false;
}
