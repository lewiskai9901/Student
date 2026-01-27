package com.school.management.domain.access.model;

import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.event.RoleCreatedEvent;
import com.school.management.domain.access.event.RolePermissionsChangedEvent;
import com.school.management.domain.shared.AggregateRoot;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Role aggregate root representing a role with associated permissions.
 */
@Getter
public class Role extends AggregateRoot<Long> {

    private Long id;
    private String roleCode;
    private String roleName;
    private String description;
    private RoleType roleType;
    private Integer level;
    private Boolean isSystem;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;

    private Set<Long> permissionIds;
    private DataScope dataScope;

    protected Role() {
        this.permissionIds = new HashSet<>();
    }

    @Builder
    public Role(Long id, String roleCode, String roleName, String description,
                RoleType roleType, Integer level, Boolean isSystem, Boolean isEnabled,
                Long createdBy, Set<Long> permissionIds, DataScope dataScope) {
        this.id = id;
        this.roleCode = Objects.requireNonNull(roleCode, "Role code is required");
        this.roleName = Objects.requireNonNull(roleName, "Role name is required");
        this.description = description;
        this.roleType = roleType != null ? roleType : RoleType.CUSTOM;
        this.level = level != null ? level : 100;
        this.isSystem = isSystem != null ? isSystem : false;
        this.isEnabled = isEnabled != null ? isEnabled : true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = createdBy;
        this.permissionIds = permissionIds != null ? new HashSet<>(permissionIds) : new HashSet<>();
        this.dataScope = dataScope != null ? dataScope : DataScope.SELF;

        validate();
    }

    /**
     * Factory method to create a new role.
     */
    public static Role create(String roleCode, String roleName, String description,
                              RoleType roleType, Long createdBy) {
        Role role = Role.builder()
            .roleCode(roleCode)
            .roleName(roleName)
            .description(description)
            .roleType(roleType)
            .createdBy(createdBy)
            .build();

        role.registerEvent(new RoleCreatedEvent(role));
        return role;
    }

    /**
     * Grants a permission to this role.
     */
    public void grantPermission(Long permissionId) {
        if (this.isSystem) {
            throw new IllegalStateException("Cannot modify system role permissions");
        }
        Set<Long> oldPermissions = new HashSet<>(this.permissionIds);
        this.permissionIds.add(permissionId);
        this.updatedAt = LocalDateTime.now();

        if (!oldPermissions.equals(this.permissionIds)) {
            registerEvent(new RolePermissionsChangedEvent(this, oldPermissions, this.permissionIds));
        }
    }

    /**
     * Revokes a permission from this role.
     */
    public void revokePermission(Long permissionId) {
        if (this.isSystem) {
            throw new IllegalStateException("Cannot modify system role permissions");
        }
        Set<Long> oldPermissions = new HashSet<>(this.permissionIds);
        this.permissionIds.remove(permissionId);
        this.updatedAt = LocalDateTime.now();

        if (!oldPermissions.equals(this.permissionIds)) {
            registerEvent(new RolePermissionsChangedEvent(this, oldPermissions, this.permissionIds));
        }
    }

    /**
     * Sets all permissions for this role.
     */
    public void setPermissions(Set<Long> newPermissionIds) {
        if (this.isSystem) {
            throw new IllegalStateException("Cannot modify system role permissions");
        }
        Set<Long> oldPermissions = new HashSet<>(this.permissionIds);
        this.permissionIds = new HashSet<>(newPermissionIds);
        this.updatedAt = LocalDateTime.now();

        if (!oldPermissions.equals(this.permissionIds)) {
            registerEvent(new RolePermissionsChangedEvent(this, oldPermissions, this.permissionIds));
        }
    }

    /**
     * Checks if this role has a specific permission.
     */
    public boolean hasPermission(Long permissionId) {
        return this.permissionIds.contains(permissionId);
    }

    /**
     * Sets the data scope for this role.
     */
    public void setDataScope(DataScope dataScope) {
        this.dataScope = dataScope;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Updates role information.
     */
    public void updateInfo(String roleName, String description) {
        if (roleName != null && !roleName.isBlank()) {
            this.roleName = roleName;
        }
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void enable() {
        this.isEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        if (this.isSystem) {
            throw new IllegalStateException("Cannot disable system role");
        }
        this.isEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if this role outranks another role.
     */
    public boolean outranks(Role other) {
        return this.level < other.level;
    }

    private void validate() {
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("Role code cannot be empty");
        }
        if (roleCode.length() > 50) {
            throw new IllegalArgumentException("Role code cannot exceed 50 characters");
        }
        if (roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("Role name cannot be empty");
        }
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<Long> getPermissionIds() {
        return Collections.unmodifiableSet(permissionIds);
    }
}
