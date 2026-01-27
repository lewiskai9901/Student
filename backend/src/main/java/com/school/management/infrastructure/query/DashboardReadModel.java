package com.school.management.infrastructure.query;

import com.school.management.config.CacheConfig;
import com.school.management.infrastructure.cache.CacheService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * CQRS Read Model for Dashboard queries.
 *
 * <p>Optimized for read-heavy dashboard operations with:
 * <ul>
 *   <li>Denormalized query results</li>
 *   <li>Aggressive caching</li>
 *   <li>Pre-computed aggregations</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardReadModel {

    private final JdbcTemplate jdbcTemplate;
    private final CacheService cacheService;

    /**
     * Gets dashboard summary statistics.
     *
     * @return dashboard statistics
     */
    @Cacheable(value = CacheConfig.CACHE_STATISTICS, key = "'dashboard:summary'")
    public DashboardSummary getDashboardSummary() {
        log.debug("Loading dashboard summary from database");

        // Student count
        Long studentCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM student WHERE deleted = 0",
            Long.class
        );

        // Class count
        Long classCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM class WHERE deleted = 0",
            Long.class
        );

        // User count
        Long userCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user WHERE deleted = 0 AND status = 1",
            Long.class
        );

        // Today's check count
        Long todayCheckCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM daily_check WHERE DATE(check_date) = CURDATE() AND deleted = 0",
            Long.class
        );

        // Pending appeals count
        Long pendingAppeals = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM check_item_appeal WHERE status = 'PENDING' AND deleted = 0",
            Long.class
        );

        // Pending tasks count
        Long pendingTasks = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM task WHERE status IN ('PENDING_ACCEPT', 'IN_PROGRESS') AND deleted = 0",
            Long.class
        );

        return DashboardSummary.builder()
            .studentCount(studentCount != null ? studentCount : 0)
            .classCount(classCount != null ? classCount : 0)
            .userCount(userCount != null ? userCount : 0)
            .todayCheckCount(todayCheckCount != null ? todayCheckCount : 0)
            .pendingAppeals(pendingAppeals != null ? pendingAppeals : 0)
            .pendingTasks(pendingTasks != null ? pendingTasks : 0)
            .build();
    }

    /**
     * Gets class ranking for a specific date range.
     *
     * @param startDate start date
     * @param endDate   end date
     * @param limit     max results
     * @return list of class rankings
     */
    public List<ClassRanking> getClassRanking(LocalDate startDate, LocalDate endDate, int limit) {
        String cacheKey = String.format("ranking:%s:%s:%d", startDate, endDate, limit);

        return cacheService.getOrLoad(cacheKey, Duration.ofMinutes(10), () -> {
            log.debug("Loading class ranking from database: {} to {}", startDate, endDate);

            String sql = """
                SELECT
                    c.id as class_id,
                    c.name as class_name,
                    d.name as department_name,
                    COALESCE(AVG(dc.final_score), 0) as avg_score,
                    COUNT(dc.id) as check_count
                FROM class c
                LEFT JOIN department d ON c.org_unit_id = d.id
                LEFT JOIN daily_check dc ON dc.class_id = c.id
                    AND dc.check_date BETWEEN ? AND ?
                    AND dc.deleted = 0
                WHERE c.deleted = 0
                GROUP BY c.id, c.name, d.name
                ORDER BY avg_score DESC
                LIMIT ?
                """;

            return jdbcTemplate.query(sql,
                (rs, rowNum) -> ClassRanking.builder()
                    .classId(rs.getLong("class_id"))
                    .className(rs.getString("class_name"))
                    .orgUnitName(rs.getString("department_name"))
                    .averageScore(rs.getBigDecimal("avg_score"))
                    .checkCount(rs.getInt("check_count"))
                    .ranking(rowNum + 1)
                    .build(),
                startDate, endDate, limit
            );
        });
    }

    /**
     * Gets org unit statistics.
     *
     * @return list of org unit stats
     */
    @Cacheable(value = CacheConfig.CACHE_STATISTICS, key = "'dashboard:departments'")
    public List<OrgUnitStats> getOrgUnitStats() {
        log.debug("Loading org unit statistics from database");

        String sql = """
            SELECT
                d.id as org_unit_id,
                d.name as department_name,
                COUNT(DISTINCT c.id) as class_count,
                COUNT(DISTINCT s.id) as student_count,
                COUNT(DISTINCT u.id) as user_count
            FROM department d
            LEFT JOIN class c ON c.org_unit_id = d.id AND c.deleted = 0
            LEFT JOIN student s ON s.class_id = c.id AND s.deleted = 0
            LEFT JOIN user_department ud ON ud.org_unit_id = d.id
            LEFT JOIN user u ON u.id = ud.user_id AND u.deleted = 0 AND u.status = 1
            WHERE d.deleted = 0
            GROUP BY d.id, d.name
            ORDER BY d.name
            """;

        return jdbcTemplate.query(sql,
            (rs, rowNum) -> OrgUnitStats.builder()
                .orgUnitId(rs.getLong("org_unit_id"))
                .orgUnitName(rs.getString("department_name"))
                .classCount(rs.getInt("class_count"))
                .studentCount(rs.getInt("student_count"))
                .userCount(rs.getInt("user_count"))
                .build()
        );
    }

    /**
     * Gets recent activity feed.
     *
     * @param limit max results
     * @return list of recent activities
     */
    public List<RecentActivity> getRecentActivities(int limit) {
        String cacheKey = "activities:recent:" + limit;

        return cacheService.getOrLoad(cacheKey, Duration.ofMinutes(2), () -> {
            log.debug("Loading recent activities from database");

            String sql = """
                (SELECT
                    'CHECK' as activity_type,
                    CONCAT('检查记录: ', c.name) as description,
                    u.real_name as actor_name,
                    dc.created_at as activity_time
                FROM daily_check dc
                JOIN class c ON dc.class_id = c.id
                JOIN user u ON dc.inspector_id = u.id
                WHERE dc.deleted = 0
                ORDER BY dc.created_at DESC
                LIMIT ?)
                UNION ALL
                (SELECT
                    'APPEAL' as activity_type,
                    CONCAT('申诉提交: ', c.name) as description,
                    u.real_name as actor_name,
                    a.created_at as activity_time
                FROM check_item_appeal a
                JOIN daily_check dc ON a.daily_check_id = dc.id
                JOIN class c ON dc.class_id = c.id
                JOIN user u ON a.created_by = u.id
                WHERE a.deleted = 0
                ORDER BY a.created_at DESC
                LIMIT ?)
                UNION ALL
                (SELECT
                    'TASK' as activity_type,
                    CONCAT('任务创建: ', t.title) as description,
                    u.real_name as actor_name,
                    t.created_at as activity_time
                FROM task t
                JOIN user u ON t.created_by = u.id
                WHERE t.deleted = 0
                ORDER BY t.created_at DESC
                LIMIT ?)
                ORDER BY activity_time DESC
                LIMIT ?
                """;

            return jdbcTemplate.query(sql,
                (rs, rowNum) -> RecentActivity.builder()
                    .activityType(rs.getString("activity_type"))
                    .description(rs.getString("description"))
                    .actorName(rs.getString("actor_name"))
                    .activityTime(rs.getTimestamp("activity_time").toLocalDateTime())
                    .build(),
                limit, limit, limit, limit
            );
        });
    }

    /**
     * Invalidates all dashboard caches.
     */
    public void invalidateDashboardCache() {
        cacheService.evictByPattern("dashboard:*");
        cacheService.evictByPattern("ranking:*");
        cacheService.evictByPattern("activities:*");
    }

    // ========== DTOs ==========

    @Data
    @Builder
    public static class DashboardSummary {
        private long studentCount;
        private long classCount;
        private long userCount;
        private long todayCheckCount;
        private long pendingAppeals;
        private long pendingTasks;
    }

    @Data
    @Builder
    public static class ClassRanking {
        private Long classId;
        private String className;
        private String orgUnitName;
        private BigDecimal averageScore;
        private int checkCount;
        private int ranking;
    }

    @Data
    @Builder
    public static class OrgUnitStats {
        private Long orgUnitId;
        private String orgUnitName;
        private int classCount;
        private int studentCount;
        private int userCount;
    }

    @Data
    @Builder
    public static class RecentActivity {
        private String activityType;
        private String description;
        private String actorName;
        private java.time.LocalDateTime activityTime;
    }
}
