package com.school.management.infrastructure.analytics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * CQRS read model for inspection analytics.
 * Queries the database directly to generate analytics data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionAnalyticsReadModel {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get class ranking data for a date range.
     * Returns list of maps with classId, className, averageScore, rank.
     */
    public List<Map<String, Object>> getClassRanking(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT cr.class_id, cr.class_name, " +
                "AVG(cr.final_score) as avg_score, " +
                "COUNT(cr.id) as check_count " +
                "FROM inspection_class_records cr " +
                "JOIN inspection_sessions s ON cr.session_id = s.id " +
                "WHERE s.status = 'PUBLISHED' " +
                "AND s.inspection_date BETWEEN ? AND ? " +
                "AND cr.deleted = 0 AND s.deleted = 0 " +
                "GROUP BY cr.class_id, cr.class_name " +
                "ORDER BY avg_score DESC";
        try {
            return jdbcTemplate.queryForList(sql, startDate, endDate);
        } catch (Exception e) {
            log.warn("Failed to query class ranking: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get violation distribution data for a date range.
     * Returns list of maps with itemName, count, totalDeduction.
     */
    public List<Map<String, Object>> getViolationDistribution(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT dd.item_name, " +
                "COUNT(dd.id) as occurrence_count, " +
                "SUM(dd.deducted_score) as total_deduction " +
                "FROM inspection_deduction_details dd " +
                "JOIN inspection_class_records cr ON dd.class_record_id = cr.id " +
                "JOIN inspection_sessions s ON cr.session_id = s.id " +
                "WHERE s.status = 'PUBLISHED' " +
                "AND s.inspection_date BETWEEN ? AND ? " +
                "AND dd.deleted = 0 AND cr.deleted = 0 AND s.deleted = 0 " +
                "GROUP BY dd.item_name " +
                "ORDER BY occurrence_count DESC " +
                "LIMIT 20";
        try {
            return jdbcTemplate.queryForList(sql, startDate, endDate);
        } catch (Exception e) {
            log.warn("Failed to query violation distribution: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get inspector workload data for a date range.
     * Returns list of maps with inspectorId, inspectorName, sessionCount, classCount.
     */
    public List<Map<String, Object>> getInspectorWorkload(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT s.inspector_id, s.inspector_name, " +
                "COUNT(DISTINCT s.id) as session_count, " +
                "COUNT(DISTINCT cr.class_id) as class_count " +
                "FROM inspection_sessions s " +
                "LEFT JOIN inspection_class_records cr ON cr.session_id = s.id AND cr.deleted = 0 " +
                "WHERE s.status = 'PUBLISHED' " +
                "AND s.inspection_date BETWEEN ? AND ? " +
                "AND s.deleted = 0 " +
                "GROUP BY s.inspector_id, s.inspector_name " +
                "ORDER BY session_count DESC";
        try {
            return jdbcTemplate.queryForList(sql, startDate, endDate);
        } catch (Exception e) {
            log.warn("Failed to query inspector workload: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get department comparison data for a date range.
     * Returns list of maps with departmentId, departmentName, avgScore, classCount.
     */
    public List<Map<String, Object>> getDepartmentComparison(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT cr.department_id, cr.department_name, " +
                "AVG(cr.final_score) as avg_score, " +
                "COUNT(DISTINCT cr.class_id) as class_count, " +
                "COUNT(cr.id) as record_count " +
                "FROM inspection_class_records cr " +
                "JOIN inspection_sessions s ON cr.session_id = s.id " +
                "WHERE s.status = 'PUBLISHED' " +
                "AND s.inspection_date BETWEEN ? AND ? " +
                "AND cr.deleted = 0 AND s.deleted = 0 " +
                "GROUP BY cr.department_id, cr.department_name " +
                "ORDER BY avg_score DESC";
        try {
            return jdbcTemplate.queryForList(sql, startDate, endDate);
        } catch (Exception e) {
            log.warn("Failed to query department comparison: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
