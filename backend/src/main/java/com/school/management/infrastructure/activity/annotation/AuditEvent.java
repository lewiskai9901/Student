package com.school.management.infrastructure.activity.annotation;

import java.lang.annotation.*;

/**
 * 声明式审计事件注解
 * 标注在 Controller 方法上，AOP 切面自动采集并发布 ActivityEvent
 *
 * SpEL 表达式可引用方法参数:
 *   #id, #request, #command 等
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditEvent {

    /** 模块: organization / place / access / inspection / user */
    String module();

    /** 动作: CREATE / UPDATE / DELETE / FREEZE / ASSIGN / LOGIN ... */
    String action();

    /** 资源类型: ORG_UNIT / PLACE / USER / ROLE ... */
    String resourceType();

    /** 资源ID — SpEL 表达式, 例如 "#id" 或 "#request.id" */
    String resourceId() default "";

    /** 资源名称 — SpEL 表达式（可选） */
    String resourceName() default "";

    /** 可读标签, 例如 "更新组织单元" */
    String label() default "";

    /** 是否捕获请求体 */
    boolean captureRequest() default false;

    /** 是否捕获响应体 */
    boolean captureResponse() default false;
}
