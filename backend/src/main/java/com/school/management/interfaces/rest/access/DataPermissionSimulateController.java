package com.school.management.interfaces.rest.access;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据权限模拟预览 — 给定 (userId + modulePermissions 快照), 逐模块计算
 * 该用户实际能访问的数据条数 + 前 3 条样本.
 *
 * 用于 /access/data-permissions 右栏 "模拟用户" 按钮:
 * 管理员还没保存的配置快照 也能预览, 避免"配完才发现范围不对".
 *
 * MVP 实现:
 *   - 支持的模块: user/org_unit/role/place/student/school_class (按 resolveMeta 列出)
 *   - 支持的 scope: ALL / SELF / DEPARTMENT / DEPARTMENT_AND_BELOW / CUSTOM
 *   - 其他维度 (BY_CLASS/BY_GRADE/BY_WARD 等插件 scope) 返回 "暂未支持模拟"
 *   - 部分表无 created_by (classes), SELF scope 会降级 返 "未支持"
 */
@RestController
@RequestMapping("/access/data-permissions/simulate")
@RequiredArgsConstructor
@Slf4j
public class DataPermissionSimulateController {

    private final JdbcTemplate jdbc;

    @PostMapping
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> simulate(@RequestBody SimulateRequest req) {
        if (req.getUserId() == null) {
            return Result.success(Map.of("error", "userId 必填"));
        }

        // 查模拟用户的 primaryOrgUnitId (DEPT / DEPT_AND_BELOW 需要)
        Long userOrgId = null;
        try {
            userOrgId = jdbc.queryForObject(
                "SELECT primary_org_unit_id FROM users WHERE id = ? AND deleted = 0",
                Long.class, req.getUserId());
        } catch (Exception e) {
            log.warn("user {} primary_org_unit_id not found: {}", req.getUserId(), e.getMessage());
        }

        List<Map<String, Object>> results = new ArrayList<>();
        List<ModulePermSnapshot> mps = req.getModulePermissions();
        if (mps != null) {
            for (ModulePermSnapshot mp : mps) {
                results.add(simulateModule(mp, req.getUserId(), userOrgId));
            }
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", String.valueOf(req.getUserId()));
        body.put("userOrgUnitId", userOrgId == null ? null : String.valueOf(userOrgId));
        body.put("results", results);
        return Result.success(body);
    }

    /** 模拟单个模块. 用粗粒度 COUNT + LIMIT 3 取样本. */
    private Map<String, Object> simulateModule(ModulePermSnapshot mp, Long userId, Long userOrgId) {
        String code = mp.getModuleCode();
        String scope = mp.getScopeCode();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("moduleCode", code);
        result.put("scopeCode", scope);

        ModuleMeta meta = resolveMeta(code);
        if (meta == null) {
            result.put("accessibleCount", -1);
            result.put("note", "此模块未实现模拟");
            return result;
        }

        try {
            String whereClause = buildWhere(scope, userId, userOrgId, mp, meta);
            if (whereClause == null) {
                result.put("accessibleCount", -1);
                result.put("note", "scope " + scope + " 暂未支持模拟");
                return result;
            }

            Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + meta.table + " WHERE " + whereClause, Long.class);
            result.put("accessibleCount", count != null ? count : 0);

            String nameSelect = meta.nameCol != null ? ", " + meta.nameCol + " AS name" : "";
            String sampleSql = "SELECT id" + nameSelect +
                    " FROM " + meta.table + " WHERE " + whereClause + " LIMIT 3";
            List<Map<String, Object>> samples = jdbc.queryForList(sampleSql);
            // id 转 string, 防 JS 精度丢失
            List<Map<String, Object>> normalized = new ArrayList<>();
            for (Map<String, Object> s : samples) {
                Map<String, Object> row = new LinkedHashMap<>();
                Object id = s.get("id");
                row.put("id", id == null ? null : id.toString());
                if (s.containsKey("name")) row.put("name", s.get("name"));
                normalized.add(row);
            }
            result.put("samples", normalized);

        } catch (Exception e) {
            log.warn("simulate module {} scope {} failed: {}", code, scope, e.getMessage());
            result.put("accessibleCount", -1);
            result.put("note", "模拟查询失败: " + e.getClass().getSimpleName());
        }
        return result;
    }

