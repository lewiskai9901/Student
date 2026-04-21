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
     * 管理员视角: 可选包含 plugin_enabled=0 的 (被禁插件贡献的) 角色.
     * 权限计算链请仍用 findAllEnabled() — 必须过滤被禁插件.
     * @param includeDisabled true 返回所有 (含灰显), false 等同 findAllEnabled
     */
    List<Role> findAllForAdmin(boolean includeDisabled);

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
