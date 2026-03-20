package com.school.management.domain.access.repository;

import com.school.management.domain.access.model.Role;
import com.school.management.domain.shared.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Role aggregate.
 */
public interface RoleRepository extends Repository<Role, Long> {

    /**
     * Finds a role by its code.
     */
    Optional<Role> findByRoleCode(String roleCode);

    /**
     * Finds all roles by type.
     */
    List<Role> findByRoleType(String roleType);

    /**
     * Finds all enabled roles.
     */
    List<Role> findAllEnabled();

    /**
     * Finds all system roles.
     */
    List<Role> findSystemRoles();

    /**
     * Checks if a role code already exists.
     */
    boolean existsByRoleCode(String roleCode);

    /**
     * Finds roles by IDs.
     */
    List<Role> findByIds(List<Long> ids);

    /**
     * Finds all roles with their permissions loaded.
     */
    List<Role> findAllWithPermissions();
}
