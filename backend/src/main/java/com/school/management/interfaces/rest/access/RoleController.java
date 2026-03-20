package com.school.management.interfaces.rest.access;

import com.school.management.application.access.*;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.activity.annotation.AuditEvent;
import com.school.management.domain.access.model.Role;
import com.school.management.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for role management.
 */
@RequiredArgsConstructor
@RestController("dddRoleController")
@RequestMapping("/roles")
@Tag(name = "Roles V2", description = "Role management API (DDD)")
public class RoleController {

    private final AccessApplicationService accessService;

    @PostMapping
    @Operation(summary = "Create a new role")
    @CasbinAccess(resource = "system:role", action = "add")
    @AuditEvent(module = "access", action = "CREATE", resourceType = "ROLE", label = "创建角色")
    public Result<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        CreateRoleCommand command = CreateRoleCommand.builder()
            .roleName(request.getRoleName())
            .description(request.getDescription())
            .roleType(request.getRoleType())
            .dataScope(request.getDataScope())
            .createdBy(SecurityUtils.requireCurrentUserId())
            .build();

        Role role = accessService.createRole(command);
        return Result.success(toResponse(role));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<RoleResponse> getRole(@PathVariable Long id) {
        return accessService.getRole(id)
            .map(role -> Result.success(toResponse(role)))
            .orElse(Result.error("Role not found"));
    }

    @GetMapping
    @Operation(summary = "List all roles")
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<List<RoleResponse>> listRoles(
            @RequestParam(required = false) String roleType) {

        List<Role> roles;
        if (roleType != null) {
            roles = accessService.listRolesByType(roleType);
        } else {
            roles = accessService.listAllRoles();
        }

        List<RoleResponse> responses = roles.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return Result.success(responses);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a role")
    @CasbinAccess(resource = "system:role", action = "edit")
    @AuditEvent(module = "access", action = "UPDATE", resourceType = "ROLE", resourceId = "#id", label = "更新角色")
    public Result<RoleResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {

        UpdateRoleCommand command = UpdateRoleCommand.builder()
            .roleName(request.getRoleName())
            .description(request.getDescription())
            .dataScope(request.getDataScope())
            .build();

        Role role = accessService.updateRole(id, command);
        return Result.success(toResponse(role));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a role")
    @CasbinAccess(resource = "system:role", action = "delete")
    @AuditEvent(module = "access", action = "DELETE", resourceType = "ROLE", resourceId = "#id", label = "删除角色")
    public Result<Void> deleteRole(@PathVariable Long id) {
        accessService.deleteRole(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Set role permissions")
    @CasbinAccess(resource = "system:role", action = "edit")
    @AuditEvent(module = "access", action = "UPDATE", resourceType = "ROLE", resourceId = "#id", label = "设置角色权限")
    public Result<RoleResponse> setRolePermissions(
            @PathVariable Long id,
            @Valid @RequestBody SetPermissionsRequest request) {

        Role role = accessService.grantPermissions(id, request.getPermissionIds());
        return Result.success(toResponse(role));
    }

    @PostMapping("/{id}/permissions/{permissionId}")
    @Operation(summary = "Add permission to role")
    @CasbinAccess(resource = "system:role", action = "edit")
    @AuditEvent(module = "access", action = "UPDATE", resourceType = "ROLE", resourceId = "#id", label = "添加角色权限")
    public Result<RoleResponse> addPermission(
            @PathVariable Long id,
            @PathVariable Long permissionId) {

        Role role = accessService.addPermissionToRole(id, permissionId);
        return Result.success(toResponse(role));
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @Operation(summary = "Remove permission from role")
    @CasbinAccess(resource = "system:role", action = "edit")
    @AuditEvent(module = "access", action = "DELETE", resourceType = "ROLE", resourceId = "#id", label = "移除角色权限")
    public Result<RoleResponse> removePermission(
            @PathVariable Long id,
            @PathVariable Long permissionId) {

        Role role = accessService.removePermissionFromRole(id, permissionId);
        return Result.success(toResponse(role));
    }

    private RoleResponse toResponse(Role role) {
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setDescription(role.getDescription());
        response.setRoleType(role.getRoleType());
        response.setLevel(role.getLevel());
        response.setIsSystem(role.getIsSystem());
        response.setIsEnabled(role.getIsEnabled());
        response.setDataScope(role.getDataScope());
        response.setPermissionIds(role.getPermissionIds());
        response.setCreatedAt(role.getCreatedAt());
        return response;
    }
}
