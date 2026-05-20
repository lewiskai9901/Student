package com.school.management.application.system;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Application service for audit trail queries.
 * Owns all data access to the {@code audit_trail} table.
 */
@Service
@RequiredArgsConstructor
public class AuditTrailApplicationService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Paginated query with optional filters.
     * Returns a map with {@code records} and {@code total}.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> list(String module,
                                    String action,
                                    String resourceType,
                                    Long operatorId,
                                    String startDate,
                                    String endDate,
                                    int page,
                                    int size) {

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
        return result;
    }

    /**
     * Get audit logs for a specific resource.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getByResource(String resourceType, String resourceId) {
        String sql = "SELECT id, module, action, resource_type AS resourceType, " +
                "resource_id AS resourceId, resource_name AS resourceName, " +
                "operator_id AS operatorId, operator_name AS operatorName, " +
                "ip_address AS ipAddress, description, created_at AS createdAt " +
                "FROM audit_trail WHERE resource_type = ? AND resource_id = ? " +
                "ORDER BY created_at DESC LIMIT 100";

        return jdbcTemplate.queryForList(sql, resourceType, resourceId);
    }
}
