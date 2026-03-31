package com.school.management.interfaces.rest.teaching;

import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 教务管理 REST Controller
 * 处理学年、学期（别名路由）、校历事件、教学周的 CRUD
 *
 * 使用 JdbcTemplate 直接操作已有的 DB 表:
 * - academic_years
 * - semesters (别名, 主路由在 SemesterController)
 * - academic_event
 * - academic_weeks
 */
@Slf4j
@RestController
@RequestMapping("/teaching")
@RequiredArgsConstructor
public class TeachingController {

    private final JdbcTemplate jdbc;

    // ==================== 学年管理 ====================

    @GetMapping("/academic-years")
    public Result<List<Map<String, Object>>> listAcademicYears() {
        List<Map<String, Object>> years = jdbc.queryForList(
            "SELECT id, year_code AS yearCode, year_name AS yearName, " +
            "start_date AS startDate, end_date AS endDate, " +
            "is_current AS isCurrent, status, created_at AS createdAt " +
            "FROM academic_years WHERE deleted = 0 ORDER BY start_date DESC"
        );
        return Result.success(years);
    }

    @GetMapping("/academic-years/{id}")
    public Result<Map<String, Object>> getAcademicYear(@PathVariable Long id) {
        Map<String, Object> year = jdbc.queryForMap(
            "SELECT id, year_code AS yearCode, year_name AS yearName, " +
            "start_date AS startDate, end_date AS endDate, " +
            "is_current AS isCurrent, status, created_at AS createdAt " +
            "FROM academic_years WHERE id = ? AND deleted = 0", id
        );
        return Result.success(year);
    }

    @GetMapping("/academic-years/current")
    public Result<Map<String, Object>> getCurrentAcademicYear() {
        try {
            Map<String, Object> year = jdbc.queryForMap(
                "SELECT id, year_code AS yearCode, year_name AS yearName, " +
                "start_date AS startDate, end_date AS endDate, " +
                "is_current AS isCurrent, status " +
                "FROM academic_years WHERE is_current = 1 AND deleted = 0 LIMIT 1"
            );
            return Result.success(year);
        } catch (Exception e) {
            return Result.success(null);
        }
    }

    @PostMapping("/academic-years")
    public Result<Map<String, Object>> createAcademicYear(@RequestBody Map<String, Object> data) {
        String yearName = (String) data.get("yearName");
        LocalDate startDate = LocalDate.parse((String) data.get("startDate"));
        LocalDate endDate = LocalDate.parse((String) data.get("endDate"));
        // Auto-generate yearCode if not provided: e.g., "2025-2026"
        String yearCode = data.get("yearCode") != null ? (String) data.get("yearCode")
            : startDate.getYear() + "-" + endDate.getYear();

        jdbc.update(
            "INSERT INTO academic_years (id, year_code, year_name, start_date, end_date, is_current, status, deleted) " +
            "VALUES (?, ?, ?, ?, ?, 0, 1, 0)",
            System.currentTimeMillis(), yearCode, yearName, startDate, endDate
        );
        return Result.success(data);
    }

    @PutMapping("/academic-years/{id}")
    public Result<Void> updateAcademicYear(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        String yearName = (String) data.get("yearName");
        String startDateStr = (String) data.get("startDate");
        String endDateStr = (String) data.get("endDate");

        jdbc.update(
            "UPDATE academic_years SET year_name = ?, start_date = ?, end_date = ? WHERE id = ? AND deleted = 0",
            yearName, LocalDate.parse(startDateStr), LocalDate.parse(endDateStr), id
        );
        return Result.success();
    }

    @DeleteMapping("/academic-years/{id}")
    public Result<Void> deleteAcademicYear(@PathVariable Long id) {
        jdbc.update("UPDATE academic_years SET deleted = 1 WHERE id = ?", id);
        return Result.success();
    }

    @PostMapping("/academic-years/{id}/set-current")
    public Result<Void> setCurrentAcademicYear(@PathVariable Long id) {
        jdbc.update("UPDATE academic_years SET is_current = 0 WHERE deleted = 0");
        jdbc.update("UPDATE academic_years SET is_current = 1 WHERE id = ? AND deleted = 0", id);
        return Result.success();
    }

    // ==================== 学期别名路由 ====================
    // 主路由在 SemesterController(/semesters)
    // 这里提供 /teaching/semesters 别名以匹配前端 API

    @GetMapping("/semesters")
    public Result<List<Map<String, Object>>> listSemesters(
            @RequestParam(required = false) Long yearId) {
        if (yearId != null) {
            try {
                Map<String, Object> year = jdbc.queryForMap(
                    "SELECT start_date, end_date FROM academic_years WHERE id = ? AND deleted = 0", yearId);
                return Result.success(jdbc.queryForList(
                    "SELECT id, semester_name AS semesterName, semester_code AS semesterCode, " +
                    "start_year AS startYear, semester_type AS semesterType, " +
                    "start_date AS startDate, end_date AS endDate, " +
                    "is_current AS isCurrent, status " +
                    "FROM semesters WHERE deleted = 0 AND start_date >= ? AND start_date <= ? ORDER BY start_date DESC",
                    year.get("start_date"), year.get("end_date")
                ));
            } catch (Exception e) {
                // yearId not found, return all
            }
        }
        return Result.success(jdbc.queryForList(
            "SELECT id, semester_name AS semesterName, semester_code AS semesterCode, " +
            "start_year AS startYear, semester_type AS semesterType, " +
            "start_date AS startDate, end_date AS endDate, " +
            "is_current AS isCurrent, status " +
            "FROM semesters WHERE deleted = 0 ORDER BY start_date DESC"
        ));
    }

