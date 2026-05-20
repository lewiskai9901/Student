package com.school.management.application.access;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 关系变更历史查询的数据访问应用服务.
 *
 * <p>从 {@code AccessRelationHistoryController} 抽离 JdbcTemplate 访问.
 */
@Service
@RequiredArgsConstructor
public class AccessRelationHistoryApplicationService {

    private final JdbcTemplate jdbcTemplate;

    /** 查某 subject (人) 的所有关系变更. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findBySubject(String subjectType, Long subjectId, int limit) {
        return jdbcTemplate.queryForList(
            "SELECT id, original_id, resource_type, resource_id, relation, " +
            "  subject_type, subject_id, access_level, valid_from, valid_to, " +
            "  archived_at, archived_reason, archived_by, operator_ip, operation " +
            "FROM access_relations_history " +
            "WHERE subject_type=? AND subject_id=? " +
            "ORDER BY archived_at DESC LIMIT ?",
            subjectType, subjectId, limit);
    }

    /** 查某 resource (东西) 的所有关系变更. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findByResource(String resourceType, Long resourceId, int limit) {
        return jdbcTemplate.queryForList(
            "SELECT id, original_id, resource_type, resource_id, relation, " +
            "  subject_type, subject_id, access_level, valid_from, valid_to, " +
            "  archived_at, archived_reason, archived_by, operator_ip, operation " +
            "FROM access_relations_history " +
            "WHERE resource_type=? AND resource_id=? " +
            "ORDER BY archived_at DESC LIMIT ?",
            resourceType, resourceId, limit);
    }

    /** 最近 N 天变更 (审计页用). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> findRecent(int days, int limit) {
        return jdbcTemplate.queryForList(
            "SELECT id, resource_type, resource_id, relation, " +
            "  subject_type, subject_id, archived_at, archived_reason, archived_by, operation " +
            "FROM access_relations_history " +
            "WHERE archived_at >= NOW() - INTERVAL ? DAY " +
            "ORDER BY archived_at DESC LIMIT ?",
            days, limit);
    }
}
