package com.school.management.interfaces.rest.access;

import com.school.management.application.access.DataPermissionApplicationService;
import com.school.management.application.access.DataPermissionApplicationService.*;
import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.school.management.infrastructure.casbin.CasbinAccess;
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
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<List<DomainModulesDTO>> getModulesV5() {
        return Result.success(dataPermissionService.getAllModulesGroupedByDomain());
    }

    @GetMapping("/data-permissions/v5/scopes")
    @Operation(summary = "获取所有数据范围类型（V5）")
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<List<ScopeTypeDTO>> getScopesV5() {
        return Result.success(dataPermissionService.getAllScopeTypes());
    }

    @GetMapping("/data-permissions/v5/scope-item-types")
    @Operation(summary = "获取所有范围项类型（V5）")
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<List<ScopeItemTypeDTO>> getScopeItemTypes() {
        return Result.success(dataPermissionService.getAllScopeItemTypes());
    }

    @GetMapping("/data-permissions/v5/scope-items")
    @Operation(summary = "搜索自定义范围可选项（V5）")
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<List<ScopeItemDTO>> searchScopeItems(
            @Parameter(description = "范围项类型代码") @RequestParam String itemTypeCode,
            @Parameter(description = "搜索关键字") @RequestParam(defaultValue = "") String keyword,
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "20") int limit) {
        return Result.success(dataPermissionService.searchScopeItems(itemTypeCode, keyword, limit));
    }

    @GetMapping("/{roleId}/data-permissions/v5")
    @Operation(summary = "获取角色数据权限配置（V5）")
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<List<RoleModulePermissionDTO>> getPermissionsV5(@PathVariable Long roleId) {
        return Result.success(dataPermissionService.getRoleDataPermissions(roleId));
    }

    @PutMapping("/{roleId}/data-permissions/v5")
    @Operation(summary = "保存角色数据权限配置（V5）")
    @CasbinAccess(resource = "system:role", action = "edit")
    public Result<Void> savePermissionsV5(
            @PathVariable Long roleId,
            @RequestBody List<SavePermissionCommand> commands) {
        dataPermissionService.saveRoleDataPermissions(roleId, commands);
        return Result.success(null);
    }

    // ==================== Clean API Endpoints (no version suffix) ====================

    @GetMapping("/{roleId}/data-permissions")
    @Operation(summary = "获取角色数据权限配置")
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<RolePermissionConfigDTO> getPermissions(@PathVariable Long roleId) {
        List<RoleModulePermissionDTO> modules = dataPermissionService.getRoleDataPermissions(roleId);
        // Wrap into RolePermissionConfig format matching frontend expectations
        RolePermissionConfigDTO config = new RolePermissionConfigDTO();
        config.setRoleId(roleId);
        config.setRoleName("");
        config.setModulePermissions(modules.stream()
                .map(m -> {
                    ModulePermissionDTO mp = new ModulePermissionDTO();
                    mp.setModuleCode(m.getModuleCode());
                    mp.setScopeCode(m.getScopeCode());
                    mp.setScopeItems(m.getScopeItems());
                    return mp;
                })
                .collect(java.util.stream.Collectors.toList()));
        return Result.success(config);
    }

    @PutMapping("/{roleId}/data-permissions")
    @Operation(summary = "保存角色数据权限配置")
    @CasbinAccess(resource = "system:role", action = "edit")
    public Result<Void> savePermissions(
            @PathVariable Long roleId,
            @RequestBody RolePermissionConfigDTO config) {
        List<SavePermissionCommand> commands = config.getModulePermissions().stream()
                .map(mp -> new SavePermissionCommand(mp.getModuleCode(), mp.getScopeCode(), mp.getScopeItems()))
                .collect(java.util.stream.Collectors.toList());
        dataPermissionService.saveRoleDataPermissions(roleId, commands);
        return Result.success(null);
    }

    @GetMapping("/data-permissions/scopes")
    @Operation(summary = "获取所有数据范围选项")
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<List<DataScopeOptionDTO>> getScopes() {
        return Result.success(dataPermissionService.getAllScopeTypes().stream()
                .map(s -> new DataScopeOptionDTO(s.getCode(), s.getName(), s.getDescription()))
                .collect(java.util.stream.Collectors.toList()));
    }

    @GetMapping("/data-permissions/modules")
    @Operation(summary = "获取所有数据模块列表（按领域分组）- Legacy")
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<Map<String, List<Map<String, String>>>> getModules() {
        return Result.success(dataPermissionService.getAllModules());
    }

    // ==================== DTO for frontend contract ====================

    @lombok.Data
    public static class RolePermissionConfigDTO {
        private Long roleId;
        private String roleName;
        private List<ModulePermissionDTO> modulePermissions;
    }

    @lombok.Data
    public static class ModulePermissionDTO {
        private String moduleCode;
        private String scopeCode;
        private List<DataPermissionApplicationService.ScopeItemDTO> scopeItems;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DataScopeOptionDTO {
        private String scopeCode;
        private String scopeName;
        private String description;
    }
}
