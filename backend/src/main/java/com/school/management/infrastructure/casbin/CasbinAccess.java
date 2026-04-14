package com.school.management.infrastructure.casbin;

import com.school.management.domain.access.model.PermissionScope;

import java.lang.annotation.*;

/**
 * Casbin access control annotation - replaces @PreAuthorize
 * Usage: @CasbinAccess(resource="student", action="view")
 *
 * <p>Scope semantics (see docs/security/access-control-guide.md):
 * <ul>
 *   <li>PUBLIC — any authenticated user passes; Casbin enforce is skipped</li>
 *   <li>SELF — Casbin enforces role grant; ownership check is the caller's responsibility
 *       (typically via V5 SELF scope at the mapper level)</li>
 *   <li>MANAGEMENT (default) — Casbin enforces role grant normally</li>
 * </ul>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CasbinAccess {
    String resource();
    String action();
    PermissionScope scope() default PermissionScope.MANAGEMENT;
}
