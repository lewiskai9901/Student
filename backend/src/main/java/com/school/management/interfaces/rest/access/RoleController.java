package com.school.management.interfaces.rest.access;

import com.school.management.application.access.*;
import com.school.management.common.result.Result;
import com.school.management.domain.access.model.Role;
import com.school.management.domain.access.model.RoleType;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller for role management.
 */
@RestController("dddRoleController")
@RequestMapping("/roles")
@Tag(name = "Roles V2", description = "Role management API (DDD)")
public class RoleController {

    private final AccessApplicationService accessService;
    private final JwtTokenService jwtTokenService;

    public RoleController(AccessApplicationService accessService, JwtTokenService jwtTokenService) {
        this.accessService = accessService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping
    @Operation(summary = "Create a new role")
    @PreAuthorize("hasAuthority('system:role:add')")
    public Result<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        CreateRoleCommand command = CreateRoleCommand.builder()
            .roleCode(request.getRoleCode())
            .roleName(request.getRoleName())
            .description(request.getDescription())
            .roleType(request.getRoleType())
            .dataScope(request.getDataScope())
            .createdBy(getCurrentUserId())
            .build();

        Role role = accessService.createRole(command);
        return Result.success(toResponse(role));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<RoleResponse> getRole(@PathVariable Long id) {
        return accessService.getRole(id)
            .map(role -> Result.success(toResponse(role)))
            .orElse(Result.error("Role not found"));
    }

    @GetMapping
    @Operation(summary = "List all roles")
    @PreAuthorize("hasAuthority('system:role:view')")
    public Result<List<RoleResponse>> listRoles(
            @RequestParam(required = false) RoleType roleType) {

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
    @PreAuthorize("hasAuthority('system:role:edit')")
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
    @PreAuthorize("hasAuthority('system:role:delete')")
    public Result<Void> deleteRole(@PathVariable Long id) {
        accessService.deleteRole(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/permissions")
    @Operation(summary = "Set role permissions")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<RoleResponse> setRolePermissions(
            @PathVariable Long id,
            @Valid @RequestBody SetPermissionsRequest request) {

        Role role = accessService.grantPermissions(id, request.getPermissionIds());
        return Result.success(toResponse(role));
    }

    @PostMapping("/{id}/permissions/{permissionId}")
    @Operation(summary = "Add permission to role")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<RoleResponse> addPermission(
            @PathVariable Long id,
            @PathVariable Long permissionId) {

        Role role = accessService.addPermissionToRole(id, permissionId);
        return Result.success(toResponse(role));
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @Operation(summary = "Remove permission from role")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<RoleResponse> removePermission(
            @PathVariable Long id,
            @PathVariable Long permissionId) {

        Role role = accessService.removePermissionFromRole(id, permissionId);
        return Result.success(toResponse(role));
    }

    private Long getCurrentUserId() {
        return jwtTokenService.getCurrentUserId();
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
