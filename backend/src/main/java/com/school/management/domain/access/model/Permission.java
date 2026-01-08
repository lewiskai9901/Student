package com.school.management.domain.access.model;

import com.school.management.domain.shared.Entity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Permission entity representing a single permission in the system.
 * Permissions follow the pattern: resource:action (e.g., "user:create", "inspection:view")
 */
@Getter
public class Permission extends Entity<Long> {

    private Long id;
    private String permissionCode;
    private String permissionName;
    private String description;
    private String resource;
    private String action;
    private PermissionType type;
    private Long parentId;
    private Integer sortOrder;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional fields for menu/routing support
    private String path;
    private String component;
    private String icon;

    protected Permission() {}

    @Builder
    public Permission(Long id, String permissionCode, String permissionName,
                      String description, String resource, String action,
                      PermissionType type, Long parentId, Integer sortOrder,
                      Boolean isEnabled, String path, String component, String icon) {
        this.id = id;
        this.permissionCode = Objects.requireNonNull(permissionCode, "Permission code is required");
        this.permissionName = Objects.requireNonNull(permissionName, "Permission name is required");
        this.description = description;
        this.resource = resource;
        this.action = action;
        this.type = type != null ? type : PermissionType.OPERATION;
        this.parentId = parentId;
        this.sortOrder = sortOrder != null ? sortOrder : 0;
        this.isEnabled = isEnabled != null ? isEnabled : true;
        this.path = path;
        this.component = component;
        this.icon = icon;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;

        validate();
    }

    /**
     * Creates a new permission with resource:action pattern.
     */
    public static Permission create(String resource, String action, String name, String description) {
        String code = resource + ":" + action;
        return Permission.builder()
            .permissionCode(code)
            .permissionName(name)
            .description(description)
            .resource(resource)
            .action(action)
            .type(PermissionType.OPERATION)
            .build();
    }

    /**
     * Creates a menu permission.
     */
    public static Permission createMenu(String code, String name, Long parentId, Integer sortOrder) {
        return Permission.builder()
            .permissionCode(code)
            .permissionName(name)
            .type(PermissionType.MENU)
            .parentId(parentId)
            .sortOrder(sortOrder)
            .build();
    }

    public void enable() {
        this.isEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        this.isEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateInfo(String name, String description) {
        if (name != null && !name.isBlank()) {
            this.permissionName = name;
        }
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Checks if this permission matches a resource and action.
     */
    public boolean matches(String resource, String action) {
        if (this.resource == null || this.action == null) {
            return false;
        }
        // Support wildcard matching
        boolean resourceMatch = "*".equals(this.resource) || this.resource.equals(resource);
        boolean actionMatch = "*".equals(this.action) || this.action.equals(action);
        return resourceMatch && actionMatch;
    }

    /**
     * Gets the full permission string for Casbin.
     */
    public String toCasbinPermission() {
        return permissionCode;
    }

    private void validate() {
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new IllegalArgumentException("Permission code cannot be empty");
        }
        if (permissionCode.length() > 100) {
            throw new IllegalArgumentException("Permission code cannot exceed 100 characters");
        }
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