    @GetMapping("/semesters/{id}")
    public Result<Map<String, Object>> getSemester(@PathVariable Long id) {
        Map<String, Object> sem = jdbc.queryForMap(
            "SELECT id, semester_name AS semesterName, semester_code AS semesterCode, " +
            "start_year AS startYear, semester_type AS semesterType, " +
            "start_date AS startDate, end_date AS endDate, " +
            "is_current AS isCurrent, status " +
            "FROM semesters WHERE id = ? AND deleted = 0", id
        );
        return Result.success(sem);
    }

    @GetMapping("/semesters/current")
    public Result<Map<String, Object>> getCurrentSemester() {
        try {
            Map<String, Object> sem = jdbc.queryForMap(
                "SELECT id, semester_name AS semesterName, semester_code AS semesterCode, " +
                "start_year AS startYear, semester_type AS semesterType, " +
                "start_date AS startDate, end_date AS endDate, " +
                "is_current AS isCurrent, status " +
                "FROM semesters WHERE is_current = 1 AND deleted = 0 LIMIT 1"
            );
            return Result.success(sem);
        } catch (Exception e) {
            return Result.success(null);
        }
    }

    @PostMapping("/semesters")
    public Result<Map<String, Object>> createSemester(@RequestBody Map<String, Object> data) {
        String semesterName = (String) data.get("semesterName");
        Integer semesterType = data.get("semesterType") != null ? ((Number) data.get("semesterType")).intValue() : 1;
        LocalDate startDate = LocalDate.parse((String) data.get("startDate"));
        LocalDate endDate = LocalDate.parse((String) data.get("endDate"));
        Integer startYear = startDate.getYear();
        // Auto-generate semester code: e.g., 2025-2026-1
        String semesterCode = data.get("semesterCode") != null ? (String) data.get("semesterCode")
            : startYear + "-" + (startYear + 1) + "-" + semesterType;

        jdbc.update(
            "INSERT INTO semesters (semester_name, semester_code, start_year, semester_type, " +
            "start_date, end_date, is_current, status, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, 0, 1, 0)",
            semesterName, semesterCode, startYear, semesterType, startDate, endDate
        );
        return Result.success(data);
    }

    @PutMapping("/semesters/{id}")
    public Result<Void> updateSemester(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        String semesterName = (String) data.get("semesterName");
        String startDateStr = (String) data.get("startDate");
        String endDateStr = (String) data.get("endDate");
        Integer semesterType = data.get("semesterType") != null ? ((Number) data.get("semesterType")).intValue() : null;

        LocalDate startDate = LocalDate.parse(startDateStr);
        StringBuilder sql = new StringBuilder("UPDATE semesters SET semester_name = ?, start_date = ?, end_date = ?, start_year = ?");
        List<Object> params = new java.util.ArrayList<>();
        params.add(semesterName);
        params.add(startDate);
        params.add(LocalDate.parse(endDateStr));
        params.add(startDate.getYear());
        if (semesterType != null) {
            sql.append(", semester_type = ?");
            params.add(semesterType);
        }
        sql.append(" WHERE id = ? AND deleted = 0");
        params.add(id);

        jdbc.update(sql.toString(), params.toArray());
        return Result.success();
    }

    @DeleteMapping("/semesters/{id}")
    public Result<Void> deleteSemester(@PathVariable Long id) {
        jdbc.update("UPDATE semesters SET deleted = 1 WHERE id = ?", id);
        return Result.success();
    }

    @PostMapping("/semesters/{id}/set-current")
    public Result<Void> setCurrentSemester(@PathVariable Long id) {
        jdbc.update("UPDATE semesters SET is_current = 0 WHERE deleted = 0");
        jdbc.update("UPDATE semesters SET is_current = 1 WHERE id = ? AND deleted = 0", id);
        return Result.success();
    }

    // ==================== 教学周 ====================

    @GetMapping("/semesters/{semesterId}/weeks")
    public Result<List<Map<String, Object>>> getWeeks(@PathVariable Long semesterId) {
        List<Map<String, Object>> weeks = jdbc.queryForList(
            "SELECT id, semester_id AS semesterId, week_number AS weekNumber, " +
            "week_name AS weekName, start_date AS startDate, end_date AS endDate, " +
            "is_current AS isCurrent, status " +
            "FROM academic_weeks WHERE semester_id = ? ORDER BY week_number",
            semesterId
        );
        return Result.success(weeks);
    }

