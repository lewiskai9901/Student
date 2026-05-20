package com.school.management.application.system;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 客户端错误上报的数据访问应用服务.
 *
 * <p>承载 {@code ClientErrorController} 原先内联的所有 JdbcTemplate 逻辑.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientErrorApplicationService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 同 fingerprint 1 小时内累加 occurrence_count. 任意异常仅记 warn.
     */
    @Transactional
    public void mergeOccurrence(String fingerprint) {
        try {
            jdbcTemplate.update(
                    "UPDATE client_error_logs SET occurrence_count = occurrence_count + 1, last_occurred_at = NOW() " +
                    "WHERE fingerprint = ? AND last_occurred_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)",
                    fingerprint);
        } catch (Exception e) {
            log.warn("merge error log failed: {}", e.getMessage());
        }
    }

    /**
     * 插入一条客户端错误日志. 任意异常仅记 error, 不向上抛.
     */
    @Transactional
    public void insertErrorLog(long tenantId, Long userId, String level, String source,
                               String message, String stack, String url, String routePath,
                               String userAgent, String fingerprint) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO client_error_logs(tenant_id, user_id, level, source, message, stack, url, route_path, user_agent, fingerprint) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    tenantId,
                    userId,
                    level,
                    source,
                    message,
                    stack,
                    url,
                    routePath,
                    userAgent,
                    fingerprint);
        } catch (Exception e) {
            log.error("Failed to write client error log: {}", e.getMessage());
        }
    }

    /**
     * 查询近期错误日志, 可按 level 过滤. limit 上限 500.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findRecent(int limit, String level) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, level, source, message, url, route_path, user_id, fingerprint, " +
                "occurrence_count, first_occurred_at, last_occurred_at, resolved " +
                "FROM client_error_logs WHERE 1=1 ");
        Object[] args;
        if (level != null && !level.isBlank()) {
            sql.append("AND level = ? ");
            args = new Object[]{level, Math.min(limit, 500)};
        } else {
            args = new Object[]{Math.min(limit, 500)};
        }
        sql.append("ORDER BY last_occurred_at DESC LIMIT ?");
        return jdbcTemplate.queryForList(sql.toString(), args);
    }

    /**
     * 标记某条错误日志为已解决.
     */
    @Transactional
    public void markResolved(Long id, Long resolvedBy) {
        jdbcTemplate.update(
                "UPDATE client_error_logs SET resolved = 1, resolved_by = ?, resolved_at = NOW() WHERE id = ?",
                resolvedBy, id);
    }
}
