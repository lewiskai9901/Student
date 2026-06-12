package com.school.management.interfaces.rest.access;

import com.school.management.application.access.DataPermissionSimulateApplicationService;
import com.school.management.application.access.SimulateModuleMetaContributor;
import com.school.management.application.access.SimulateModuleMetaContributor.SimulateModuleMeta;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.extern.slf4j.Slf4j;
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
 *   - 通用模块 (user/org_unit/role/place) 核心内置; 行业模块 (如教育 student/school_class)
 *     由行业插件经 {@link SimulateModuleMetaContributor} 登记, 插件禁用即优雅降级
 *   - 支持的 scope: ALL / SELF / DEPARTMENT / DEPARTMENT_AND_BELOW / CUSTOM
 *   - 其他维度 (BY_CLASS/BY_GRADE/BY_WARD 等插件 scope) 返回 "暂未支持模拟"
 *   - 无 created_by 的表 SELF scope 降级返 "未支持"
 */
@RestController
@RequestMapping("/access/data-permissions/simulate")
@Slf4j
public class DataPermissionSimulateController {

    private final DataPermissionSimulateApplicationService simulateService;

    /** moduleCode → 表元数据: 核心通用模块 + 插件贡献 (后者可覆盖, 以插件口径为准)。 */
    private final Map<String, SimulateModuleMeta> moduleMetas;

    public DataPermissionSimulateController(
            DataPermissionSimulateApplicationService simulateService,
            List<SimulateModuleMetaContributor> contributors) {
        this.simulateService = simulateService;
        Map<String, SimulateModuleMeta> metas = new LinkedHashMap<>();
        // 通用核心模块 — 不含任何行业表
        // user 归属来自 access_relations member 关系 (primary_org_unit_id 已删):
        // orgCol = 主键 id, membership → buildWhere 用 member 子查询过滤.
        metas.put("user", SimulateModuleMeta.membership("users", "real_name", "id", true, true));
        metas.put("system_user", metas.get("user"));
        metas.put("org_unit", SimulateModuleMeta.orgColumn("org_units", "unit_name", "id", true, true));
        metas.put("role", SimulateModuleMeta.orgColumn("roles", "role_name", null, true, false));
        metas.put("system_role", metas.get("role"));
        metas.put("place", SimulateModuleMeta.orgColumn("places", "place_name", "org_unit_id", true, true));
        // 行业模块 (如教育 student/school_class) 由插件贡献
        for (SimulateModuleMetaContributor c : contributors) {
            metas.putAll(c.contribute());
        }
        this.moduleMetas = metas;
    }

    @PostMapping
    @CasbinAccess(resource = "admin", action = "access")
    public Result<Map<String, Object>> simulate(@RequestBody SimulateRequest req) {
        if (req.getUserId() == null) {
            return Result.success(Map.of("error", "userId 必填"));
        }

        // 查模拟用户的归属 org (DEPT / DEPT_AND_BELOW 需要) — 走 member 关系
        Long userOrgId = simulateService.findUserOrgUnitId(req.getUserId());

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

        SimulateModuleMeta meta = resolveMeta(code);
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

            Long count = simulateService.countByWhere(meta.table(), whereClause);
            result.put("accessibleCount", count != null ? count : 0);

            List<Map<String, Object>> samples =
                    simulateService.sampleRows(meta.table(), meta.nameCol(), whereClause);
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

    /** 模块 → 表元数据 (核心内置 + 插件贡献). 未登记的模块返 null → "此模块未实现模拟". */
    private SimulateModuleMeta resolveMeta(String moduleCode) {
        return moduleCode == null ? null : moduleMetas.get(moduleCode);
    }

    /** 构造 SQL WHERE. 返 null = 暂未支持. */
    private String buildWhere(String scope, Long userId, Long userOrgId,
                               ModulePermSnapshot mp, SimulateModuleMeta meta) {
        if (scope == null) return null;
        String delFilter = meta.hasDeleted() ? " AND deleted = 0" : "";

        switch (scope) {
            case "ALL":
                return "1=1" + delFilter;

            case "SELF":
                if (!meta.hasCreatedBy()) return null;
                return "created_by = " + userId + delFilter;

            case "DEPARTMENT":
                if (meta.orgCol() == null || userOrgId == null) return null;
                if (meta.membershipBased()) {
                    return meta.orgCol() + " IN (" + memberSubquery(String.valueOf(userOrgId)) + ")" + delFilter;
                }
                return meta.orgCol() + " = " + userOrgId + delFilter;

            case "DEPARTMENT_AND_BELOW":
                if (meta.orgCol() == null || userOrgId == null) return null;
                // tree_path 本身已包含当前节点 id (且以 '/' 结尾), 子节点的 tree_path 以父的 tree_path 为前缀.
                // 所以子树 = tree_path LIKE '<parent_tree_path>%'
                String subtreeOrgIds =
                        "SELECT id FROM org_units WHERE tree_path LIKE CONCAT(" +
                        "(SELECT IFNULL(tree_path,'') FROM org_units WHERE id = " + userOrgId + "), '%')";
                if (meta.membershipBased()) {
                    return meta.orgCol() + " IN (" + memberSubquery("(" + subtreeOrgIds + ")") + ")" + delFilter;
                }
                return meta.orgCol() + " IN (" + subtreeOrgIds + ")" + delFilter;

            case "CUSTOM":
                List<Long> ids = extractScopeIds(mp.getScopeItems());
                if (ids == null || ids.isEmpty()) return "1=0";  // 空自定义 = 空集
                if (meta.orgCol() == null) return null;
                String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
                if (meta.membershipBased()) {
                    return meta.orgCol() + " IN (" + memberSubquery("(" + joined + ")") + ")" + delFilter;
                }
                return meta.orgCol() + " IN (" + joined + ")" + delFilter;

            default:
                // BY_CLASS / BY_GRADE / BY_MAJOR / BY_WARD 等插件维度
                return null;
        }
    }

    /**
     * member 关系子查询: 返回归属到 {@code orgExpr} (单 org id 或 "(SELECT id ...)" 子查询) 的 user 主键集合.
     * 与 @DataPermission(resourceType="user") 的 member 子查询口径对齐.
     */
    private String memberSubquery(String orgExpr) {
        return "SELECT ar.subject_id FROM access_relations ar " +
               "WHERE ar.relation = 'member' AND ar.resource_type = 'org_unit' " +
               "AND ar.subject_type = 'user' AND ar.deleted = 0 " +
               "AND (ar.valid_to IS NULL OR ar.valid_to > NOW()) " +
               "AND ar.resource_id " + (orgExpr.startsWith("(") ? "IN " + orgExpr : "= " + orgExpr);
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

}
