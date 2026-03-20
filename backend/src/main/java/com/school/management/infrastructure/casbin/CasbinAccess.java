package com.school.management.infrastructure.casbin;

import java.lang.annotation.*;

/**
 * Casbin access control annotation - replaces @PreAuthorize
 * Usage: @CasbinAccess(resource="student", action="view")
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CasbinAccess {
    String resource();
    String action();
}
