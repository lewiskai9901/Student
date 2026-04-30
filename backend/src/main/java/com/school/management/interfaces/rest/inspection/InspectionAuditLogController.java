package com.school.management.interfaces.rest.inspection;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
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
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, entity_type, entity_id, entity_code, action, " +
                "       actor_user_id, actor_user_name, reason, payload, occurred_at " +
                "FROM inspection_audit_logs " +
                "WHERE entity_type = ? AND entity_id = ? " +
                "ORDER BY occurred_at DESC LIMIT ?",
                entityType, entityId, limit);
        return Result.success(rows);
    }

    /** 按动作类型 + 时间窗口查询 (用于审计报表) */
    @GetMapping("/by-action")
    @CasbinAccess(resource = "insp:audit", action = "view")
    public Result<List<Map<String, Object>>> listByAction(@RequestParam String action,
                                                           @RequestParam(defaultValue = "100") int limit) {
        if (limit > 500) limit = 500;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, entity_type, entity_id, entity_code, action, " +
                "       actor_user_id, actor_user_name, reason, payload, occurred_at " +
                "FROM inspection_audit_logs " +
                "WHERE action = ? " +
                "ORDER BY occurred_at DESC LIMIT ?",
                action, limit);
        return Result.success(rows);
    }
}
