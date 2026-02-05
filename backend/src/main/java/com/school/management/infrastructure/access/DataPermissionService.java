package com.school.management.infrastructure.access;

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
 * 数据权限服务 (V5)
 * 处理角色数据权限查询和多角色权限合并
 */
@Slf4j
@Service
public class DataPermissionService {

    private static final String CACHE_NAME = "dataPermission";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取角色在指定模块的数据权限配置
     */
    public RoleDataPermission getRoleModulePermission(Long roleId, String moduleCode) {
        String sql = "SELECT id, role_id, module_code, scope_code, description, created_at, updated_at " +
                "FROM role_data_permissions_v5 WHERE role_id = ? AND module_code = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, roleId, moduleCode);
        if (rows.isEmpty()) {
            return null;
        }

        Map<String, Object> row = rows.get(0);
        RoleDataPermission permission = mapToPermission(row);

        // 如果是CUSTOM范围，加载明细项
        if ("CUSTOM".equals(permission.getScopeCode())) {
            loadScopeItems(permission);
        }

        return permission;
    }

    /**
     * 获取角色的所有数据权限配置
     */
    public List<RoleDataPermission> getRolePermissions(Long roleId) {
        String sql = "SELECT id, role_id, module_code, scope_code, description, created_at, updated_at " +
                "FROM role_data_permissions_v5 WHERE role_id = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, roleId);
        List<RoleDataPermission> permissions = rows.stream()
                .map(this::mapToPermission)
                .collect(Collectors.toList());

        // 为CUSTOM范围加载明细项
        for (RoleDataPermission permission : permissions) {
            if ("CUSTOM".equals(permission.getScopeCode())) {
                loadScopeItems(permission);
            }
        }

        return permissions;
    }

    /**
     * 获取多个角色在指定模块的合并权限
     */
    @Cacheable(value = CACHE_NAME, key = "'merged:' + #roleIds.hashCode() + ':' + #moduleCode",
            unless = "#result == null")
    public MergedDataScope getMergedScope(List<Long> roleIds, String moduleCode) {
        if (roleIds == null || roleIds.isEmpty()) {
            return MergedDataScope.builder()
                    .moduleCode(moduleCode)
                    .effectiveScope(com.school.management.domain.access.model.DataScope.SELF)
                    .hasSelfScope(true)
                    .build();
        }

        // 查询所有角色在该模块的权限
        String placeholders = roleIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, role_id, module_code, scope_code, description " +
                "FROM role_data_permissions_v5 WHERE role_id IN (" + placeholders + ") AND module_code = ?";

        Object[] params = new Object[roleIds.size() + 1];
        for (int i = 0; i < roleIds.size(); i++) {
            params[i] = roleIds.get(i);
        }
        params[roleIds.size()] = moduleCode;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);

        if (rows.isEmpty()) {
            return MergedDataScope.builder()
                    .moduleCode(moduleCode)
                    .effectiveScope(com.school.management.domain.access.model.DataScope.SELF)
                    .hasSelfScope(true)
                    .sourceRoleIds(roleIds)
                    .build();
        }

        List<RoleDataPermission> permissions = rows.stream()
                .map(this::mapToPermission)
                .collect(Collectors.toList());

        // 为CUSTOM范围加载明细项
        for (RoleDataPermission permission : permissions) {
            if ("CUSTOM".equals(permission.getScopeCode())) {
                loadScopeItems(permission);
            }
        }

        return MergedDataScope.merge(moduleCode, permissions);
    }

    /**
     * 保存角色数据权限配置
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void saveRolePermission(RoleDataPermission permission) {
        // 先删除旧配置
        jdbcTemplate.update(
                "DELETE FROM role_data_permissions_v5 WHERE role_id = ? AND module_code = ?",
                permission.getRoleId(), permission.getModuleCode());

        // 插入新配置
        String insertSql = "INSERT INTO role_data_permissions_v5 (role_id, module_code, scope_code, description) " +
                "VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(insertSql,
                permission.getRoleId(),
                permission.getModuleCode(),
                permission.getScopeCode(),
                permission.getDescription());

        // 获取新插入的ID
        Long permissionId = jdbcTemplate.queryForObject(
                "SELECT id FROM role_data_permissions_v5 WHERE role_id = ? AND module_code = ?",
                Long.class, permission.getRoleId(), permission.getModuleCode());

        // 如果是CUSTOM范围，保存明细项
        if ("CUSTOM".equals(permission.getScopeCode()) && permission.getScopeItems() != null) {
            for (DataScopeItem item : permission.getScopeItems()) {
                jdbcTemplate.update(
                        "INSERT INTO role_data_scope_items (role_data_permission_id, item_type_code, scope_id, scope_name, include_children) " +
                                "VALUES (?, ?, ?, ?, ?)",
                        permissionId, item.getItemTypeCode(), item.getScopeId(),
                        item.getScopeName(), item.getIncludeChildren() ? 1 : 0);
            }
        }

        log.info("Saved role data permission: roleId={}, moduleCode={}, scopeCode={}",
                permission.getRoleId(), permission.getModuleCode(), permission.getScopeCode());
    }

    /**
     * 批量保存角色数据权限
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void saveRolePermissions(Long roleId, List<RoleDataPermission> permissions) {
        // 删除角色的所有旧配置
        jdbcTemplate.update("DELETE FROM role_data_permissions_v5 WHERE role_id = ?", roleId);

        // 批量插入新配置
        for (RoleDataPermission permission : permissions) {
            permission.setRoleId(roleId);
            saveRolePermission(permission);
        }
    }

    /**
     * 清除缓存
     */
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearCache() {
        log.info("Data permission cache cleared");
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
