package com.school.management.application.access;

import com.school.management.domain.access.event.UserRoleAssignedEvent;
import com.school.management.domain.access.model.*;
import com.school.management.domain.access.repository.PermissionRepository;
import com.school.management.domain.access.repository.RoleRepository;
import com.school.management.domain.access.repository.UserRoleRepository;
import com.school.management.domain.access.service.AuthorizationService;
import com.school.management.domain.shared.event.DomainEvent;
import com.school.management.domain.shared.event.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application service for access control operations.
 */
@Service
@Transactional
public class AccessApplicationService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuthorizationService authorizationService;
    private final DomainEventPublisher eventPublisher;

    public AccessApplicationService(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            AuthorizationService authorizationService,
            DomainEventPublisher eventPublisher) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.authorizationService = authorizationService;
        this.eventPublisher = eventPublisher;
    }

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
     */
    @Transactional(readOnly = true)
    public List<Permission> getPermissionTree() {
        return permissionRepository.findRoots();
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
        if (roleRepository.existsByRoleCode(command.getRoleCode())) {
            throw new IllegalArgumentException("Role code already exists: " + command.getRoleCode());
        }

        Role role = Role.create(
            command.getRoleCode(),
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
    public List<Role> listRolesByType(RoleType roleType) {
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
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        role.setPermissions(permissionIds);
        role = roleRepository.save(role);
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
     * Assigns a role to a user.
     */
    public UserRole assignRoleToUser(Long userId, Long roleId, Long assignedBy) {
        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new IllegalArgumentException("User already has this role");
        }

        UserRole userRole = UserRole.assign(userId, roleId, assignedBy);
        userRole = userRoleRepository.save(userRole);

        // Publish event
        eventPublisher.publish(new UserRoleAssignedEvent(userId, roleId, null, assignedBy));

        // Refresh user's permission cache
        authorizationService.refreshCache(userId);

        return userRole;
    }

    /**
     * Assigns a role to a user with organization scope.
     */
    public UserRole assignRoleToUserWithScope(Long userId, Long roleId, Long orgUnitId, Long assignedBy) {
        UserRole userRole = UserRole.assignWithScope(userId, roleId, orgUnitId, assignedBy);
        userRole = userRoleRepository.save(userRole);

        eventPublisher.publish(new UserRoleAssignedEvent(userId, roleId, orgUnitId, assignedBy));
        authorizationService.refreshCache(userId);

        return userRole;
    }

    /**
     * Removes a role from a user.
     */
    public void removeRoleFromUser(Long userId, Long roleId) {
        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        authorizationService.refreshCache(userId);
    }

    /**
     * Gets all roles for a user.
     */
    @Transactional(readOnly = true)
    public List<Role> getUserRoles(Long userId) {
        List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userId);
        List<Long> roleIds = userRoles.stream()
            .map(UserRole::getRoleId)
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
     * Sets all roles for a user.
     */
    public void setUserRoles(Long userId, Set<Long> roleIds, Long assignedBy) {
        // Remove all existing roles
        userRoleRepository.deleteByUserId(userId);

        // Assign new roles
        for (Long roleId : roleIds) {
            UserRole userRole = UserRole.assign(userId, roleId, assignedBy);
            userRoleRepository.save(userRole);
        }

        authorizationService.refreshCache(userId);
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
