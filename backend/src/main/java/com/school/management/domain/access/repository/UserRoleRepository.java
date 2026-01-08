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
     * Finds role assignments for a user within an organization unit.
     */
    List<UserRole> findByUserIdAndOrgUnitId(Long userId, Long orgUnitId);

    /**
     * Checks if a user has a specific role.
     */
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    /**
     * Removes all role assignments for a user.
     */
    void deleteByUserId(Long userId);

    /**
     * Removes a specific role assignment.
     */
    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}
