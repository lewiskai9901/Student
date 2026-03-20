package com.school.management.interfaces.rest.access;

import com.school.management.application.access.*;
import com.school.management.common.result.Result;
import com.school.management.domain.access.model.Permission;
import com.school.management.domain.access.model.PermissionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for permission management.
 */
@RequiredArgsConstructor
@RestController("dddPermissionController")
@RequestMapping("/permissions")
@Tag(name = "Permissions V2", description = "Permission management API (DDD)")
public class PermissionController {

    private final AccessApplicationService accessService;

    @PostMapping
    @Operation(summary = "Create a new permission")
    @CasbinAccess(resource = "system:permission", action = "add")
    public Result<PermissionResponse> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        CreatePermissionCommand command = CreatePermissionCommand.builder()
            .permissionCode(request.getPermissionCode())
            .permissionName(request.getPermissionName())
            .description(request.getDescription())
            .resource(request.getResource())
            .action(request.getAction())
            .type(request.getType())
            .parentId(request.getParentId())
            .sortOrder(request.getSortOrder())
            .build();

        Permission permission = accessService.createPermission(command);
        return Result.success(toResponse(permission));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get permission by ID")
    @CasbinAccess(resource = "system:permission", action = "view")
    public Result<PermissionResponse> getPermission(@PathVariable Long id) {
        return accessService.getPermission(id)
            .map(perm -> Result.success(toResponse(perm)))
            .orElse(Result.error("Permission not found"));
    }

    @GetMapping
    @Operation(summary = "List all permissions")
    @CasbinAccess(resource = "system:permission", action = "view")
    public Result<List<PermissionResponse>> listPermissions(
            @RequestParam(required = false) PermissionType type) {

        List<Permission> permissions;
        if (type != null) {
            permissions = accessService.listPermissionsByType(type);
        } else {
            permissions = accessService.listAllPermissions();
        }

        List<PermissionResponse> responses = permissions.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return Result.success(responses);
    }

    @GetMapping("/tree")
    @Operation(summary = "Get permission tree")
    @CasbinAccess(resource = "system:permission", action = "tree")
    public Result<List<PermissionResponse>> getPermissionTree() {
        List<Permission> roots = accessService.getPermissionTree();
        List<PermissionResponse> responses = roots.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return Result.success(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a permission")
    @CasbinAccess(resource = "system:permission", action = "edit")
    public Result<PermissionResponse> updatePermission(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePermissionRequest request) {

        UpdatePermissionCommand command = UpdatePermissionCommand.builder()
            .permissionName(request.getPermissionName())
            .description(request.getDescription())
            .build();

        Permission permission = accessService.updatePermission(id, command);
        return Result.success(toResponse(permission));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a permission")
    @CasbinAccess(resource = "system:permission", action = "delete")
    public Result<Void> deletePermission(@PathVariable Long id) {
        accessService.deletePermission(id);
        return Result.success(null);
    }

    private PermissionResponse toResponse(Permission permission) {
        PermissionResponse response = new PermissionResponse();
        response.setId(permission.getId());
        response.setPermissionCode(permission.getPermissionCode());
        response.setPermissionName(permission.getPermissionName());
        response.setDescription(permission.getDescription());
        response.setResource(permission.getResource());
        response.setAction(permission.getAction());
        response.setType(permission.getType());
        response.setParentId(permission.getParentId());
        response.setSortOrder(permission.getSortOrder());
        response.setIsEnabled(permission.getIsEnabled());
        response.setCreatedAt(permission.getCreatedAt());

        // Recursively convert children
        if (permission.getChildren() != null && !permission.getChildren().isEmpty()) {
            response.setChildren(permission.getChildren().stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
        }

        return response;
    }
}
