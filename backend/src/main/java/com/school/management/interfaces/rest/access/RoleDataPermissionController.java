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
    public Result<List<DomainModulesDTO>> getModulesV5(
            @RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        return Result.success(dataPermissionService.getAllModulesGroupedByDomain(Boolean.TRUE.equals(includeDisabled)));
    }

    @GetMapping("/data-permissions/v5/scopes")
    @Operation(summary = "获取所有数据范围类型（V5）")
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<List<ScopeTypeDTO>> getScopesV5() {
        return Result.success(dataPermissionService.getAllScopeTypes());
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
                    mp.setTypeFilter(m.getTypeFilter());
                    // 可组合三轴 (T8): 回带当前态
                    mp.setOrgAnchor(m.getOrgAnchor());
                    mp.setAnchorParam(m.getAnchorParam());
                    mp.setIncludeSubtree(m.getIncludeSubtree());
                    mp.setCustomOrgIds(m.getCustomOrgIds());
                    mp.setSubjectRelInclude(m.getSubjectRelInclude());
                    mp.setSubjectRelExclude(m.getSubjectRelExclude());
                    mp.setRelationGrants(m.getRelationGrants());  // R3/R4: 回带多锚点
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
                .map(mp -> {
                    // 4-arg: moduleCode/scopeCode/scopeItems/typeFilter; 三轴用 setter 透传
                    SavePermissionCommand cmd = new SavePermissionCommand(
                            mp.getModuleCode(), mp.getScopeCode(), mp.getScopeItems(), mp.getTypeFilter());
                    cmd.setOrgAnchor(mp.getOrgAnchor());
                    cmd.setAnchorParam(mp.getAnchorParam());
                    cmd.setIncludeSubtree(mp.getIncludeSubtree());
                    cmd.setCustomOrgIds(mp.getCustomOrgIds());
                    cmd.setSubjectRelInclude(mp.getSubjectRelInclude());
                    cmd.setSubjectRelExclude(mp.getSubjectRelExclude());
                    cmd.setRelationGrants(mp.getRelationGrants());  // R3/R4: 透传多锚点
                    return cmd;
                })
                .collect(java.util.stream.Collectors.toList());
        dataPermissionService.saveRoleDataPermissions(roleId, commands);
        return Result.success(null);
    }

    @GetMapping("/data-permissions/scopes")
    @Operation(summary = "获取所有数据范围选项 (合并 CORE 5 种 + 插件贡献维度)")
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<List<DataScopeOptionDTO>> getScopes() {
        return Result.success(dataPermissionService.getAllScopeTypes().stream()
                .map(s -> new DataScopeOptionDTO(s.getCode(), s.getName(), s.getDescription(), s.getSource(), s.getIndustry()))
                .collect(java.util.stream.Collectors.toList()));
    }

    @GetMapping("/data-permissions/modules")
    @Operation(summary = "获取所有数据模块列表（按领域分组）- Legacy")
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<Map<String, List<Map<String, String>>>> getModules(
            @RequestParam(required = false, defaultValue = "false") Boolean includeDisabled) {
        return Result.success(dataPermissionService.getAllModules(Boolean.TRUE.equals(includeDisabled)));
    }

    @GetMapping("/data-permissions/resource-relations")
    @Operation(summary = "获取某资源已注册的可锚定关系 (R3c 多 grant 编辑器用; 数据驱动无硬编码)")
    @CasbinAccess(resource = "system:role", action = "view")
    public Result<List<ResourceRelationOption>> getResourceRelations(@RequestParam String module) {
        return Result.success(dataPermissionService.getResourceRelations(module));
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
        /** 旧预设码 (向后兼容); 未带三轴时由其翻译。 */
        private String scopeCode;
        private List<DataPermissionApplicationService.ScopeItemDTO> scopeItems;
        /** 类型过滤(闸2/2b)=轴③: 类型码集, 与组织范围 AND 组合; null/空=不限 */
        private List<String> typeFilter;

        // ── 可组合三轴 (T8) ──
        /** 轴① org anchor 名。 */
        private String orgAnchor;
        /** 轴① 参数: RELATION 时为关系码, PLUGIN_DIM 时为维度码。 */
        private String anchorParam;
        /** 轴① 是否含子树。 */
        private Boolean includeSubtree;
        /** 轴① CUSTOM_ORG 时的指定组织 id 集合。 */
        private List<Long> customOrgIds;
        /** 轴② 结果关系 include。 */
        private List<String> subjectRelInclude;
        /** 轴② 结果关系 exclude。 */
        private List<String> subjectRelExclude;
        /** R3/R4: 多锚点关系授予 (>1 = 多 grant 配置)。 */
        private List<com.school.management.domain.access.model.valueobject.RelationGrant> relationGrants;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class DataScopeOptionDTO {
        private String scopeCode;
        private String scopeName;
        private String description;
        /** "CORE" for hardcoded 5 enums, "PLUGIN:<domain>" for plugin-contributed dims */
        private String source;
        /** 行业归属码 (CORE/EDU...). 与 source 里的 domainCode 可能不同名 (domainCode=education vs industry=EDU),
         *  插件平台按 industry 归类用。 */
        private String industry;

        public DataScopeOptionDTO(String scopeCode, String scopeName, String description) {
            this(scopeCode, scopeName, description, "CORE", "CORE");
        }
    }
}