    /** 模块 → 表元数据. 未列出的模块返 null. */
    private ModuleMeta resolveMeta(String moduleCode) {
        if (moduleCode == null) return null;
        switch (moduleCode) {
            case "user":
            case "system_user":
                return new ModuleMeta("users", "real_name", "primary_org_unit_id", true, true);
            case "org_unit":
                return new ModuleMeta("org_units", "unit_name", "id", true, true);
            case "role":
            case "system_role":
                return new ModuleMeta("roles", "role_name", null, true, false);
            case "place":
                return new ModuleMeta("places", "place_name", "org_unit_id", true, true);
            case "student":
                return new ModuleMeta("user_student", "student_no", "org_unit_id", true, true);
            case "school_class":
            case "class":
                // classes 表无 created_by 列
                return new ModuleMeta("classes", "class_name", "org_unit_id", true, false);
            default:
                return null;
        }
    }

    /** 构造 SQL WHERE. 返 null = 暂未支持. */
    private String buildWhere(String scope, Long userId, Long userOrgId,
                               ModulePermSnapshot mp, ModuleMeta meta) {
        if (scope == null) return null;
        String delFilter = meta.hasDeleted ? " AND deleted = 0" : "";

        switch (scope) {
            case "ALL":
                return "1=1" + delFilter;

            case "SELF":
                if (!meta.hasCreatedBy) return null;
                return "created_by = " + userId + delFilter;

            case "DEPARTMENT":
                if (meta.orgCol == null || userOrgId == null) return null;
                return meta.orgCol + " = " + userOrgId + delFilter;

            case "DEPARTMENT_AND_BELOW":
                if (meta.orgCol == null || userOrgId == null) return null;
                // 使用 tree_path 子树: org_units.tree_path LIKE 'xxx/userOrgId/%' OR id = userOrgId
                // 这里按 org_unit_id IN (子树 id 列表)
                return meta.orgCol + " IN (" +
                        "SELECT id FROM org_units WHERE id = " + userOrgId +
                        " OR tree_path LIKE CONCAT((SELECT IFNULL(tree_path,'') FROM org_units o2 WHERE o2.id = " + userOrgId +
                        "), '/', " + userOrgId + ", '%')" +
                        ")" + delFilter;

            case "CUSTOM":
                List<Long> ids = extractScopeIds(mp.getScopeItems());
                if (ids == null || ids.isEmpty()) return "1=0";  // 空自定义 = 空集
                if (meta.orgCol == null) return null;
                String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
                return meta.orgCol + " IN (" + joined + ")" + delFilter;

            default:
                // BY_CLASS / BY_GRADE / BY_MAJOR / BY_WARD 等插件维度
                return null;
        }
    }

    /** 兼容两种 scopeItems 格式: List<Long> / List<{scopeId}>. */
    @SuppressWarnings("unchecked")
    private List<Long> extractScopeIds(List<?> items) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        List<Long> out = new ArrayList<>();
        for (Object item : items) {
            if (item == null) continue;
            if (item instanceof Number) {
                out.add(((Number) item).longValue());
            } else if (item instanceof String) {
                try { out.add(Long.parseLong((String) item)); } catch (NumberFormatException ignore) {}
            } else if (item instanceof Map) {
                Object v = ((Map<String, Object>) item).get("scopeId");
                if (v == null) v = ((Map<String, Object>) item).get("id");
                if (v instanceof Number) out.add(((Number) v).longValue());
                else if (v instanceof String) {
                    try { out.add(Long.parseLong((String) v)); } catch (NumberFormatException ignore) {}
                }
            }
        }
        return out;
    }

    // --- DTO ---
    @lombok.Data
    public static class SimulateRequest {
        private Long userId;
        private List<ModulePermSnapshot> modulePermissions;
    }

    @lombok.Data
    public static class ModulePermSnapshot {
        private String moduleCode;
        private String scopeCode;
        /** 可接受 [1,2,3] 或 [{scopeId:1},{scopeId:2}] 两种形式 */
        private List<Object> scopeItems;
    }

    /** 内部表元数据 */
    private static class ModuleMeta {
        final String table;
        final String nameCol;    // 样本名称列, 可能 null
        final String orgCol;     // 组织过滤列, 可能 null (如 roles)
        final boolean hasDeleted;
        final boolean hasCreatedBy;

        ModuleMeta(String table, String nameCol, String orgCol, boolean hasDeleted, boolean hasCreatedBy) {
            this.table = table;
            this.nameCol = nameCol;
            this.orgCol = orgCol;
            this.hasDeleted = hasDeleted;
            this.hasCreatedBy = hasCreatedBy;
        }
    }
}
