package com.school.management.infrastructure.extension.plugins.education.application.calendar;

import com.school.management.common.util.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 作息表/节次配置应用服务 (M3.2.1, 2026-05-20).
 * PeriodConfigController 9 处直 jdbc 全部下沉到此.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PeriodConfigApplicationService {

    private static final String LIST_COLUMNS =
        "id, semester_id AS semesterId, config_name AS configName, " +
        "periods_per_day AS periodsPerDay, schedule_days AS scheduleDays, periods, " +
        "is_default AS isDefault, created_at AS createdAt";

    private static final String DETAIL_COLUMNS =
        "id, semester_id AS semesterId, config_name AS configName, " +
        "periods_per_day AS periodsPerDay, schedule_days AS scheduleDays, periods, " +
        "is_default AS isDefault";

    private final JdbcTemplate jdbc;

    public List<Map<String, Object>> listBySemester(Long semesterId) {
        return jdbc.queryForList(
            "SELECT " + LIST_COLUMNS +
            " FROM period_configs WHERE semester_id = ? AND deleted = 0 ORDER BY is_default DESC",
            semesterId);
    }

    /** 取一条作息配置; 不存在返 null. */
    public Map<String, Object> findById(Long id) {
        try {
            return jdbc.queryForMap(
                "SELECT " + DETAIL_COLUMNS + " FROM period_configs WHERE id = ? AND deleted = 0", id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Transactional
    public Long create(Map<String, Object> data) {
        Long semesterId = Long.valueOf(data.get("semesterId").toString());
        String configName = (String) data.getOrDefault("configName", "默认作息表");
        int periodsPerDay = data.get("periodsPerDay") != null
            ? ((Number) data.get("periodsPerDay")).intValue() : 8;
        String scheduleDays = toJsonString(data.get("scheduleDays"));
        String periods = toJsonString(data.get("periods"));

        jdbc.update(
            "INSERT INTO period_configs (semester_id, config_name, periods_per_day, schedule_days, periods, is_default, created_by, deleted) " +
            "VALUES (?, ?, ?, ?, ?, 1, ?, 0)",
            semesterId, configName, periodsPerDay, scheduleDays, periods,
            SecurityUtils.requireCurrentUserId());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Transactional
    public void update(Long id, Map<String, Object> data) {
        String configName = (String) data.getOrDefault("configName", "默认作息表");
        int periodsPerDay = data.get("periodsPerDay") != null
            ? ((Number) data.get("periodsPerDay")).intValue() : 8;
        String scheduleDays = toJsonString(data.get("scheduleDays"));
        String periods = toJsonString(data.get("periods"));

        jdbc.update(
            "UPDATE period_configs SET config_name=?, periods_per_day=?, schedule_days=?, periods=? " +
            "WHERE id=? AND deleted=0",
            configName, periodsPerDay, scheduleDays, periods, id);
    }

    @Transactional
    public void softDelete(Long id) {
        jdbc.update("UPDATE period_configs SET deleted=1 WHERE id=?", id);
    }

    /**
     * 学期初始化: 若已有配置直接跳过; 否则尝试从上一学期继承.
     * 返回 message 供 controller 包装.
     */
    @Transactional
    public String initFromPrevious(Long semesterId) {
        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM period_configs WHERE semester_id=? AND deleted=0",
            Long.class, semesterId);
        if (exists != null && exists > 0) {
            return "已有配置，跳过继承";
        }
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
            return "已从上一学期继承";
        } catch (Exception e) {
            return "无上一学期配置，请手动创建";
        }
    }

    private String toJsonString(Object obj) {
        if (obj == null) return "[]";
        if (obj instanceof String) return (String) obj;
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
