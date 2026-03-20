package com.school.management.domain.access.repository;

import com.school.management.domain.access.model.UserRole;
import com.school.management.domain.shared.Repository;

import java.util.List;

/**
 * Repository interface for UserRole entity.
 */
public interface UserRoleRepository extends Repository<UserRole, Long> {

    /**
     * Finds all role assignments for a user.
     */
    List<UserRole> findByUserId(Long userId);

    /**
     * Finds all active role assignments for a user.
     */
    List<UserRole> findActiveByUserId(Long userId);

    /**
     * Finds all users with a specific role.
     */
    List<UserRole> findByRoleId(Long roleId);

    /**
     * Finds role assignments for a user with a specific scope.
     */
    List<UserRole> findByUserIdAndScope(Long userId, String scopeType, Long scopeId);

    /**
     * Checks if a user has a specific role (any scope).
     */
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * Checks if a user has a specific role with exact scope.
     */
    boolean existsByUserIdAndRoleIdAndScope(Long userId, Long roleId, String scopeType, Long scopeId);

    /**
     * Removes all role assignments for a user.
     */
    void deleteByUserId(Long userId);

    /**
     * Removes all assignments of a specific role from a user (all scopes).
     */
    void deleteByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * Removes a specific role assignment with exact scope.
     */
    void deleteByUserIdAndRoleIdAndScope(Long userId, Long roleId, String scopeType, Long scopeId);
}
