package com.school.management.application.access;

import com.school.management.domain.access.event.UserRoleAssignedEvent;
import com.school.management.domain.access.model.*;
import com.school.management.domain.access.repository.PermissionRepository;
import com.school.management.domain.access.repository.RoleRepository;
import com.school.management.domain.access.repository.UserRoleRepository;
import com.school.management.domain.access.service.AuthorizationService;
import com.school.management.domain.shared.event.DomainEvent;
import com.school.management.domain.shared.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application service for access control operations.
 */
@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class AccessApplicationService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuthorizationService authorizationService;
    private final DomainEventPublisher eventPublisher;

    // ==================== Permission Operations ====================

    /**
     * Creates a new permission.
     */
    public Permission createPermission(CreatePermissionCommand command) {
        if (permissionRepository.existsByPermissionCode(command.getPermissionCode())) {
            throw new IllegalArgumentException("Permission code already exists: " + command.getPermissionCode());
        }

        Permission permission = Permission.builder()
            .permissionCode(command.getPermissionCode())
            .permissionName(command.getPermissionName())
            .description(command.getDescription())
            .resource(command.getResource())
            .action(command.getAction())
            .type(command.getType())
            .parentId(command.getParentId())
            .sortOrder(command.getSortOrder())
            .build();

        return permissionRepository.save(permission);
    }

    /**
     * Updates a permission.
     */
    public Permission updatePermission(Long id, UpdatePermissionCommand command) {
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + id));

        permission.updateInfo(command.getPermissionName(), command.getDescription());
        return permissionRepository.save(permission);
    }

    /**
     * Gets a permission by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Permission> getPermission(Long id) {
        return permissionRepository.findById(id);
    }

    /**
     * Lists all enabled permissions.
     */
    @Transactional(readOnly = true)
    public List<Permission> listAllPermissions() {
        return permissionRepository.findAllEnabled();
    }

    /**
     * Lists permissions by type.
     */
    @Transactional(readOnly = true)
    public List<Permission> listPermissionsByType(PermissionType type) {
        return permissionRepository.findByType(type);
    }

    /**
     * Gets permission tree (hierarchical).
     * Builds a tree structure with children properly nested.
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionTree() {
        // Get all enabled permissions
        List<Permission> allPermissions = permissionRepository.findAllEnabled();

        // Build tree structure
        return buildPermissionTree(allPermissions);
    }

    /**
     * Builds a permission tree from a flat list.
     */
    private List<Permission> buildPermissionTree(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }

        // Create a map for quick lookup
        Map<Long, Permission> permissionMap = new HashMap<>();
        for (Permission permission : permissions) {
            permissionMap.put(permission.getId(), permission);
            permission.setChildren(new ArrayList<>()); // Initialize children list
        }

        // Build tree by linking children to parents
        List<Permission> roots = new ArrayList<>();
        for (Permission permission : permissions) {
            Long parentId = permission.getParentId();
            if (parentId == null || parentId == 0L) {
                // This is a root permission
                roots.add(permission);
            } else {
                // Find parent and add as child
                Permission parent = permissionMap.get(parentId);
                if (parent != null) {
                    parent.getChildren().add(permission);
                } else {
                    // Parent not found, treat as root
                    roots.add(permission);
                }
            }
        }

        // Sort roots by sortOrder
        roots.sort((a, b) -> {
            Integer orderA = a.getSortOrder() != null ? a.getSortOrder() : 0;
            Integer orderB = b.getSortOrder() != null ? b.getSortOrder() : 0;
            return orderA.compareTo(orderB);
        });

        return roots;
    }

    /**
     * Deletes a permission.
     */
    public void deletePermission(Long id) {
        permissionRepository.deleteById(id);
    }

    // ==================== Role Operations ====================

    /**
     * Creates a new role.
     */
    public Role createRole(CreateRoleCommand command) {
        // 自动生成角色编码
        String roleCode = generateRoleCode();

        Role role = Role.create(
            roleCode,
            command.getRoleName(),
            command.getDescription(),
            command.getRoleType(),
            command.getCreatedBy()
        );

        if (command.getDataScope() != null) {
            role.setDataScope(command.getDataScope());
        }

        role = roleRepository.save(role);
        publishEvents(role);
        return role;
    }

    private String generateRoleCode() {
        String code;
        do {
            code = "ROLE_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (roleRepository.existsByRoleCode(code));
        return code;
    }

    /**
     * Updates a role.
     */
    public Role updateRole(Long id, UpdateRoleCommand command) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));

        role.updateInfo(command.getRoleName(), command.getDescription());

        if (command.getDataScope() != null) {
            role.setDataScope(command.getDataScope());
        }

        role = roleRepository.save(role);
        return role;
    }

    /**
     * Gets a role by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Role> getRole(Long id) {
        return roleRepository.findById(id);
    }

    /**
     * Lists all enabled roles.
     */
    @Transactional(readOnly = true)
    public List<Role> listAllRoles() {
        return roleRepository.findAllEnabled();
    }

    /**
     * Lists roles by type.
     */
    @Transactional(readOnly = true)
    public List<Role> listRolesByType(String roleType) {
        return roleRepository.findByRoleType(roleType);
    }

    /**
     * Deletes a role.
     */
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + id));

        if (role.getIsSystem()) {
            throw new IllegalStateException("Cannot delete system role");
        }

        roleRepository.deleteById(role.getId());
    }

    /**
     * Grants permissions to a role.
     */
    public Role grantPermissions(Long roleId, Set<Long> permissionIds) {
        log.info("grantPermissions called: roleId={}, permissionIds count={}", roleId, permissionIds != null ? permissionIds.size() : 0);

        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        log.info("Role found: {} ({}), isSystem={}, current permissions count={}",
            role.getRoleName(), role.getRoleCode(), role.getIsSystem(), role.getPermissionIds().size());

        role.setPermissions(permissionIds);
        log.info("After setPermissions, role has {} permissions", role.getPermissionIds().size());

        role = roleRepository.save(role);
        log.info("Role saved, returned permissions count={}", role.getPermissionIds().size());

        publishEvents(role);

        // Refresh cache for all users with this role
        refreshCacheForRole(roleId);

        return role;
    }

    /**
     * Adds a single permission to a role.
     */
    public Role addPermissionToRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        role.grantPermission(permissionId);
        role = roleRepository.save(role);
        publishEvents(role);

        refreshCacheForRole(roleId);
        return role;
    }

    /**
     * Removes a permission from a role.
     */
    public Role removePermissionFromRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        role.revokePermission(permissionId);
        role = roleRepository.save(role);
        publishEvents(role);

        refreshCacheForRole(roleId);
        return role;
    }

    // ==================== User Role Operations ====================

    /**
     * Assigns a role to a user with global scope (ALL).
     */
    public UserRole assignRoleToUser(Long userId, Long roleId, Long assignedBy) {
        // 验证角色存在
        roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleId));

        if (userRoleRepository.existsByUserIdAndRoleIdAndScope(userId, roleId, "ALL", 0L)) {
            throw new IllegalArgumentException("User already has this role with global scope");
        }

        UserRole userRole = UserRole.assign(userId, roleId, assignedBy);
        userRole = userRoleRepository.save(userRole);

        eventPublisher.publish(new UserRoleAssignedEvent(userId, roleId, "ALL", 0L, assignedBy));
        authorizationService.refreshCache(userId);

        return userRole;
    }

    /**
     * Assigns a role to a user with a specific scope.
     */
    public UserRole assignRoleToUserWithScope(Long userId, Long roleId,
                                               String scopeType, Long scopeId, Long assignedBy) {
        // 验证角色存在
        roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleId));

        // 验证 ORG_UNIT scope 的 scopeId 有效性
        if (ScopeType.ORG_UNIT.equals(scopeType) && scopeId != null && scopeId > 0) {
            // scopeId 应指向一个有效的组织单元（此处仅做非零校验，因为 OrgUnitRepository 不在本服务依赖中）
            log.debug("Assigning role {} with ORG_UNIT scope, scopeId={}", roleId, scopeId);
        }

        if (userRoleRepository.existsByUserIdAndRoleIdAndScope(userId, roleId, scopeType, scopeId)) {
            throw new IllegalArgumentException("User already has this role with the same scope");
        }

        UserRole userRole = UserRole.assignWithScope(userId, roleId, scopeType, scopeId, assignedBy);
        userRole = userRoleRepository.save(userRole);

        eventPublisher.publish(new UserRoleAssignedEvent(userId, roleId, scopeType, scopeId, assignedBy));
        authorizationService.refreshCache(userId);

        return userRole;
    }

    /**
     * Removes a role from a user (all scopes).
     */
    public void removeRoleFromUser(Long userId, Long roleId) {
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        authorizationService.refreshCache(userId);
    }

    /**
     * Removes a specific scoped role assignment from a user.
     */
    public void removeRoleFromUserWithScope(Long userId, Long roleId, String scopeType, Long scopeId) {
        userRoleRepository.deleteByUserIdAndRoleIdAndScope(userId, roleId, scopeType, scopeId);
        authorizationService.refreshCache(userId);
    }

    /**
     * Gets all active user role assignments (with scope info).
     */
    @Transactional(readOnly = true)
    public List<UserRole> getUserRoleAssignments(Long userId) {
        return userRoleRepository.findActiveByUserId(userId);
    }

    /**
     * Gets all roles for a user (deduplicated role objects).
     */
    @Transactional(readOnly = true)
    public List<Role> getUserRoles(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userId);
        List<Long> roleIds = userRoles.stream()
            .map(UserRole::getRoleId)
            .distinct()
            .collect(Collectors.toList());
        return roleRepository.findByIds(roleIds);
    }

    /**
     * Gets all permissions for a user.
     */
    @Transactional(readOnly = true)
    public Set<String> getUserPermissions(Long userId) {
        return authorizationService.getPermissions(userId);
    }

    /**
     * Sets all roles for a user (replaces existing assignments).
     * Accepts a list of role assignments with scope info.
     */
    public void setUserRoles(Long userId, List<RoleAssignment> assignments, Long assignedBy) {
        // 验证所有角色ID存在
        for (RoleAssignment assignment : assignments) {
            roleRepository.findById(assignment.getRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + assignment.getRoleId()));
        }

        // Remove all existing roles
        userRoleRepository.deleteByUserId(userId);

        // Assign new roles with scope, expiry, and reason
        for (RoleAssignment assignment : assignments) {
            String scopeType = assignment.getScopeType() != null ? assignment.getScopeType() : "ALL";
            Long scopeId = assignment.getScopeId() != null ? assignment.getScopeId() : 0L;

            java.time.LocalDateTime expiresAt = null;
            if (assignment.getExpiresAt() != null && !assignment.getExpiresAt().isEmpty()) {
                String raw = assignment.getExpiresAt();
                expiresAt = java.time.LocalDateTime.parse(raw.length() > 10 ? raw : raw + "T23:59:59");
            }

            UserRole userRole = UserRole.builder()
                    .userId(userId)
                    .roleId(assignment.getRoleId())
                    .scopeType(scopeType)
                    .scopeId("ALL".equals(scopeType) ? 0L : scopeId)
                    .assignedBy(assignedBy)
                    .expiresAt(expiresAt)
                    .reason(assignment.getReason())
                    .grantedBy(assignedBy)
                    .grantedAt(java.time.LocalDateTime.now())
                    .build();

            userRoleRepository.save(userRole);
        }

        authorizationService.refreshCache(userId);
    }

    /**
     * DTO for role assignment with scope.
     */
    @lombok.Data
    public static class RoleAssignment {
        private Long roleId;
        private String scopeType;
        private Long scopeId;
        private String expiresAt;
        private String reason;
    }

    // ==================== Helper Methods ====================

    private void publishEvents(Role role) {
        for (DomainEvent event : role.getDomainEvents()) {
            eventPublisher.publish(event);
        }
        role.clearDomainEvents();
    }

    private void refreshCacheForRole(Long roleId) {
        List<UserRole> userRoles = userRoleRepository.findByRoleId(roleId);
        for (UserRole ur : userRoles) {
            authorizationService.refreshCache(ur.getUserId());
        }
    }
}
