package com.school.management.interfaces.rest.access;

import com.school.management.common.ApiResponse;
import com.school.management.dto.RoleDataPermissionDTO;
import com.school.management.service.RoleDataPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V2 角色数据权限管理控制器
 */
@RestController
@RequestMapping("/v2/roles")
@RequiredArgsConstructor
@Tag(name = "角色数据权限管理 V2", description = "V2 API - 角色数据权限配置，支持DDD对齐模块和自定义范围")
public class RoleDataPermissionController {

    private final RoleDataPermissionService service;

    @GetMapping("/{roleId}/data-permissions")
    @Operation(summary = "获取角色数据权限配置")
    @PreAuthorize("hasAuthority('system:role:view') or hasAuthority('system:role:edit')")
    public ApiResponse<RoleDataPermissionDTO.RolePermissionConfig> getPermissions(@PathVariable Long roleId) {
        return ApiResponse.success(service.getRolePermissionConfigV2(roleId));
    }

    @PutMapping("/{roleId}/data-permissions")
    @Operation(summary = "更新角色数据权限配置")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public ApiResponse<Void> updatePermissions(
            @PathVariable Long roleId,
            @RequestBody RoleDataPermissionDTO.RolePermissionConfig config) {
        service.saveRolePermissionConfigV2(roleId, config);
        return ApiResponse.success(null);
    }

    @GetMapping("/data-permissions/modules")
    @Operation(summary = "获取所有数据模块列表（按领域分组）")
    @PreAuthorize("hasAuthority('system:role:view') or hasAuthority('system:role:edit')")
    public ApiResponse<Map<String, List<Map<String, String>>>> getModules() {
        return ApiResponse.success(service.getAllModulesV2());
    }

    @GetMapping("/data-permissions/scopes")
    @Operation(summary = "获取所有数据范围选项")
    @PreAuthorize("hasAuthority('system:role:view') or hasAuthority('system:role:edit')")
    public ApiResponse<List<Map<String, String>>> getScopes() {
        return ApiResponse.success(service.getAllScopesV2());
    }
}
