package com.school.management.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个 REST endpoint 为公开端点 (无需 auth) — M3.1 (2026-05-20).
 *
 * <p>用法: 登录 / 注册 / 健康检查 / OpenAPI doc / actuator 等天生公开的端点必须显式
 * 标 @PublicEndpoint, 让 ArchUnitAllRestEndpointsProtectedTest 通过.
 *
 * <p>这是声明性安全 — 任何 REST 端点必须三选一: @PreAuthorize / @CasbinAccess / @PublicEndpoint.
 * 避免 MyClassController 同款 "忘记加权限" 长期裸奔事故.
 *
 * <p>该注解仅是文档性标记, 不主动改变 Spring Security 行为 (具体放行规则仍在 SecurityConfig).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicEndpoint {
    /** 公开理由 — 强制写, 便于 review. */
    String reason() default "";
}
