package com.school.management.interfaces.rest.access;

import com.school.management.application.access.AccessApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.access.model.Role;
import com.school.management.domain.access.model.UserRole;
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
 * REST controller for user role assignment.
 */
@RestController("userRoleControllerV2")
@RequestMapping("/v2/users")
@Tag(name = "User Roles", description = "User role assignment API")
public class UserRoleController {

    private final AccessApplicationService accessService;
    private final JwtTokenService jwtTokenService;

    public UserRoleController(AccessApplicationService accessService, JwtTokenService jwtTokenService) {
        this.accessService = accessService;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping("/{userId}/roles")
    @Operation(summary = "Get user's roles")
    @PreAuthorize("hasAuthority('user:role:view')")
    public Result<List<RoleResponse>> getUserRoles(@PathVariable Long userId) {
        List<Role> roles = accessService.getUserRoles(userId);
        List<RoleResponse> responses = roles.stream()
            .map(this::toRoleResponse)
            .collect(Collectors.toList());
        return Result.success(responses);
    }

    @GetMapping("/{userId}/permissions")
    @Operation(summary = "Get user's permissions")
    @PreAuthorize("hasAuthority('user:permission:view')")
    public Result<Set<String>> getUserPermissions(@PathVariable Long userId) {
        Set<String> permissions = accessService.getUserPermissions(userId);
        return Result.success(permissions);
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "Assign role to user")
    @PreAuthorize("hasAuthority('user:role:assign')")
    public Result<UserRoleResponse> assignRole(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        UserRole userRole = accessService.assignRoleToUser(userId, roleId, getCurrentUserId());
        return Result.success(toUserRoleResponse(userRole));
    }

    @PostMapping("/{userId}/roles/{roleId}/scoped")
    @Operation(summary = "Assign role to user with organization scope")
    @PreAuthorize("hasAuthority('user:role:assign')")
    public Result<UserRoleResponse> assignRoleWithScope(
            @PathVariable Long userId,
            @PathVariable Long roleId,
            @RequestBody AssignRoleWithScopeRequest request) {

        UserRole userRole = accessService.assignRoleToUserWithScope(
            userId, roleId, request.getOrgUnitId(), getCurrentUserId());
        return Result.success(toUserRoleResponse(userRole));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "Remove role from user")
    @PreAuthorize("hasAuthority('user:role:remove')")
    public Result<Void> removeRole(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        accessService.removeRoleFromUser(userId, roleId);
        return Result.success(null);
    }

    @PutMapping("/{userId}/roles")
    @Operation(summary = "Set user's roles")
    @PreAuthorize("hasAuthority('user:role:assign')")
    public Result<Void> setUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody SetUserRolesRequest request) {

        accessService.setUserRoles(userId, request.getRoleIds(), getCurrentUserId());
        return Result.success(null);
    }

    @GetMapping("/me/permissions")
    @Operation(summary = "Get current user's permissions")
    public Result<Set<String>> getMyPermissions() {
        Long userId = getCurrentUserId();
        Set<String> permissions = accessService.getUserPermissions(userId);
        return Result.success(permissions);
    }

    @GetMapping("/me/roles")
    @Operation(summary = "Get current user's roles")
    public Result<List<RoleResponse>> getMyRoles() {
        Long userId = getCurrentUserId();
        List<Role> roles = accessService.getUserRoles(userId);
        List<RoleResponse> responses = roles.stream()
            .map(this::toRoleResponse)
            .collect(Collectors.toList());
        return Result.success(responses);
    }

    private Long getCurrentUserId() {
        return jwtTokenService.getCurrentUserId();
    }

    private RoleResponse toRoleResponse(Role role) {
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

    private UserRoleResponse toUserRoleResponse(UserRole userRole) {
        UserRoleResponse response = new UserRoleResponse();
        response.setId(userRole.getId());
        response.setUserId(userRole.getUserId());
        response.setRoleId(userRole.getRoleId());
        response.setOrgUnitId(userRole.getOrgUnitId());
        response.setAssignedAt(userRole.getAssignedAt());
        response.setExpiresAt(userRole.getExpiresAt());
        response.setIsActive(userRole.getIsActive());
        return response;
    }
}
