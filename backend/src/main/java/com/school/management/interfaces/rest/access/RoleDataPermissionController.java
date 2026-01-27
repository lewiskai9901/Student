package com.school.management.interfaces.rest.access;

import com.school.management.application.access.DataPermissionApplicationService;
import com.school.management.application.access.DataPermissionApplicationService.RolePermissionConfig;
import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V2 角色数据权限管理控制器 (DDD架构)
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "角色数据权限管理 V2", description = "V2 API - 角色数据权限配置，支持DDD对齐模块和自定义范围")
public class RoleDataPermissionController {

    private final DataPermissionApplicationService dataPermissionService;

    @GetMapping("/{roleId}/data-permissions")
    @Operation(summary = "获取角色数据权限配置")
    @PreAuthorize("hasAuthority('system:role:view') or hasAuthority('system:role:edit')")
    public Result<RolePermissionConfig> getPermissions(@PathVariable Long roleId) {
        return Result.success(dataPermissionService.getRolePermissionConfig(roleId));
    }

    @PutMapping("/{roleId}/data-permissions")
    @Operation(summary = "更新角色数据权限配置")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<Void> updatePermissions(
            @PathVariable Long roleId,
            @RequestBody RolePermissionConfig config) {
        dataPermissionService.saveRolePermissionConfig(roleId, config);
        return Result.success(null);
    }

    @GetMapping("/data-permissions/modules")
    @Operation(summary = "获取所有数据模块列表（按领域分组）")
    @PreAuthorize("hasAuthority('system:role:view') or hasAuthority('system:role:edit')")
    public Result<Map<String, List<Map<String, String>>>> getModules() {
        return Result.success(dataPermissionService.getAllModules());
    }

    @GetMapping("/data-permissions/scopes")
    @Operation(summary = "获取所有数据范围选项")
    @PreAuthorize("hasAuthority('system:role:view') or hasAuthority('system:role:edit')")
    public Result<List<Map<String, String>>> getScopes() {
        return Result.success(dataPermissionService.getAllScopes());
    }
}
