package com.school.management.domain.access.service;

import com.school.management.domain.access.model.DataScope;

import java.util.List;
import java.util.Set;

/**
 * Domain service for authorization decisions.
 */
public interface AuthorizationService {

    /**
     * Checks if a user has a specific permission.
     *
     * @param userId       the user ID
     * @param permission   the permission code (e.g., "user:create")
     * @return true if the user has the permission
     */
    boolean hasPermission(Long userId, String permission);

    /**
     * Checks if a user has any of the specified permissions.
     *
     * @param userId       the user ID
     * @param permissions  the permission codes
     * @return true if the user has any of the permissions
     */
    boolean hasAnyPermission(Long userId, String... permissions);

    /**
     * Checks if a user has all of the specified permissions.
     *
     * @param userId       the user ID
     * @param permissions  the permission codes
     * @return true if the user has all of the permissions
     */
    boolean hasAllPermissions(Long userId, String... permissions);

    /**
     * Checks if a user has a specific role.
     *
     * @param userId   the user ID
     * @param roleCode the role code
     * @return true if the user has the role
     */
    boolean hasRole(Long userId, String roleCode);

    /**
     * Checks if a user has any of the specified roles.
     *
     * @param userId    the user ID
     * @param roleCodes the role codes
     * @return true if the user has any of the roles
     */
    boolean hasAnyRole(Long userId, String... roleCodes);

    /**
     * Gets all permissions for a user.
     *
     * @param userId the user ID
     * @return set of permission codes
     */
    Set<String> getPermissions(Long userId);

    /**
     * Gets all roles for a user.
     *
     * @param userId the user ID
     * @return set of role codes
     */
    Set<String> getRoles(Long userId);

    /**
     * Gets the data scope for a user.
     *
     * @param userId the user ID
     * @return the effective data scope
     */
    DataScope getDataScope(Long userId);

    /**
     * Gets the organization unit IDs that a user can access.
     *
     * @param userId the user ID
     * @return list of accessible org unit IDs
     */
    List<Long> getAccessibleOrgUnitIds(Long userId);

    /**
     * Checks if a user can access data for a specific organization unit.
     *
     * @param userId    the user ID
     * @param orgUnitId the organization unit ID
     * @return true if the user can access the data
     */
    boolean canAccessOrgUnit(Long userId, Long orgUnitId);

    /**
     * Refreshes the permission cache for a user.
     *
     * @param userId the user ID
     */
    void refreshCache(Long userId);

    /**
     * Refreshes the entire permission cache.
     */
    void refreshAllCache();
}
