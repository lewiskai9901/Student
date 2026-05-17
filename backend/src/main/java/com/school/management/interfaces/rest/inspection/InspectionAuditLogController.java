package com.school.management.interfaces.rest.inspection;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.access.UserContextHolder;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.inspection.InspectionScopeHelper;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 检查平台审计日志查询 (review #13).
 *
 * <p>读取 inspection_audit_logs 表 — 让法务/纠纷场景能溯源高敏感动作.
 * 写路径在各 ApplicationService 通过 InspectionAuditLogger.
 */
@RestController
@RequestMapping("/inspection/audit-logs")
@RequiredArgsConstructor
public class InspectionAuditLogController {

    private final JdbcTemplate jdbcTemplate;
    private final InspectionScopeHelper scopeHelper;

    /**
     * 查询某个聚合的审计历史.
     *
     * @param entityType InspAppeal / CorrectiveCase / InspTask / InspProject
     * @param entityId   聚合 ID
     * @param limit      最多返回条数 (默认 50)
     */
    @GetMapping
    @CasbinAccess(resource = "insp:audit", action = "view")
    public Result<List<Map<String, Object>>> listByEntity(@RequestParam String entityType,
                                                           @RequestParam Long entityId,
                                                           @RequestParam(defaultValue = "50") int limit) {
        if (limit > 200) limit = 200;
        // I4: 加 tenant 隔离 + org_unit_id scope (允许 NULL 兼容历史无 org 数据)
        Long tenantId = TenantContextHolder.getTenantId();
        String scopeClause = orgScopeClauseAllowNull();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, entity_type, entity_id, entity_code, action, " +
                "       actor_user_id, actor_user_name, org_unit_id, reason, payload, occurred_at " +
                "FROM inspection_audit_logs " +
                "WHERE entity_type = ? AND entity_id = ? AND tenant_id = ?" + scopeClause +
                " ORDER BY occurred_at DESC LIMIT ?",
                entityType, entityId, tenantId, limit);
        return Result.success(rows);
    }

    /** 按动作类型 + 时间窗口查询 (用于审计报表). I4: 按 org_unit_id scope 收窄. */
    @GetMapping("/by-action")
    @CasbinAccess(resource = "insp:audit", action = "view")
    public Result<List<Map<String, Object>>> listByAction(@RequestParam String action,
                                                           @RequestParam(defaultValue = "100") int limit) {
        if (limit > 500) limit = 500;
        Long tenantId = TenantContextHolder.getTenantId();
        String scopeClause = orgScopeClauseAllowNull();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, entity_type, entity_id, entity_code, action, " +
                "       actor_user_id, actor_user_name, org_unit_id, reason, payload, occurred_at " +
                "FROM inspection_audit_logs " +
                "WHERE action = ? AND tenant_id = ?" + scopeClause +
                " ORDER BY occurred_at DESC LIMIT ?",
                action, tenantId, limit);
        return Result.success(rows);
    }

    /**
     * Audit log 专用 scope 片段 — 允许 org_unit_id IS NULL 通过 (历史无 org 兼容).
     * 不受限时返回 ""; 拒绝时返回 " AND 1=0".
     */
    private String orgScopeClauseAllowNull() {
        var ids = scopeHelper.allowedOrgIds();
        if (ids == null) return "";
        if (ids.isEmpty()) return " AND 1=0";
        String csv = ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        return " AND (org_unit_id IS NULL OR org_unit_id IN (" + csv + "))";
    }
}
