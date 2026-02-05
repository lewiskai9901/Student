package com.school.management.interfaces.rest.dashboard;

import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 仪表盘控制器
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "仪表盘", description = "仪表盘数据接口")
public class DashboardController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/statistics")
    @Operation(summary = "获取仪表盘统计数据")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(defaultValue = "7") int days) {
        log.debug("获取仪表盘统计数据, days={}", days);

        Map<String, Object> stats = new HashMap<>();

        // 获取学生总数 (student_status: 1=在读)
        Integer studentCount = getCount("SELECT COUNT(*) FROM students WHERE deleted = 0 AND student_status = 1");
        stats.put("studentCount", studentCount);

        // 获取班级总数 (使用classes表)
        Integer classCount = getCount("SELECT COUNT(*) FROM classes WHERE deleted = 0");
        stats.put("classCount", classCount);

        // 获取宿舍总数 (使用space表，room_type='DORMITORY')
        Integer dormitoryCount = getCount("SELECT COUNT(*) FROM space WHERE deleted = 0 AND room_type = 'DORMITORY'");
        stats.put("dormitoryCount", dormitoryCount);

        // 获取今日检查数
        Integer todayCheckCount = getCount(
            "SELECT COUNT(*) FROM check_records WHERE deleted = 0 AND DATE(check_time) = CURDATE()");
        stats.put("todayCheckCount", todayCheckCount);

        // 计算宿舍入住率 (使用space表的capacity和current_occupancy)
        Integer totalBeds = getCount("SELECT COALESCE(SUM(capacity), 0) FROM space WHERE deleted = 0 AND room_type = 'DORMITORY'");
        Integer occupiedBeds = getCount("SELECT COALESCE(SUM(current_occupancy), 0) FROM space WHERE deleted = 0 AND room_type = 'DORMITORY'");
        int occupancyRate = totalBeds > 0 ? Math.round(occupiedBeds * 100.0f / totalBeds) : 0;
        stats.put("occupancyRate", occupancyRate);

        // 获取今日完成/待处理检查数
        Integer completedChecks = getCount(
            "SELECT COUNT(*) FROM check_records WHERE deleted = 0 AND DATE(check_time) = CURDATE() AND status = 2");
        Integer pendingChecks = getCount(
            "SELECT COUNT(*) FROM check_records WHERE deleted = 0 AND DATE(check_time) = CURDATE() AND status IN (0, 1)");
        stats.put("completedChecks", completedChecks);
        stats.put("pendingChecks", pendingChecks);

        // 完成率
        int totalToday = completedChecks + pendingChecks;
        int completionRate = totalToday > 0 ? Math.round(completedChecks * 100.0f / totalToday) : 0;
        stats.put("completionRate", completionRate);

        // 图表数据 - 最近N天的检查平均分
        List<Map<String, Object>> chartData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd");
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Map<String, Object> point = new HashMap<>();
            point.put("date", date.format(formatter));
            // 随机示例分数，真实场景应从数据库查询
            point.put("score", 70 + (int)(Math.random() * 25));
            chartData.add(point);
        }
        stats.put("chartData", chartData);

        // 检查分类统计
        List<Map<String, Object>> checkCategories = new ArrayList<>();
        checkCategories.add(createCategory("卫生检查", 85, "#3b82f6"));
        checkCategories.add(createCategory("纪律检查", 92, "#10b981"));
        checkCategories.add(createCategory("安全检查", 78, "#f59e0b"));
        checkCategories.add(createCategory("考勤检查", 95, "#8b5cf6"));
        stats.put("checkCategories", checkCategories);

        // 最近检查记录
        List<Map<String, Object>> recentRecords = getRecentRecords();
        stats.put("recentRecords", recentRecords);

        return Result.success(stats);
    }

    private Integer getCount(String sql) {
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("执行统计SQL失败: {}, 错误: {}", sql, e.getMessage());
            return 0;
        }
    }

    private Map<String, Object> createCategory(String name, int value, String color) {
        Map<String, Object> category = new HashMap<>();
        category.put("name", name);
        category.put("value", value);
        category.put("color", color);
        return category;
    }

    private List<Map<String, Object>> getRecentRecords() {
        List<Map<String, Object>> records = new ArrayList<>();
        try {
            String sql = """
                SELECT cr.id, cr.check_name,
                       COALESCE(cp.plan_name, '日常检查') as type_name,
                       cr.total_score, cr.avg_score, cr.check_time as created_at
                FROM check_records cr
                LEFT JOIN check_plans cp ON cr.plan_id = cp.id
                WHERE cr.deleted = 0
                ORDER BY cr.check_time DESC
                LIMIT 5
                """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", row.get("id"));
                record.put("typeName", row.get("type_name") != null ? row.get("type_name") : "日常检查");
                record.put("targetName", row.get("check_name"));
                Object score = row.get("avg_score");
                int avgScore = score != null ? ((Number) score).intValue() : 0;
                record.put("totalScore", avgScore);
                record.put("scoreRate", avgScore);
                record.put("createdAt", row.get("created_at") != null ? row.get("created_at").toString() : "");
                records.add(record);
            }
        } catch (Exception e) {
            log.warn("获取最近检查记录失败: {}", e.getMessage());
        }
        return records;
    }
}
