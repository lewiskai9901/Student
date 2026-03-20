package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.entity.DataScopeItem;
import com.school.management.domain.access.model.entity.RoleDataPermission;
import com.school.management.domain.access.model.valueobject.MergedDataScope;
import com.school.management.application.access.DynamicModuleService;
import com.school.management.infrastructure.persistence.access.DataModulePO;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Data permission policy service.
 * Replaces DataPermissionService with tenant-aware + Casbin-driven approach.
 * Uses MyBatis parameterized queries instead of string concatenation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPermissionPolicyService {

    private static final String CACHE_NAME = "dataPermissionPolicy";

    private final JdbcTemplate jdbcTemplate;
    private final DynamicModuleService dynamicModuleService;

    /**
     * Get merged data scope for multiple roles on a specific module.
     * Cache key uses sorted roleIds to avoid ordering issues.
     */
    @Cacheable(value = CACHE_NAME,
            key = "'merged:' + #tenantId + ':' + T(java.util.stream.Collectors).joining(',', #roleIds.stream().sorted().map(T(String)::valueOf)) + ':' + #moduleCode",
            unless = "#result == null")
    public MergedDataScope getMergedScope(Long tenantId, List<Long> roleIds, String moduleCode) {
        if (roleIds == null || roleIds.isEmpty()) {
            return MergedDataScope.builder()
                    .moduleCode(moduleCode)
                    .effectiveScope(DataScope.SELF)
                    .hasSelfScope(true)
                    .build();
        }

        // Query permissions using parameterized placeholders
        String placeholders = roleIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, role_id, module_code, scope_code, description " +
                "FROM role_data_permissions_v5 WHERE tenant_id = ? AND role_id IN (" +
                placeholders + ") AND module_code = ?";

        Object[] params = new Object[roleIds.size() + 2];
        params[0] = tenantId;
        for (int i = 0; i < roleIds.size(); i++) {
            params[i + 1] = roleIds.get(i);
        }
        params[roleIds.size() + 1] = moduleCode;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);

        if (rows.isEmpty()) {
            return MergedDataScope.builder()
                    .moduleCode(moduleCode)
                    .effectiveScope(DataScope.SELF)
                    .hasSelfScope(true)
                    .sourceRoleIds(roleIds)
                    .build();
        }

        List<RoleDataPermission> permissions = rows.stream()
                .map(this::mapToPermission)
                .collect(Collectors.toList());

        // Load CUSTOM scope items
        for (RoleDataPermission permission : permissions) {
            if ("CUSTOM".equals(permission.getScopeCode())) {
                loadScopeItems(permission);
            }
        }

        return MergedDataScope.merge(moduleCode, permissions);
    }

    /**
     * Save role data permission configuration
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void saveRolePermission(Long tenantId, RoleDataPermission permission) {
        // Delete old config
        jdbcTemplate.update(
                "DELETE FROM role_data_permissions_v5 WHERE tenant_id = ? AND role_id = ? AND module_code = ?",
                tenantId, permission.getRoleId(), permission.getModuleCode());

        // Insert new config
        jdbcTemplate.update(
                "INSERT INTO role_data_permissions_v5 (tenant_id, role_id, module_code, scope_code, description) VALUES (?, ?, ?, ?, ?)",
                tenantId, permission.getRoleId(), permission.getModuleCode(),
                permission.getScopeCode(), permission.getDescription());

        // Get new ID
        Long permissionId = jdbcTemplate.queryForObject(
                "SELECT id FROM role_data_permissions_v5 WHERE tenant_id = ? AND role_id = ? AND module_code = ?",
                Long.class, tenantId, permission.getRoleId(), permission.getModuleCode());

        // Save CUSTOM scope items
        if ("CUSTOM".equals(permission.getScopeCode()) && permission.getScopeItems() != null) {
            for (DataScopeItem item : permission.getScopeItems()) {
                jdbcTemplate.update(
                        "INSERT INTO role_data_scope_items (tenant_id, role_data_permission_id, item_type_code, scope_id, scope_name, include_children) VALUES (?, ?, ?, ?, ?, ?)",
                        tenantId, permissionId, item.getItemTypeCode(), item.getScopeId(),
                        item.getScopeName(), item.getIncludeChildren() ? 1 : 0);
            }
        }

        log.info("Saved role data permission: tenantId={}, roleId={}, moduleCode={}, scopeCode={}",
                tenantId, permission.getRoleId(), permission.getModuleCode(), permission.getScopeCode());
    }

    /**
     * Batch save role data permissions
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void saveRolePermissions(Long tenantId, Long roleId, List<RoleDataPermission> permissions) {
        jdbcTemplate.update("DELETE FROM role_data_permissions_v5 WHERE tenant_id = ? AND role_id = ?",
                tenantId, roleId);
        for (RoleDataPermission permission : permissions) {
            permission.setRoleId(roleId);
            saveRolePermission(tenantId, permission);
        }
    }

    /**
     * Get role permissions
     */
    public List<RoleDataPermission> getRolePermissions(Long tenantId, Long roleId) {
        String sql = "SELECT id, role_id, module_code, scope_code, description " +
                "FROM role_data_permissions_v5 WHERE tenant_id = ? AND role_id = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tenantId, roleId);
        List<RoleDataPermission> permissions = rows.stream()
                .map(this::mapToPermission)
                .collect(Collectors.toList());

        for (RoleDataPermission permission : permissions) {
            if ("CUSTOM".equals(permission.getScopeCode())) {
                loadScopeItems(permission);
            }
        }

        return permissions;
    }

    /**
     * Get the DataScope for a single role on a specific module.
     * Returns null if no permission is configured.
     */
    public DataScope getScopeForRole(Long tenantId, Long roleId, String moduleCode) {
        String sql = "SELECT scope_code FROM role_data_permissions_v5 " +
                "WHERE tenant_id = ? AND role_id = ? AND module_code = ? LIMIT 1";
        try {
            String scopeCode = jdbcTemplate.queryForObject(sql, String.class, tenantId, roleId, moduleCode);
            if (scopeCode != null) {
                try {
                    return DataScope.valueOf(scopeCode);
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown scope_code '{}' for role {} module {}", scopeCode, roleId, moduleCode);
                }
            }
        } catch (Exception e) {
            // No config found → return null
        }
        return null;
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearCache() {
        log.info("Data permission policy cache cleared");
    }

    private RoleDataPermission mapToPermission(Map<String, Object> row) {
        return RoleDataPermission.builder()
                .id(getLongValue(row, "id"))
                .roleId(getLongValue(row, "role_id"))
                .moduleCode((String) row.get("module_code"))
                .scopeCode((String) row.get("scope_code"))
                .description((String) row.get("description"))
                .build();
    }

    private void loadScopeItems(RoleDataPermission permission) {
        String sql = "SELECT id, role_data_permission_id, item_type_code, scope_id, scope_name, include_children " +
                "FROM role_data_scope_items WHERE role_data_permission_id = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, permission.getId());

        List<DataScopeItem> items = rows.stream()
                .map(row -> DataScopeItem.builder()
                        .id(getLongValue(row, "id"))
                        .roleDataPermissionId(getLongValue(row, "role_data_permission_id"))
                        .itemTypeCode((String) row.get("item_type_code"))
                        .scopeId(getLongValue(row, "scope_id"))
                        .scopeName((String) row.get("scope_name"))
                        .includeChildren(getIntValue(row, "include_children") == 1)
                        .build())
                .collect(Collectors.toList());

        permission.setScopeItems(items);
    }

    private Long getLongValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    private int getIntValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }
}
