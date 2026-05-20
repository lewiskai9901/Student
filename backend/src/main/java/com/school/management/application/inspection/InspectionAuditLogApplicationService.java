package com.school.management.application.inspection;

import com.school.management.infrastructure.inspection.InspectionScopeHelper;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 检查平台审计日志查询数据访问 — 从 InspectionAuditLogController 抽离的 jdbc 持久化逻辑.
 */
@Service
@RequiredArgsConstructor
public class InspectionAuditLogApplicationService {

    private final JdbcTemplate jdbcTemplate;
    private final InspectionScopeHelper scopeHelper;

    /** 查询某个聚合的审计历史. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByEntity(String entityType, Long entityId, int limit) {
        // I4: 加 tenant 隔离 + org_unit_id scope (允许 NULL 兼容历史无 org 数据)
        Long tenantId = TenantContextHolder.getTenantId();
        String scopeClause = orgScopeClauseAllowNull();
        return jdbcTemplate.queryForList(
                "SELECT id, entity_type, entity_id, entity_code, action, " +
                "       actor_user_id, actor_user_name, org_unit_id, reason, payload, occurred_at " +
                "FROM inspection_audit_logs " +
                "WHERE entity_type = ? AND entity_id = ? AND tenant_id = ?" + scopeClause +
                " ORDER BY occurred_at DESC LIMIT ?",
                entityType, entityId, tenantId, limit);
    }

    /** 按动作类型 + 时间窗口查询 (用于审计报表). I4: 按 org_unit_id scope 收窄. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listByAction(String action, int limit) {
        Long tenantId = TenantContextHolder.getTenantId();
        String scopeClause = orgScopeClauseAllowNull();
        return jdbcTemplate.queryForList(
                "SELECT id, entity_type, entity_id, entity_code, action, " +
                "       actor_user_id, actor_user_name, org_unit_id, reason, payload, occurred_at " +
                "FROM inspection_audit_logs " +
                "WHERE action = ? AND tenant_id = ?" + scopeClause +
                " ORDER BY occurred_at DESC LIMIT ?",
                action, tenantId, limit);
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
