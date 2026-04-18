package com.school.management.infrastructure.access;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.entity.DataScopeItem;
import com.school.management.domain.access.model.entity.RoleDataPermission;
import com.school.management.domain.access.model.valueobject.MergedDataScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据权限策略服务 (v3 改读 role_data_scopes 替代 role_data_permissions_v5)
 *
 * 关键字段映射:
 *   v2: module_code / scope_code        (role_data_permissions_v5)
 *   v3: resource_code / scope_type      (role_data_scopes)
 *
 * CUSTOM scope 自定义项:
 *   v2: role_data_scope_items 独立表 (item_type_code / scope_id / scope_name / include_children)
 *   v3: role_data_scopes.custom_org_unit_ids JSON 列表
 *   简化: v3 CUSTOM 就是"给这个角色指定这些组织 ID",不再区分 item_type_code。
 */
@Slf4j
@Service
public class DataPermissionPolicyService {

    private static final String CACHE_NAME = "dataPermissionPolicy";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DataPermissionPolicyService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取多角色合并 DataScope。
     * Cache key 使用 sorted roleIds 避免顺序问题。
     */
    @Cacheable(value = CACHE_NAME,
            key = "'merged:' + #tenantId + ':' + new java.util.TreeSet(#roleIds) + ':' + #moduleCode",
            unless = "#result == null")
    public MergedDataScope getMergedScope(Long tenantId, List<Long> roleIds, String moduleCode) {
        if (roleIds == null || roleIds.isEmpty()) {
            return MergedDataScope.builder()
                    .moduleCode(moduleCode)
                    .effectiveScope(DataScope.SELF)
                    .hasSelfScope(true)
                    .build();
        }

        // v3: role_data_scopes (resource_code / scope_type / custom_org_unit_ids)
        String placeholders = roleIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, role_id, resource_code, scope_type, custom_org_unit_ids " +
                "FROM role_data_scopes WHERE tenant_id = ? AND role_id IN (" +
                placeholders + ") AND resource_code = ? AND deleted = 0";

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

        // CUSTOM scope: 从 custom_org_unit_ids JSON 解析组织 ID 列表
        for (RoleDataPermission permission : permissions) {
            if ("CUSTOM".equals(permission.getScopeCode())) {
                loadCustomOrgUnitIds(permission, rows);
            }
        }

        return MergedDataScope.merge(moduleCode, permissions);
    }

    /** 保存单条角色数据权限配置 */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void saveRolePermission(Long tenantId, RoleDataPermission permission) {
        // 软删除旧配置 (同一 role+resource 的旧记录置 deleted=1)
        jdbcTemplate.update(
                "UPDATE role_data_scopes SET deleted = 1 " +
                "WHERE tenant_id = ? AND role_id = ? AND resource_code = ? AND deleted = 0",
                tenantId, permission.getRoleId(), permission.getModuleCode());

        // 构造 custom_org_unit_ids JSON
        String customJson = null;
        if ("CUSTOM".equals(permission.getScopeCode()) && permission.getScopeItems() != null
                && !permission.getScopeItems().isEmpty()) {
            try {
                List<Long> orgIds = permission.getScopeItems().stream()
                        .map(DataScopeItem::getScopeId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                customJson = objectMapper.writeValueAsString(orgIds);
            } catch (Exception e) {
                log.warn("Failed to serialize custom_org_unit_ids: {}", e.getMessage());
            }
        }

        jdbcTemplate.update(
                "INSERT INTO role_data_scopes (tenant_id, role_id, resource_code, scope_type, custom_org_unit_ids, priority, created_at, deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), 0)",
                tenantId, permission.getRoleId(), permission.getModuleCode(),
                permission.getScopeCode(), customJson,
                0);

        log.info("Saved role data scope: tenantId={}, roleId={}, resourceCode={}, scopeType={}",
                tenantId, permission.getRoleId(), permission.getModuleCode(), permission.getScopeCode());
    }

    /** 批量保存角色的所有数据权限 */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void saveRolePermissions(Long tenantId, Long roleId, List<RoleDataPermission> permissions) {
        jdbcTemplate.update(
                "UPDATE role_data_scopes SET deleted = 1 WHERE tenant_id = ? AND role_id = ? AND deleted = 0",
                tenantId, roleId);
        for (RoleDataPermission permission : permissions) {
            permission.setRoleId(roleId);
            saveRolePermission(tenantId, permission);
        }
    }

    /** 获取角色的所有权限配置 */
    public List<RoleDataPermission> getRolePermissions(Long tenantId, Long roleId) {
        String sql = "SELECT id, role_id, resource_code, scope_type, custom_org_unit_ids " +
                "FROM role_data_scopes WHERE tenant_id = ? AND role_id = ? AND deleted = 0";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tenantId, roleId);
        List<RoleDataPermission> permissions = rows.stream()
                .map(this::mapToPermission)
                .collect(Collectors.toList());

        for (RoleDataPermission permission : permissions) {
            if ("CUSTOM".equals(permission.getScopeCode())) {
                loadCustomOrgUnitIds(permission, rows);
            }
        }

        return permissions;
    }

    /** 获取单角色在某资源上的 DataScope */
    public DataScope getScopeForRole(Long tenantId, Long roleId, String moduleCode) {
        String sql = "SELECT scope_type FROM role_data_scopes " +
                "WHERE tenant_id = ? AND role_id = ? AND resource_code = ? AND deleted = 0 LIMIT 1";
        try {
            String scopeCode = jdbcTemplate.queryForObject(sql, String.class, tenantId, roleId, moduleCode);
            if (scopeCode != null) {
                try {
                    return DataScope.valueOf(scopeCode);
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown scope_type '{}' for role {} resource {}", scopeCode, roleId, moduleCode);
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

    // ══════════════════════════════════════════════════════════════
    // 内部映射
    // ══════════════════════════════════════════════════════════════

    private RoleDataPermission mapToPermission(Map<String, Object> row) {
        return RoleDataPermission.builder()
                .id(getLongValue(row, "id"))
                .roleId(getLongValue(row, "role_id"))
                .moduleCode((String) row.get("resource_code"))  // v3 字段映射
                .scopeCode((String) row.get("scope_type"))      // v3 字段映射
                .description(null)  // v3 无 description 字段
                .build();
    }

    /** 从 custom_org_unit_ids JSON 构造 scopeItems 列表 */
    private void loadCustomOrgUnitIds(RoleDataPermission permission, List<Map<String, Object>> allRows) {
        // 找到 permission 对应的行
        Map<String, Object> myRow = allRows.stream()
                .filter(r -> Objects.equals(getLongValue(r, "id"), permission.getId()))
                .findFirst().orElse(null);
        if (myRow == null) return;

        Object json = myRow.get("custom_org_unit_ids");
        if (json == null) return;

        try {
            List<Long> orgIds = objectMapper.readValue(json.toString(),
                    new TypeReference<List<Long>>() {});
            List<DataScopeItem> items = orgIds.stream()
                    .map(id -> DataScopeItem.builder()
                            .roleDataPermissionId(permission.getId())
                            .itemTypeCode("org_unit")     // v3 无 item_type_code,统一 'org_unit'
                            .scopeId(id)
                            .scopeName(null)              // 按需加载
                            .includeChildren(true)        // v3 默认 include_children
                            .build())
                    .collect(Collectors.toList());
            permission.setScopeItems(items);
        } catch (Exception e) {
            log.warn("Failed to parse custom_org_unit_ids: {}", e.getMessage());
        }
    }

    private Long getLongValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
}
