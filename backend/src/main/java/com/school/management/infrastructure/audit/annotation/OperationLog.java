package com.school.management.infrastructure.audit.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解 (DDD基础设施层)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    String module();

    String type();

    String name();

    boolean recordParams() default true;

    boolean recordResponse() default false;
}
