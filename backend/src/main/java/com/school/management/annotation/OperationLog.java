package com.school.management.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标注在Controller方法上,用于自动记录操作日志
 *
 * @author system
 * @since 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作模块
     */
    String module();

    /**
     * 操作类型
     */
    String type();

    /**
     * 操作名称
     */
    String name();

    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;

    /**
     * 是否记录响应结果
     */
    boolean recordResponse() default false;
}
