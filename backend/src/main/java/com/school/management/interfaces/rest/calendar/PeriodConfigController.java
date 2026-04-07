package com.school.management.interfaces.rest.calendar;

import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 作息表/节次配置 Controller
 * 每个学期可独立配置上几节课、每节什么时间
 */
@Slf4j
@RestController
@RequestMapping("/calendar/period-configs")
@RequiredArgsConstructor
public class PeriodConfigController {

    private final JdbcTemplate jdbc;

    @GetMapping
    @CasbinAccess(resource = "calendar", action = "view")
    public Result<List<Map<String, Object>>> list(@RequestParam Long semesterId) {
        List<Map<String, Object>> list = jdbc.queryForList(
            "SELECT id, semester_id AS semesterId, config_name AS configName, " +
            "periods_per_day AS periodsPerDay, schedule_days AS scheduleDays, periods, " +
            "is_default AS isDefault, created_at AS createdAt " +
            "FROM period_configs WHERE semester_id = ? AND deleted = 0 ORDER BY is_default DESC",
            semesterId);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "calendar", action = "view")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> row = jdbc.queryForMap(
            "SELECT id, semester_id AS semesterId, config_name AS configName, " +
            "periods_per_day AS periodsPerDay, schedule_days AS scheduleDays, periods, " +
            "is_default AS isDefault " +
            "FROM period_configs WHERE id = ? AND deleted = 0", id);
        return Result.success(row);
    }

    @PostMapping
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> data) {
        Long semesterId = ((Number) data.get("semesterId")).longValue();
        String configName = (String) data.getOrDefault("configName", "默认作息表");
        int periodsPerDay = data.get("periodsPerDay") != null ? ((Number) data.get("periodsPerDay")).intValue() : 8;
        String scheduleDays = toJsonString(data.get("scheduleDays"));
        String periods = toJsonString(data.get("periods"));

        jdbc.update(
            "INSERT INTO period_configs (semester_id, config_name, periods_per_day, schedule_days, periods, is_default, created_by, deleted) " +
            "VALUES (?, ?, ?, ?, ?, 1, ?, 0)",
            semesterId, configName, periodsPerDay, scheduleDays, periods, SecurityUtils.requireCurrentUserId());
        Long newId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return Result.success(Map.of("id", newId));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        String configName = (String) data.getOrDefault("configName", "默认作息表");
        int periodsPerDay = data.get("periodsPerDay") != null ? ((Number) data.get("periodsPerDay")).intValue() : 8;
        String scheduleDays = toJsonString(data.get("scheduleDays"));
        String periods = toJsonString(data.get("periods"));

        jdbc.update(
            "UPDATE period_configs SET config_name=?, periods_per_day=?, schedule_days=?, periods=? WHERE id=? AND deleted=0",
            configName, periodsPerDay, scheduleDays, periods, id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Void> delete(@PathVariable Long id) {
        jdbc.update("UPDATE period_configs SET deleted=1 WHERE id=?", id);
        return Result.success();
    }

    @PostMapping("/init-from-previous")
    @CasbinAccess(resource = "calendar", action = "edit")
    public Result<Map<String, Object>> initFromPrevious(@RequestBody Map<String, Object> data) {
        Long semesterId = ((Number) data.get("semesterId")).longValue();
        // Check if already has config
        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM period_configs WHERE semester_id=? AND deleted=0", Long.class, semesterId);
        if (exists != null && exists > 0) {
            return Result.success(Map.of("message", "已有配置，跳过继承"));
        }
        // Find previous semester's config
        try {
            Map<String, Object> prev = jdbc.queryForMap(
                "SELECT periods_per_day, schedule_days, periods FROM period_configs pc " +
                "JOIN semesters s ON s.id = pc.semester_id " +
                "WHERE pc.deleted=0 AND s.deleted=0 AND s.id != ? " +
                "ORDER BY s.start_date DESC LIMIT 1", semesterId);
            jdbc.update(
                "INSERT INTO period_configs (semester_id, config_name, periods_per_day, schedule_days, periods, is_default, created_by, deleted) " +
                "VALUES (?, '默认作息表', ?, ?, ?, 1, ?, 0)",
                semesterId, prev.get("periods_per_day"), prev.get("schedule_days"), prev.get("periods"),
                SecurityUtils.requireCurrentUserId());
            return Result.success(Map.of("message", "已从上一学期继承"));
        } catch (Exception e) {
            return Result.success(Map.of("message", "无上一学期配置，请手动创建"));
        }
    }

    private String toJsonString(Object obj) {
        if (obj == null) return "[]";
        if (obj instanceof String) return (String) obj;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
