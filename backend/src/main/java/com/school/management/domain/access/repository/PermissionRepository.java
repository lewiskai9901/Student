package com.school.management.domain.access.repository;

import com.school.management.domain.access.model.Permission;
import com.school.management.domain.access.model.PermissionType;
import com.school.management.domain.shared.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Permission aggregate.
 */
public interface PermissionRepository extends Repository<Permission, Long> {

    /**
     * Finds a permission by its code.
     */
    Optional<Permission> findByPermissionCode(String permissionCode);

    /**
     * Finds all permissions by type.
     */
    List<Permission> findByType(PermissionType type);

    /**
     * Finds all enabled permissions.
     */
    List<Permission> findAllEnabled();

    /**
     * Finds all child permissions of a parent.
     */
    List<Permission> findByParentId(Long parentId);

    /**
     * Finds all root permissions (no parent).
     */
    List<Permission> findRoots();

    /**
     * Finds permissions by resource.
     */
    List<Permission> findByResource(String resource);

    /**
     * Checks if a permission code already exists.
     */
    boolean existsByPermissionCode(String permissionCode);

    /**
     * Finds permissions by IDs.
     */
    List<Permission> findByIds(List<Long> ids);
}