    @PostMapping("/semesters/{semesterId}/generate-weeks")
    public Result<List<Map<String, Object>>> generateWeeks(@PathVariable Long semesterId) {
        // Get semester dates
        Map<String, Object> sem = jdbc.queryForMap(
            "SELECT start_date, end_date FROM semesters WHERE id = ? AND deleted = 0", semesterId);
        Object startObj = sem.get("start_date");
        Object endObj = sem.get("end_date");
        LocalDate start = startObj instanceof LocalDate ? (LocalDate) startObj : ((java.sql.Date) startObj).toLocalDate();
        LocalDate end = endObj instanceof LocalDate ? (LocalDate) endObj : ((java.sql.Date) endObj).toLocalDate();

        // Delete existing weeks
        jdbc.update("DELETE FROM academic_weeks WHERE semester_id = ?", semesterId);

        // Generate weeks
        int weekNum = 1;
        LocalDate weekStart = start;
        while (weekStart.isBefore(end)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(end)) weekEnd = end;
            jdbc.update(
                "INSERT INTO academic_weeks (semester_id, week_number, week_name, start_date, end_date, is_current, status) " +
                "VALUES (?, ?, ?, ?, ?, 0, 1)",
                semesterId, weekNum, "第" + weekNum + "周", weekStart, weekEnd
            );
            weekStart = weekEnd.plusDays(1);
            weekNum++;
        }

        return getWeeks(semesterId);
    }

    // ==================== 校历事件 ====================

    @GetMapping("/events")
    public Result<List<Map<String, Object>>> listEvents(
            @RequestParam(required = false) Long yearId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer eventType) {
        StringBuilder sql = new StringBuilder(
            "SELECT id, year_id AS yearId, semester_id AS semesterId, " +
            "event_name AS eventName, event_type AS eventType, " +
            "start_date AS startDate, end_date AS endDate, " +
            "all_day AS allDay, description " +
            "FROM academic_event WHERE deleted = 0"
        );
        List<Object> params = new java.util.ArrayList<>();
        if (yearId != null) {
            sql.append(" AND year_id = ?");
            params.add(yearId);
        }
        if (semesterId != null) {
            sql.append(" AND semester_id = ?");
            params.add(semesterId);
        }
        if (eventType != null) {
            sql.append(" AND event_type = ?");
            params.add(eventType);
        }
        sql.append(" ORDER BY start_date");

        return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
    }

    @GetMapping("/events/{id}")
    public Result<Map<String, Object>> getEvent(@PathVariable Long id) {
        Map<String, Object> event = jdbc.queryForMap(
            "SELECT id, year_id AS yearId, semester_id AS semesterId, " +
            "event_name AS eventName, event_type AS eventType, " +
            "start_date AS startDate, end_date AS endDate, " +
            "all_day AS allDay, description " +
            "FROM academic_event WHERE id = ? AND deleted = 0", id
        );
        return Result.success(event);
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.valueOf(val.toString());
    }
    private Integer toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.valueOf(val.toString());
    }

    @PostMapping("/events")
    public Result<Map<String, Object>> createEvent(@RequestBody Map<String, Object> data) {
        Long yearId = toLong(data.get("yearId"));
        Long semesterId = toLong(data.get("semesterId"));
        String eventName = (String) data.get("eventName");
        Integer eventType = toInt(data.get("eventType"), 5);
        LocalDate startDate = LocalDate.parse((String) data.get("startDate"));
        String endDateStr = (String) data.get("endDate");
        LocalDate endDate = endDateStr != null ? LocalDate.parse(endDateStr) : null;
        Boolean allDay = data.get("allDay") != null ? (Boolean) data.get("allDay") : true;
        String description = (String) data.get("description");

        jdbc.update(
            "INSERT INTO academic_event (year_id, semester_id, event_name, event_type, " +
            "start_date, end_date, all_day, description, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)",
            yearId, semesterId, eventName, eventType, startDate, endDate, allDay ? 1 : 0, description
        );
        return Result.success(data);
    }

    @PutMapping("/events/{id}")
    public Result<Void> updateEvent(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        String eventName = (String) data.get("eventName");
        Integer eventType = data.get("eventType") != null ? toInt(data.get("eventType"), 5) : null;
        LocalDate startDate = LocalDate.parse((String) data.get("startDate"));
        String endDateStr = (String) data.get("endDate");
        LocalDate endDate = endDateStr != null ? LocalDate.parse(endDateStr) : null;
        String description = (String) data.get("description");

        jdbc.update(
            "UPDATE academic_event SET event_name = ?, event_type = ?, " +
            "start_date = ?, end_date = ?, description = ? WHERE id = ? AND deleted = 0",
            eventName, eventType, startDate, endDate, description, id
        );
        return Result.success();
    }

    @DeleteMapping("/events/{id}")
    public Result<Void> deleteEvent(@PathVariable Long id) {
        jdbc.update("UPDATE academic_event SET deleted = 1 WHERE id = ?", id);
        return Result.success();
    }
}
