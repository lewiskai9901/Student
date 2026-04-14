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
 *   <li>SELF — Casbin enforce is skipped. Data isolation (user_id = currentUserId)
 *       is the endpoint's responsibility, enforced via
 *       {@code SecurityUtils.requireCurrentUserId()} and guarded at architecture
 *       level by {@code ArchUnitMyEndpointTest} (forbids identity params on /my/*)</li>
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
