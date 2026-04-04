package com.school.management.interfaces.rest.system;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Audit trail query API.
 * Read-only access to the audit_trail table.
 */
@RestController("systemAuditTrailController")
@RequestMapping("/audit-trail")
@RequiredArgsConstructor
public class AuditTrailController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Paginated query with optional filters.
     */
    @GetMapping
    @CasbinAccess(resource = "system:audit", action = "view")
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (module != null && !module.isBlank()) {
            where.append(" AND module = ?");
            params.add(module);
        }
        if (action != null && !action.isBlank()) {
            where.append(" AND action = ?");
            params.add(action);
        }
        if (resourceType != null && !resourceType.isBlank()) {
            where.append(" AND resource_type = ?");
            params.add(resourceType);
        }
        if (operatorId != null) {
            where.append(" AND operator_id = ?");
            params.add(operatorId);
        }
        if (startDate != null && !startDate.isBlank()) {
            where.append(" AND created_at >= ?");
            params.add(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            where.append(" AND created_at <= ?");
            params.add(endDate + " 23:59:59");
        }

        // Count
        String countSql = "SELECT COUNT(*) FROM audit_trail" + where;
        Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());

        // Data
        String dataSql = "SELECT id, module, action, resource_type AS resourceType, " +
                "resource_id AS resourceId, resource_name AS resourceName, " +
                "operator_id AS operatorId, operator_name AS operatorName, " +
                "ip_address AS ipAddress, description, created_at AS createdAt " +
                "FROM audit_trail" + where +
                " ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(size);
        dataParams.add((page - 1) * size);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(dataSql, dataParams.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total != null ? total : 0);
        return Result.success(result);
    }

    /**
     * Get audit logs for a specific resource.
     */
    @GetMapping("/by-resource")
    @CasbinAccess(resource = "system:audit", action = "view")
    public Result<List<Map<String, Object>>> getByResource(
            @RequestParam String resourceType,
            @RequestParam String resourceId) {

        String sql = "SELECT id, module, action, resource_type AS resourceType, " +
                "resource_id AS resourceId, resource_name AS resourceName, " +
                "operator_id AS operatorId, operator_name AS operatorName, " +
                "ip_address AS ipAddress, description, created_at AS createdAt " +
                "FROM audit_trail WHERE resource_type = ? AND resource_id = ? " +
                "ORDER BY created_at DESC LIMIT 100";

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, resourceType, resourceId);
        return Result.success(records);
    }
}
