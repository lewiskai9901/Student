package com.school.management.interfaces.rest.access;

import com.school.management.application.access.DataPermissionApplicationService;
import com.school.management.application.access.DataPermissionApplicationService.*;
import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色数据权限管理控制器 (V5)
 * 支持模块级数据权限配置和自定义范围
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "角色数据权限管理", description = "V5 API - 角色数据权限配置，支持模块级范围和自定义范围项")
public class RoleDataPermissionController {

    private final DataPermissionApplicationService dataPermissionService;

    // ==================== V5 API Endpoints ====================

    @GetMapping("/data-permissions/v5/modules")
    @Operation(summary = "获取所有数据模块（V5按领域分组）")
    @PreAuthorize("hasAuthority('system:role:view') or hasAuthority('system:role:edit')")
    public Result<List<DomainModulesDTO>> getModulesV5() {
        return Result.success(dataPermissionService.getAllModulesGroupedByDomain());
    }

    @GetMapping("/data-permissions/v5/scopes")
    @Operation(summary = "获取所有数据范围类型（V5）")
    @PreAuthorize("hasAuthority('system:role:view') or hasAuthority('system:role:edit')")
    public Result<List<ScopeTypeDTO>> getScopesV5() {
        return Result.success(dataPermissionService.getAllScopeTypes());
    }

    @GetMapping("/data-permissions/v5/scope-item-types")
    @Operation(summary = "获取所有范围项类型（V5）")
    @PreAuthorize("hasAuthority('system:role:view') or hasAuthority('system:role:edit')")
    public Result<List<ScopeItemTypeDTO>> getScopeItemTypes() {
        return Result.success(dataPermissionService.getAllScopeItemTypes());
    }

    @GetMapping("/data-permissions/v5/scope-items")
    @Operation(summary = "搜索自定义范围可选项（V5）")
    @PreAuthorize("hasAuthority('system:role:view') or hasAuthority('system:role:edit')")
    public Result<List<ScopeItemDTO>> searchScopeItems(
            @Parameter(description = "范围项类型代码") @RequestParam String itemTypeCode,
            @Parameter(description = "搜索关键字") @RequestParam(defaultValue = "") String keyword,
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "20") int limit) {
        return Result.success(dataPermissionService.searchScopeItems(itemTypeCode, keyword, limit));
    }

    @GetMapping("/{roleId}/data-permissions/v5")
    @Operation(summary = "获取角色数据权限配置（V5）")
    @PreAuthorize("hasAuthority('system:role:view') or hasAuthority('system:role:edit')")
    public Result<List<RoleModulePermissionDTO>> getPermissionsV5(@PathVariable Long roleId) {
        return Result.success(dataPermissionService.getRoleDataPermissionsV5(roleId));
    }

    @PutMapping("/{roleId}/data-permissions/v5")
    @Operation(summary = "保存角色数据权限配置（V5）")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<Void> savePermissionsV5(
            @PathVariable Long roleId,
            @RequestBody List<SavePermissionCommand> commands) {
        dataPermissionService.saveRoleDataPermissionsV5(roleId, commands);
        return Result.success(null);
    }

    // ==================== Legacy API Endpoints (backward compatibility) ====================

    @GetMapping("/data-permissions/modules")
    @Operation(summary = "获取所有数据模块列表（按领域分组）- Legacy")
    @PreAuthorize("hasAuthority('system:role:view') or hasAuthority('system:role:edit')")
    public Result<Map<String, List<Map<String, String>>>> getModules() {
        return Result.success(dataPermissionService.getAllModules());
    }

    @GetMapping("/data-permissions/scopes")
    @Operation(summary = "获取所有数据范围选项 - Legacy")
    @PreAuthorize("hasAuthority('system:role:view') or hasAuthority('system:role:edit')")
    public Result<List<Map<String, String>>> getScopes() {
        return Result.success(dataPermissionService.getAllScopes());
    }
}
