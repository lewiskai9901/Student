package com.school.management.infrastructure.access;

import com.school.management.domain.access.service.AuthorizationService;
import com.school.management.security.JwtTokenService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Utility component for checking permissions in controllers and services.
 */
@Component
public class PermissionChecker {

    private final AuthorizationService authorizationService;
    private final JwtTokenService jwtTokenService;

    public PermissionChecker(AuthorizationService authorizationService,
                             JwtTokenService jwtTokenService) {
        this.authorizationService = authorizationService;
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * Checks if the current user has the specified permission.
     *
     * @param permission the permission code
     * @throws AccessDeniedException if the user doesn't have the permission
     */
    public void checkPermission(String permission) {
        Long userId = jwtTokenService.getCurrentUserId();
        if (!authorizationService.hasPermission(userId, permission)) {
            throw new AccessDeniedException("Access denied: required permission " + permission);
        }
    }

    /**
     * Checks if the current user has any of the specified permissions.
     *
     * @param permissions the permission codes
     * @throws AccessDeniedException if the user doesn't have any of the permissions
     */
    public void checkAnyPermission(String... permissions) {
        Long userId = jwtTokenService.getCurrentUserId();
        if (!authorizationService.hasAnyPermission(userId, permissions)) {
            throw new AccessDeniedException("Access denied: required one of permissions " +
                String.join(", ", permissions));
        }
    }

    /**
     * Checks if the current user has all of the specified permissions.
     *
     * @param permissions the permission codes
     * @throws AccessDeniedException if the user doesn't have all the permissions
     */
    public void checkAllPermissions(String... permissions) {
        Long userId = jwtTokenService.getCurrentUserId();
        if (!authorizationService.hasAllPermissions(userId, permissions)) {
            throw new AccessDeniedException("Access denied: required all permissions " +
                String.join(", ", permissions));
        }
    }

    /**
     * Checks if the current user has the specified role.
     *
     * @param roleCode the role code
     * @throws AccessDeniedException if the user doesn't have the role
     */
    public void checkRole(String roleCode) {
        Long userId = jwtTokenService.getCurrentUserId();
        if (!authorizationService.hasRole(userId, roleCode)) {
            throw new AccessDeniedException("Access denied: required role " + roleCode);
        }
    }

    /**
     * Checks if the current user can access data for the specified organization unit.
     *
     * @param orgUnitId the organization unit ID
     * @throws AccessDeniedException if the user cannot access the data
     */
    public void checkOrgUnitAccess(Long orgUnitId) {
        Long userId = jwtTokenService.getCurrentUserId();
        if (!authorizationService.canAccessOrgUnit(userId, orgUnitId)) {
            throw new AccessDeniedException("Access denied: cannot access organization unit " + orgUnitId);
        }
    }

    /**
     * Returns true if the current user has the specified permission.
     */
    public boolean hasPermission(String permission) {
        Long userId = jwtTokenService.getCurrentUserId();
        return authorizationService.hasPermission(userId, permission);
    }

    /**
     * Returns true if the current user has the specified role.
     */
    public boolean hasRole(String roleCode) {
        Long userId = jwtTokenService.getCurrentUserId();
        return authorizationService.hasRole(userId, roleCode);
    }
}
