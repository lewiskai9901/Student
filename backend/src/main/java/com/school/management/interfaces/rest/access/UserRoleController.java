package com.school.management.interfaces.rest.access;

import com.school.management.application.access.AccessApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.access.model.Role;
import com.school.management.domain.access.model.ScopeType;
import com.school.management.domain.access.model.UserRole;
import com.school.management.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for user role assignment with scope support.
 */
@RequiredArgsConstructor
@RestController("userRoleController")
@RequestMapping("/users")
@Tag(name = "User Roles", description = "User role assignment API")
public class UserRoleController {

    private final AccessApplicationService accessService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/{userId}/roles")
    @Operation(summary = "Get user's role assignments with scope info")
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<List<UserRoleResponse>> getUserRoles(@PathVariable Long userId) {
        List<UserRole> assignments = accessService.getUserRoleAssignments(userId);

        // Batch load role info
        Set<Long> roleIds = assignments.stream().map(UserRole::getRoleId).collect(Collectors.toSet());
        Map<Long, Role> roleMap = new HashMap<>();
        if (!roleIds.isEmpty()) {
            List<Role> roles = accessService.getUserRoles(userId);
            for (Role r : roles) {
                roleMap.put(r.getId(), r);
            }
        }

        List<UserRoleResponse> responses = assignments.stream()
            .map(ur -> toUserRoleResponse(ur, roleMap))
            .collect(Collectors.toList());
        return Result.success(responses);
    }

    @GetMapping("/{userId}/permissions")
    @Operation(summary = "Get user's permissions")
    @CasbinAccess(resource = "system:permission", action = "view")
    public Result<Set<String>> getUserPermissions(@PathVariable Long userId) {
        Set<String> permissions = accessService.getUserPermissions(userId);
        return Result.success(permissions);
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "Assign role to user (global scope)")
    @CasbinAccess(resource = "system:role", action = "edit")
    public Result<UserRoleResponse> assignRole(
            @PathVariable Long userId,
            @PathVariable Long roleId) {

        UserRole userRole = accessService.assignRoleToUser(userId, roleId, SecurityUtils.requireCurrentUserId());
        return Result.success(toUserRoleResponse(userRole, Collections.emptyMap()));
    }

    @PostMapping("/{userId}/roles/{roleId}/scoped")
    @Operation(summary = "Assign role to user with scope")
    @CasbinAccess(resource = "system:role", action = "edit")
    public Result<UserRoleResponse> assignRoleWithScope(
            @PathVariable Long userId,
            @PathVariable Long roleId,
            @RequestBody AssignRoleWithScopeRequest request) {

        String scopeType = request.getScopeType() != null ? request.getScopeType() : ScopeType.ALL;
        Long scopeId = request.getScopeId() != null ? request.getScopeId() : 0L;

        UserRole userRole = accessService.assignRoleToUserWithScope(
            userId, roleId, scopeType, scopeId, SecurityUtils.requireCurrentUserId());
        return Result.success(toUserRoleResponse(userRole, Collections.emptyMap()));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "Remove role from user (all scopes or specific scope)")
    @CasbinAccess(resource = "system:role", action = "edit")
    public Result<Void> removeRole(
            @PathVariable Long userId,
            @PathVariable Long roleId,
            @RequestParam(required = false) String scopeType,
            @RequestParam(required = false) Long scopeId) {

        if (scopeType != null && scopeId != null) {
            accessService.removeRoleFromUserWithScope(userId, roleId, scopeType, scopeId);
        } else {
            accessService.removeRoleFromUser(userId, roleId);
        }
        return Result.success(null);
    }

    @PutMapping("/{userId}/roles")
    @Operation(summary = "Set user's roles with scope")
    @CasbinAccess(resource = "system:role", action = "edit")
    public Result<Void> setUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody SetUserRolesRequest request) {

        List<AccessApplicationService.RoleAssignment> assignments = request.getAssignments().stream()
            .map(item -> {
                AccessApplicationService.RoleAssignment ra = new AccessApplicationService.RoleAssignment();
                ra.setRoleId(item.getRoleId());
                ra.setScopeType(item.getScopeType());
                ra.setScopeId(item.getScopeId());
                ra.setExpiresAt(item.getExpiresAt());
                ra.setReason(item.getReason());
                return ra;
            })
            .collect(Collectors.toList());

        accessService.setUserRoles(userId, assignments, SecurityUtils.requireCurrentUserId());
        return Result.success(null);
    }

    @GetMapping("/me/permissions")
    @Operation(summary = "Get current user's permissions")
    public Result<Set<String>> getMyPermissions() {
        Long userId = SecurityUtils.requireCurrentUserId();
        Set<String> permissions = accessService.getUserPermissions(userId);
        return Result.success(permissions);
    }

    @GetMapping("/me/roles")
    @Operation(summary = "Get current user's roles")
    public Result<List<RoleResponse>> getMyRoles() {
        Long userId = SecurityUtils.requireCurrentUserId();
        List<Role> roles = accessService.getUserRoles(userId);
        List<RoleResponse> responses = roles.stream()
            .map(this::toRoleResponse)
            .collect(Collectors.toList());
        return Result.success(responses);
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
        response.setPermissionIds(role.getPermissionIds());
        response.setCreatedAt(role.getCreatedAt());
        return response;
    }

    private UserRoleResponse toUserRoleResponse(UserRole userRole, Map<Long, Role> roleMap) {
        UserRoleResponse response = new UserRoleResponse();
        response.setId(userRole.getId());
        response.setUserId(userRole.getUserId());
        response.setRoleId(userRole.getRoleId());
        response.setScopeType(userRole.getScopeType());
        response.setScopeId(userRole.getScopeId());
        response.setAssignedAt(userRole.getAssignedAt());
        response.setExpiresAt(userRole.getExpiresAt());
        response.setIsActive(userRole.getIsActive());
        response.setReason(userRole.getReason());
        response.setGrantedBy(userRole.getGrantedBy());
        response.setGrantedAt(userRole.getGrantedAt());

        // Enrich with role info
        Role role = roleMap.get(userRole.getRoleId());
        if (role != null) {
            response.setRoleName(role.getRoleName());
            response.setRoleCode(role.getRoleCode());
        }

        // Enrich scope name for ORG_UNIT scope
        if (ScopeType.ORG_UNIT.equals(userRole.getScopeType())
                && userRole.getScopeId() != null && userRole.getScopeId() > 0) {
            try {
                String scopeName = jdbcTemplate.queryForObject(
                        "SELECT unit_name FROM org_units WHERE id = ? AND deleted = 0",
                        String.class, userRole.getScopeId());
                response.setScopeName(scopeName);
            } catch (Exception ignored) {
            }
        } else if (ScopeType.ALL.equals(userRole.getScopeType())) {
            response.setScopeName("全局");
        }

        return response;
    }
}
