package com.school.management.interfaces.rest.teaching;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.application.teaching.ScheduleExportService;
import com.school.management.common.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.application.event.TriggerService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 排课与调课管理 REST Controller
 * 处理课表条目和调课申请的 CRUD
 *
 * 使用 JdbcTemplate 直接操作已有的 DB 表:
 * - schedule_entries
 * - schedule_adjustments
 */
@Slf4j
@RestController
@RequestMapping("/teaching")
@RequiredArgsConstructor
public class TeachingScheduleController {

    private final JdbcTemplate jdbc;

    @Autowired(required = false)
    private com.school.management.application.teaching.AutoSchedulingService autoSchedulingService;

    @Autowired
    private ScheduleExportService exportService;

    @Autowired(required = false)
    private TriggerService triggerService;

    @Autowired(required = false)
    private com.school.management.application.teaching.InstanceGenerationService instanceService;

    // ==================== 实况课表 ====================

    @GetMapping("/instances")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<Map<String, Object>>> listInstances(
            @RequestParam Long semesterId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Long classroomId) {

        StringBuilder sql = new StringBuilder(
            "SELECT si.id, si.entry_id AS entryId, si.actual_date AS actualDate, si.weekday, " +
            "si.week_number AS weekNumber, si.start_slot AS startSlot, si.end_slot AS endSlot, " +
            "si.status, si.cancel_reason AS cancelReason, si.source_type AS sourceType, " +
            "si.actual_hours AS actualHours, si.teacher_id AS teacherId, " +
            "si.original_teacher_id AS originalTeacherId, si.classroom_id AS classroomId, " +
            "c.course_name AS courseName, c.course_code AS courseCode, " +
            "ou.unit_name AS className, " +
            "COALESCE(p.place_code, p.place_name, '') AS classroomName, " +
            "u.real_name AS teacherName " +
            "FROM schedule_instances si " +
            "LEFT JOIN courses c ON c.id = si.course_id " +
            "LEFT JOIN org_units ou ON ou.id = si.org_unit_id " +
            "LEFT JOIN places p ON p.id = si.classroom_id " +
            "LEFT JOIN users u ON u.id = si.teacher_id " +
            "WHERE si.semester_id = ? AND si.deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        params.add(semesterId);

        if (date != null) { sql.append(" AND si.actual_date = ?"); params.add(date); }
        if (weekNumber != null) { sql.append(" AND si.week_number = ?"); params.add(weekNumber); }
        if (teacherId != null) { sql.append(" AND si.teacher_id = ?"); params.add(teacherId); }
        if (orgUnitId != null) { sql.append(" AND si.org_unit_id = ?"); params.add(orgUnitId); }
        if (classroomId != null) { sql.append(" AND si.classroom_id = ?"); params.add(classroomId); }
        sql.append(" ORDER BY si.actual_date, si.start_slot");

        return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
    }

    @PostMapping("/instances/generate")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> generateInstances(@RequestBody Map<String, Object> data) {
        if (instanceService == null) return Result.error("服务未启用");
        Long semesterId = Long.valueOf(data.get("semesterId").toString());
        Map<String, Object> result = instanceService.generateInstances(semesterId);
        return Result.success(result);
    }

    @PostMapping("/instances/apply-event")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> applyEvent(@RequestBody Map<String, Object> data) {
        if (instanceService == null) return Result.error("服务未启用");
        Long eventId = ((Number) data.get("eventId")).longValue();
        int affected = instanceService.applyCalendarEvent(eventId);
        return Result.success(Map.of("affected", affected));
    }

    // ==================== 课时统计 ====================

    @GetMapping("/statistics/hours")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> hoursStatistics(
            @RequestParam Long semesterId,
            @RequestParam String groupBy,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) Integer month) {

        String groupCol, nameJoin, nameCol;
        switch (groupBy) {
            case "teacher":
                groupCol = "si.teacher_id"; nameCol = "COALESCE(u.real_name, u.username, CONCAT('教师',si.teacher_id))";
                nameJoin = "LEFT JOIN users u ON u.id = si.teacher_id"; break;
            case "class":
                groupCol = "si.org_unit_id"; nameCol = "cl.class_name";
                nameJoin = "LEFT JOIN classes cl ON cl.id = si.org_unit_id"; break;
            case "course":
                groupCol = "si.course_id"; nameCol = "c.course_name";
                nameJoin = "LEFT JOIN courses c ON c.id = si.course_id"; break;
            case "classroom":
                groupCol = "si.classroom_id"; nameCol = "COALESCE(p.place_name, CONCAT('教室',si.classroom_id))";
                nameJoin = "LEFT JOIN places p ON p.id = si.classroom_id"; break;
            default:
                return Result.error("无效的 groupBy 参数");
        }

        StringBuilder where = new StringBuilder("si.semester_id = ? AND si.deleted = 0");
        List<Object> params = new ArrayList<>();
        params.add(semesterId);

        if ("week".equals(period) && weekNumber != null) {
            where.append(" AND si.week_number = ?"); params.add(weekNumber);
        }
        if ("month".equals(period) && month != null) {
            where.append(" AND MONTH(si.actual_date) = ?"); params.add(month);
        }

        String sql = String.format(
            "SELECT %s AS groupId, %s AS name, " +
            "COUNT(*) AS totalInstances, " +
            "SUM(CASE WHEN si.status IN (0,3,4) THEN si.actual_hours ELSE 0 END) AS actualHours, " +
            "SUM(CASE WHEN si.status = 0 THEN si.actual_hours ELSE 0 END) AS normalHours, " +
            "SUM(CASE WHEN si.status = 1 THEN si.actual_hours ELSE 0 END) AS cancelledHours, " +
            "SUM(CASE WHEN si.status = 3 THEN si.actual_hours ELSE 0 END) AS substituteHours, " +
            "SUM(CASE WHEN si.status = 4 THEN si.actual_hours ELSE 0 END) AS proxyHours, " +
            "SUM(si.actual_hours) AS totalHours " +
            "FROM schedule_instances si %s WHERE %s AND %s IS NOT NULL GROUP BY %s ORDER BY actualHours DESC",
            groupCol, nameCol, nameJoin, where, groupCol, groupCol);

        List<Map<String, Object>> items = jdbc.queryForList(sql, params.toArray());

        // Summary
        double totalActual = items.stream().mapToDouble(i -> ((Number) i.getOrDefault("actualHours", 0)).doubleValue()).sum();
        return Result.success(Map.of(
            "items", items,
            "summary", Map.of("count", items.size(), "totalActualHours", totalActual)
        ));
    }

    // ==================== 代课 ====================

    @PostMapping("/instances/{id}/substitute")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> substituteTeacher(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long newTeacherId = Long.valueOf(data.get("teacherId").toString());
        String reason = (String) data.getOrDefault("reason", "代课");
        // 记录原教师, 改为代课状态
        jdbc.update(
            "UPDATE schedule_instances SET original_teacher_id = teacher_id, teacher_id = ?, " +
            "status = 4, cancel_reason = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            newTeacherId, reason, id);
        return Result.success();
    }

    @PostMapping("/instances/{id}/cancel")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> cancelInstance(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        String reason = (String) data.getOrDefault("reason", "临时取消");
        jdbc.update(
            "UPDATE schedule_instances SET status = 1, cancel_reason = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            reason, id);
        return Result.success();
    }

    @PostMapping("/instances/{id}/restore")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> restoreInstance(@PathVariable Long id) {
        jdbc.update(
            "UPDATE schedule_instances SET status = 0, cancel_reason = NULL, " +
            "original_teacher_id = NULL, updated_at = NOW() WHERE id = ? AND deleted = 0", id);
        return Result.success();
    }

    // ==================== 节次配置 & 数据就绪 ====================

    @GetMapping("/schedule-config")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> getScheduleConfig(@RequestParam Long semesterId) {
        try {
            String json = jdbc.queryForObject(
                "SELECT config_value FROM system_configs WHERE config_key = ? AND deleted = 0",
                String.class, "schedule.periods." + semesterId);
            // Parse JSON string to Map
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> config = om.readValue(json, Map.class);
            return Result.success(config);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // No config for this semester, return default
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("periodsPerDay", 8);
            def.put("scheduleDays", List.of(1, 2, 3, 4, 5));
            List<Map<String, Object>> periods = new ArrayList<>();
            String[][] defaultPeriods = {
                {"1", "第一节", "08:00", "08:45"}, {"2", "第二节", "08:55", "09:40"},
                {"3", "第三节", "10:00", "10:45"}, {"4", "第四节", "10:55", "11:40"},
                {"5", "第五节", "14:00", "14:45"}, {"6", "第六节", "14:55", "15:40"},
                {"7", "第七节", "16:00", "16:45"}, {"8", "第八节", "16:55", "17:40"},
            };
            for (String[] p : defaultPeriods) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("period", Integer.parseInt(p[0])); m.put("name", p[1]);
                m.put("startTime", p[2]); m.put("endTime", p[3]);
                periods.add(m);
            }
            def.put("periods", periods);
            return Result.success(def);
        } catch (Exception e) {
            log.error("Failed to load schedule config", e);
            return Result.success(Map.of("periodsPerDay", 8));
        }
    }

    @PutMapping("/schedule-config")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> saveScheduleConfig(@RequestBody Map<String, Object> data) {
        Long semesterId = Long.valueOf(data.get("semesterId").toString());
        String key = "schedule.periods." + semesterId;
        try {
            // Remove semesterId from saved value
            Map<String, Object> config = new LinkedHashMap<>(data);
            config.remove("semesterId");
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = om.writeValueAsString(config);
            Long exists = jdbc.queryForObject("SELECT COUNT(1) FROM system_configs WHERE config_key = ?", Long.class, key);
            if (exists != null && exists > 0) {
                jdbc.update("UPDATE system_configs SET config_value = ?, updated_by = ? WHERE config_key = ?",
                    json, SecurityUtils.requireCurrentUserId(), key);
            } else {
                jdbc.update("INSERT INTO system_configs (config_key, config_value, config_type, description, created_by, deleted) VALUES (?, ?, 'JSON', '排课节次配置', ?, 0)",
                    key, json, SecurityUtils.requireCurrentUserId());
            }
            return Result.success();
        } catch (Exception e) {
            log.error("Failed to save schedule config", e);
            return Result.error("保存失败");
        }
    }

    @GetMapping("/schedule-readiness")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> checkReadiness(@RequestParam Long semesterId) {
        Map<String, Object> result = new LinkedHashMap<>();
        // 开课计划
        Long offeringCount = jdbc.queryForObject("SELECT COUNT(1) FROM semester_course_offerings WHERE semester_id=? AND deleted=0", Long.class, semesterId);
        result.put("offerings", Map.of("count", offeringCount, "status", offeringCount > 0 ? "ready" : "empty"));
        // 教学任务
        Long taskCount = jdbc.queryForObject("SELECT COUNT(1) FROM teaching_tasks WHERE semester_id=? AND deleted=0", Long.class, semesterId);
        Long noTeacher = jdbc.queryForObject("SELECT COUNT(1) FROM teaching_tasks t WHERE t.semester_id=? AND t.deleted=0 AND NOT EXISTS (SELECT 1 FROM teaching_task_teachers tt WHERE tt.task_id=t.id)", Long.class, semesterId);
        String taskStatus = taskCount == 0 ? "empty" : (noTeacher > 0 ? "warning" : "ready");
        result.put("tasks", Map.of("count", taskCount, "withoutTeacher", noTeacher, "status", taskStatus));
        // 教室
        Long classroomCount = jdbc.queryForObject("SELECT COUNT(1) FROM places WHERE deleted=0 AND type_code IN ('TYPE_CLASSROOM','CLASSROOM','TYPE_MULTIMEDIA','TYPE_COMPUTER_LAB','TYPE_LAB','TYPE_SMART_CLASS','TYPE_TRAINING')", Long.class);
        result.put("classrooms", Map.of("count", classroomCount, "status", classroomCount > 0 ? "ready" : "empty"));
        // 约束
        Long constraintCount = jdbc.queryForObject("SELECT COUNT(1) FROM scheduling_constraints WHERE semester_id=? AND deleted=0 AND enabled=1", Long.class, semesterId);
        result.put("constraints", Map.of("count", constraintCount, "status", constraintCount > 0 ? "ready" : "empty"));
        // 排课方案
        Long planCount = jdbc.queryForObject("SELECT COUNT(1) FROM course_schedules WHERE semester_id=? AND deleted=0", Long.class, semesterId);
        Long entryCount = jdbc.queryForObject("SELECT COUNT(1) FROM schedule_entries WHERE semester_id=? AND deleted=0", Long.class, semesterId);
        result.put("plans", Map.of("count", planCount, "entryCount", entryCount, "status", entryCount > 0 ? "ready" : "empty"));
        return Result.success(result);
    }

    // ==================== 排课方案 (CourseSchedule) ====================

    @GetMapping("/schedule-plans")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<Map<String, Object>>> listSchedulePlans(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer status) {
        StringBuilder sql = new StringBuilder(
            "SELECT id, semester_id AS semesterId, name, description, status, entry_count AS entryCount, " +
            "generated_at AS generatedAt, published_at AS publishedAt, remark, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM course_schedules WHERE deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        if (semesterId != null) { sql.append(" AND semester_id = ?"); params.add(semesterId); }
        if (status != null) { sql.append(" AND status = ?"); params.add(status); }
        sql.append(" ORDER BY created_at DESC");
        return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
    }

    @GetMapping("/schedule-plans/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> getSchedulePlan(@PathVariable Long id) {
        Map<String, Object> plan = jdbc.queryForMap(
            "SELECT id, semester_id AS semesterId, name, description, status, entry_count AS entryCount, " +
            "generated_at AS generatedAt, published_at AS publishedAt, remark, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM course_schedules WHERE id = ? AND deleted = 0", id);
        return Result.success(plan);
    }

    @PostMapping("/schedule-plans")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> createSchedulePlan(@RequestBody Map<String, Object> data) {
        Long semesterId = Long.valueOf(data.get("semesterId").toString());
        String name = (String) data.get("name");
        String description = (String) data.getOrDefault("description", "");
        jdbc.update(
            "INSERT INTO course_schedules (semester_id, name, description, status, entry_count, created_by, deleted) " +
            "VALUES (?, ?, ?, 0, 0, ?, 0)",
            semesterId, name, description, SecurityUtils.requireCurrentUserId());
        Long newId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return Result.success(Map.of("id", newId, "name", name, "status", 0));
    }

    @PutMapping("/schedule-plans/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> updateSchedulePlan(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        String name = (String) data.get("name");
        String description = (String) data.getOrDefault("description", "");
        jdbc.update("UPDATE course_schedules SET name = ?, description = ?, updated_by = ? WHERE id = ? AND deleted = 0",
            name, description, SecurityUtils.requireCurrentUserId(), id);
        return Result.success();
    }

    @DeleteMapping("/schedule-plans/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> deleteSchedulePlan(@PathVariable Long id) {
        jdbc.update("UPDATE course_schedules SET deleted = 1 WHERE id = ?", id);
        // 同时软删关联的条目
        jdbc.update("UPDATE schedule_entries SET deleted = 1 WHERE schedule_id = ?", id);
        return Result.success();
    }

    @PostMapping("/schedule-plans/{id}/publish")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> publishSchedulePlan(@PathVariable Long id) {
        jdbc.update("UPDATE course_schedules SET status = 1, published_at = NOW(), updated_by = ? WHERE id = ? AND deleted = 0",
            SecurityUtils.requireCurrentUserId(), id);
        return Result.success();
    }

    @PostMapping("/schedule-plans/{id}/archive")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> archiveSchedulePlan(@PathVariable Long id) {
        jdbc.update("UPDATE course_schedules SET status = 2, updated_by = ? WHERE id = ? AND deleted = 0",
            SecurityUtils.requireCurrentUserId(), id);
        return Result.success();
    }

    // ==================== 课表条目 ====================

    @GetMapping("/schedules")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<Map<String, Object>>> listSchedules(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer status) {

        StringBuilder sql = new StringBuilder(
            "SELECT id, semester_id AS semesterId, task_id AS taskId, " +
            "course_id AS courseId, org_unit_id AS orgUnitId, teacher_id AS teacherId, " +
            "classroom_id AS classroomId, weekday, start_slot AS startSlot, " +
            "end_slot AS endSlot, start_week AS startWeek, end_week AS endWeek, " +
            "week_type AS weekType, schedule_type AS scheduleType, is_locked AS isLocked, " +
            "entry_status AS entryStatus, conflict_flag AS conflictFlag, " +
            "created_at AS createdAt " +
            "FROM schedule_entries WHERE deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        if (semesterId != null) {
            sql.append(" AND semester_id = ?");
            params.add(semesterId);
        }
        if (status != null) {
            sql.append(" AND entry_status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY weekday, start_slot");

        return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
    }

    @GetMapping("/schedules/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> getSchedule(@PathVariable Long id) {
        Map<String, Object> entry = jdbc.queryForMap(
            "SELECT id, semester_id AS semesterId, task_id AS taskId, " +
            "course_id AS courseId, org_unit_id AS orgUnitId, teacher_id AS teacherId, " +
            "classroom_id AS classroomId, weekday, start_slot AS startSlot, " +
            "end_slot AS endSlot, start_week AS startWeek, end_week AS endWeek, " +
            "week_type AS weekType, schedule_type AS scheduleType, is_locked AS isLocked, " +
            "entry_status AS entryStatus, conflict_flag AS conflictFlag, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM schedule_entries WHERE id = ? AND deleted = 0", id
        );
        return Result.success(entry);
    }

    @PostMapping("/schedules")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> createSchedule(@RequestBody Map<String, Object> data) {
        long id = IdWorker.getId();
        Long semesterId = data.get("semesterId") != null ? ((Number) data.get("semesterId")).longValue() : null;
        Long taskId = data.get("taskId") != null ? ((Number) data.get("taskId")).longValue() : null;
        Long courseId = data.get("courseId") != null ? ((Number) data.get("courseId")).longValue() : null;
        Long orgUnitId = data.get("orgUnitId") != null ? ((Number) data.get("orgUnitId")).longValue() : null;
        Long teacherId = data.get("teacherId") != null ? ((Number) data.get("teacherId")).longValue() : null;
        Long classroomId = data.get("classroomId") != null ? ((Number) data.get("classroomId")).longValue() : null;
        Integer weekday = data.get("weekday") != null ? ((Number) data.get("weekday")).intValue() : null;
        Integer startSlot = data.get("startSlot") != null ? ((Number) data.get("startSlot")).intValue() : null;
        Integer endSlot = data.get("endSlot") != null ? ((Number) data.get("endSlot")).intValue() : null;
        Integer startWeek = data.get("startWeek") != null ? ((Number) data.get("startWeek")).intValue() : 1;
        Integer endWeek = data.get("endWeek") != null ? ((Number) data.get("endWeek")).intValue() : getSemesterTeachingWeeks(semesterId);
        Integer weekType = data.get("weekType") != null ? ((Number) data.get("weekType")).intValue() : 0;
        Integer scheduleType = data.get("scheduleType") != null ? ((Number) data.get("scheduleType")).intValue() : 1;

        jdbc.update(
            "INSERT INTO schedule_entries (id, semester_id, task_id, course_id, org_unit_id, " +
            "teacher_id, classroom_id, weekday, start_slot, end_slot, " +
            "start_week, end_week, week_type, schedule_type, " +
            "entry_status, conflict_flag, created_by, created_at, updated_at, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, ?, NOW(), NOW(), 0)",
            id, semesterId, taskId, courseId, orgUnitId,
            teacherId, classroomId, weekday, startSlot, endSlot,
            startWeek, endWeek, weekType, scheduleType,
            SecurityUtils.requireCurrentUserId()
        );

        data.put("id", id);
        return Result.success(data);
    }

    @PutMapping("/schedules/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> updateSchedule(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        // 构建动态 UPDATE：只更新传入的字段
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (data.containsKey("teacherId")) {
            sets.add("teacher_id = ?");
            params.add(data.get("teacherId") != null ? ((Number) data.get("teacherId")).longValue() : null);
        }
        if (data.containsKey("classroomId")) {
            sets.add("classroom_id = ?");
            params.add(data.get("classroomId") != null ? ((Number) data.get("classroomId")).longValue() : null);
        }
        if (data.containsKey("weekday")) {
            sets.add("weekday = ?");
            params.add(((Number) data.get("weekday")).intValue());
        }
        if (data.containsKey("startSlot")) {
            sets.add("start_slot = ?");
            params.add(((Number) data.get("startSlot")).intValue());
        }
        if (data.containsKey("endSlot")) {
            sets.add("end_slot = ?");
            params.add(((Number) data.get("endSlot")).intValue());
        }
        if (data.containsKey("startWeek")) {
            sets.add("start_week = ?");
            params.add(((Number) data.get("startWeek")).intValue());
        }
        if (data.containsKey("endWeek")) {
            sets.add("end_week = ?");
            params.add(((Number) data.get("endWeek")).intValue());
        }
        if (data.containsKey("weekType")) {
            sets.add("week_type = ?");
            params.add(((Number) data.get("weekType")).intValue());
        }
        if (sets.isEmpty()) return Result.success();
        sets.add("updated_at = NOW()");
        params.add(id);
        jdbc.update(
            "UPDATE schedule_entries SET " + String.join(", ", sets) + " WHERE id = ? AND deleted = 0",
            params.toArray()
        );
        return Result.success();
    }

    @DeleteMapping("/schedules/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> deleteSchedule(@PathVariable Long id) {
        jdbc.update("UPDATE schedule_entries SET deleted = 1 WHERE id = ?", id);
        return Result.success();
    }

    /** 锁定/解锁排课条目，锁定后自动排课不会覆盖 */
    @PostMapping("/schedules/{id}/toggle-lock")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> toggleLock(@PathVariable Long id) {
        jdbc.update("UPDATE schedule_entries SET is_locked = 1 - is_locked WHERE id = ? AND deleted = 0", id);
        Integer locked = jdbc.queryForObject("SELECT is_locked FROM schedule_entries WHERE id = ?", Integer.class, id);
        return Result.success(Map.of("isLocked", locked != null ? locked : 0));
    }

    /** 批量锁定/解锁 */
    @PostMapping("/schedules/batch-lock")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> batchLock(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("ids");
        Integer lock = ((Number) body.getOrDefault("lock", 1)).intValue();
        if (ids == null || ids.isEmpty()) return Result.success(Map.of("updated", 0));
        String placeholders = String.join(",", ids.stream().map(i -> "?").toList());
        Object[] args = new Object[ids.size() + 1];
        args[0] = lock;
        for (int i = 0; i < ids.size(); i++) args[i + 1] = ids.get(i).longValue();
        int updated = jdbc.update(
            "UPDATE schedule_entries SET is_locked = ? WHERE id IN (" + placeholders + ") AND deleted = 0", args);
        return Result.success(Map.of("updated", updated));
    }

    /**
     * 重置排课：清除排课条目 + 重置任务排课状态。已锁定的条目不会被清除。
     */
    @PostMapping("/schedules/reset")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> resetSchedule(@RequestBody Map<String, Object> body) {
        Long semesterId = ((Number) body.get("semesterId")).longValue();
        boolean keepLocked = body.get("keepLocked") == null || Boolean.TRUE.equals(body.get("keepLocked"));
        int cleared;
        if (keepLocked) {
            cleared = jdbc.update(
                "UPDATE schedule_entries SET deleted = 1 WHERE semester_id = ? AND deleted = 0 AND is_locked = 0", semesterId);
        } else {
            cleared = jdbc.update(
                "UPDATE schedule_entries SET deleted = 1 WHERE semester_id = ? AND deleted = 0", semesterId);
        }
        // 重置任务状态：无排课条目的 → 0，只有部分锁定条目的 → 1
        int reset = jdbc.update(
            "UPDATE teaching_tasks t SET scheduling_status = 0 " +
            "WHERE t.semester_id = ? AND t.deleted = 0 " +
            "AND NOT EXISTS (SELECT 1 FROM schedule_entries e WHERE e.task_id = t.id AND e.deleted = 0)",
            semesterId);
        jdbc.update(
            "UPDATE teaching_tasks t SET scheduling_status = 1 " +
            "WHERE t.semester_id = ? AND t.deleted = 0 " +
            "AND EXISTS (SELECT 1 FROM schedule_entries e WHERE e.task_id = t.id AND e.deleted = 0)",
            semesterId);
        return Result.success(Map.of("cleared", cleared, "reset", reset, "keepLocked", keepLocked));
    }

    @GetMapping("/schedule-teachers")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<Map<String, Object>>> getTeachersGroupedByDept() {
        return Result.success(jdbc.queryForList(
            "SELECT u.id, u.real_name AS realName, u.username, " +
            "COALESCE(ou.unit_name, '未分配') AS deptName, ou.id AS deptId " +
            "FROM users u " +
            "LEFT JOIN org_units ou ON ou.id = u.primary_org_unit_id AND ou.deleted = 0 " +
            "WHERE u.user_type_code = 'TEACHER' AND u.deleted = 0 " +
            "ORDER BY ou.unit_name, u.real_name"
        ));
    }

    @GetMapping("/schedules/by-class/{classId}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<Map<String, Object>>> getSchedulesByClass(
            @PathVariable("classId") Long orgUnitId,
            @RequestParam(required = false) Long semesterId) {

        StringBuilder sql = new StringBuilder(
            "SELECT se.id, se.semester_id AS semesterId, se.task_id AS taskId, " +
            "se.course_id AS courseId, se.org_unit_id AS orgUnitId, se.teacher_id AS teacherId, " +
            "se.classroom_id AS classroomId, " +
            "se.weekday AS dayOfWeek, se.start_slot AS periodStart, " +
            "se.end_slot AS periodEnd, se.start_week AS weekStart, se.end_week AS weekEnd, " +
            "se.week_type AS weekType, se.schedule_type AS scheduleType, se.is_locked AS isLocked, " +
            "se.entry_status AS entryStatus, " +
            "c.course_name AS courseName, u.real_name AS teacherName, " +
            "COALESCE(p.place_code, p.place_name) AS classroomName " +
            "FROM schedule_entries se " +
            "LEFT JOIN courses c ON c.id = se.course_id " +
            "LEFT JOIN users u ON u.id = se.teacher_id " +
            "LEFT JOIN places p ON p.id = se.classroom_id " +
            "WHERE se.org_unit_id = ? AND se.deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        params.add(orgUnitId);
        if (semesterId != null) {
            sql.append(" AND se.semester_id = ?");
            params.add(semesterId);
        }
        sql.append(" ORDER BY se.weekday, se.start_slot");

        return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
    }

    @GetMapping("/schedules/by-teacher/{teacherId}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<Map<String, Object>>> getSchedulesByTeacher(
            @PathVariable Long teacherId,
            @RequestParam(required = false) Long semesterId) {

        StringBuilder sql = new StringBuilder(
            "SELECT se.id, se.semester_id AS semesterId, se.task_id AS taskId, " +
            "se.course_id AS courseId, se.org_unit_id AS orgUnitId, se.teacher_id AS teacherId, " +
            "se.classroom_id AS classroomId, " +
            "se.weekday AS dayOfWeek, se.start_slot AS periodStart, " +
            "se.end_slot AS periodEnd, se.start_week AS weekStart, se.end_week AS weekEnd, " +
            "se.week_type AS weekType, se.schedule_type AS scheduleType, se.is_locked AS isLocked, " +
            "se.entry_status AS entryStatus, " +
            "c.course_name AS courseName, u.real_name AS teacherName, " +
            "COALESCE(p.place_code, p.place_name) AS classroomName " +
            "FROM schedule_entries se " +
            "LEFT JOIN courses c ON c.id = se.course_id " +
            "LEFT JOIN users u ON u.id = se.teacher_id " +
            "LEFT JOIN places p ON p.id = se.classroom_id " +
            "WHERE se.teacher_id = ? AND se.deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        params.add(teacherId);
        if (semesterId != null) {
            sql.append(" AND se.semester_id = ?");
            params.add(semesterId);
        }
        sql.append(" ORDER BY se.weekday, se.start_slot");

        return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
    }

    @GetMapping("/schedules/by-classroom/{classroomId}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<Map<String, Object>>> getSchedulesByClassroom(
            @PathVariable Long classroomId,
            @RequestParam(required = false) Long semesterId) {

        StringBuilder sql = new StringBuilder(
            "SELECT se.id, se.semester_id AS semesterId, se.task_id AS taskId, " +
            "se.course_id AS courseId, se.org_unit_id AS orgUnitId, se.teacher_id AS teacherId, " +
            "se.classroom_id AS classroomId, " +
            "se.weekday AS dayOfWeek, se.start_slot AS periodStart, " +
            "se.end_slot AS periodEnd, se.start_week AS weekStart, se.end_week AS weekEnd, " +
            "se.week_type AS weekType, se.schedule_type AS scheduleType, se.is_locked AS isLocked, " +
            "se.entry_status AS entryStatus, " +
            "c.course_name AS courseName, u.real_name AS teacherName, " +
            "COALESCE(p.place_code, p.place_name) AS classroomName " +
            "FROM schedule_entries se " +
            "LEFT JOIN courses c ON c.id = se.course_id " +
            "LEFT JOIN users u ON u.id = se.teacher_id " +
            "LEFT JOIN places p ON p.id = se.classroom_id " +
            "WHERE se.classroom_id = ? AND se.deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        params.add(classroomId);
        if (semesterId != null) {
            sql.append(" AND se.semester_id = ?");
            params.add(semesterId);
        }
        sql.append(" ORDER BY se.weekday, se.start_slot");

        return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
    }

    // ==================== 智能排课 / 拖拽移动 / 冲突检测 ====================

    @PostMapping("/schedules/auto-schedule")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> autoSchedule(@RequestBody Map<String, Object> params) {
        if (autoSchedulingService == null) {
            return Result.error("排课服务未启用");
        }
        Long semesterId = Long.parseLong(params.get("semesterId").toString());
        Map<String, Object> result = autoSchedulingService.autoSchedule(semesterId, params);

        // 触发事件: 排课完成
        if (triggerService != null) {
            try {
                triggerService.fire("SCHEDULE_PUBLISHED", Map.of(
                    "semesterId", semesterId,
                    "scheduledCount", result.getOrDefault("scheduledCount", 0),
                    "action", "AUTO_SCHEDULE"
                ));
            } catch (Exception ignored) {}
        }

        return Result.success(result);
    }

    @PostMapping("/schedules/{id}/move")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> moveEntry(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int newDay = Integer.parseInt(body.get("dayOfWeek").toString());
        int newPeriod = Integer.parseInt(body.get("periodStart").toString());

        // Get current entry span
        Map<String, Object> entry = jdbc.queryForMap(
            "SELECT end_slot - start_slot as span FROM schedule_entries WHERE id = ? AND deleted = 0", id);
        int span = ((Number) entry.get("span")).intValue();
        int newEndPeriod = newPeriod + span;

        // Update
        jdbc.update(
            "UPDATE schedule_entries SET weekday = ?, start_slot = ?, end_slot = ? WHERE id = ? AND deleted = 0",
            newDay, newPeriod, newEndPeriod, id);

        // Optionally update classroom
        if (body.get("classroomId") != null) {
            Long classroomId = Long.parseLong(body.get("classroomId").toString());
            jdbc.update("UPDATE schedule_entries SET classroom_id = ? WHERE id = ?", classroomId, id);
        }

        return Result.success(Map.of("success", true));
    }

    @PostMapping("/schedules/check-move-conflict")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> checkMoveConflict(@RequestBody Map<String, Object> body) {
        Long entryId = Long.parseLong(body.get("entryId").toString());
        Long semesterId = Long.parseLong(body.get("semesterId").toString());
        int newDay = Integer.parseInt(body.get("dayOfWeek").toString());
        int newPeriod = Integer.parseInt(body.get("periodStart").toString());

        Map<String, Object> entry = jdbc.queryForMap(
            "SELECT teacher_id, org_unit_id, end_slot - start_slot + 1 as span FROM schedule_entries WHERE id = ?", entryId);
        int span = ((Number) entry.get("span")).intValue();
        int newEndPeriod = newPeriod + span - 1;

        Map<String, Object> result = new HashMap<>();
        boolean hasConflict = false;

        // Check teacher conflict
        if (entry.get("teacher_id") != null) {
            Long teacherId = ((Number) entry.get("teacher_id")).longValue();
            List<Map<String, Object>> tc = jdbc.queryForList(
                "SELECT id FROM schedule_entries WHERE semester_id=? AND teacher_id=? AND weekday=? " +
                "AND start_slot <= ? AND end_slot >= ? AND id != ? AND deleted=0",
                semesterId, teacherId, newDay, newEndPeriod, newPeriod, entryId);
            if (!tc.isEmpty()) hasConflict = true;
            result.put("teacherConflicts", tc);
        }

        // Check class conflict
        if (entry.get("org_unit_id") != null) {
            Long orgUnitId = ((Number) entry.get("org_unit_id")).longValue();
            List<Map<String, Object>> cc = jdbc.queryForList(
                "SELECT id FROM schedule_entries WHERE semester_id=? AND org_unit_id=? AND weekday=? " +
                "AND start_slot <= ? AND end_slot >= ? AND id != ? AND deleted=0",
                semesterId, orgUnitId, newDay, newEndPeriod, newPeriod, entryId);
            if (!cc.isEmpty()) hasConflict = true;
            result.put("classConflicts", cc);
        }

        result.put("hasConflict", hasConflict);
        return Result.success(result);
    }

    // ==================== 课表导出 ====================

    @GetMapping("/schedules/export/class/{classId}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public void exportClassSchedule(
            @PathVariable Long orgUnitId,
            @RequestParam Long semesterId,
            HttpServletResponse response) throws java.io.IOException {
        byte[] data = exportService.exportClassSchedule(semesterId, orgUnitId);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=class_schedule.xlsx");
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    @GetMapping("/schedules/export/teacher/{teacherId}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public void exportTeacherSchedule(
            @PathVariable Long teacherId,
            @RequestParam Long semesterId,
            HttpServletResponse response) throws java.io.IOException {
        byte[] data = exportService.exportTeacherSchedule(semesterId, teacherId);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=teacher_schedule.xlsx");
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    // ==================== 调课管理 ====================

    @GetMapping("/adjustments")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> listAdjustments(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        StringBuilder where = new StringBuilder(" WHERE deleted = 0");
        List<Object> params = new ArrayList<>();
        if (status != null) {
            where.append(" AND approval_status = ?");
            params.add(status);
        }

        String countSql = "SELECT COUNT(*) FROM schedule_adjustments" + where;
        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        String sql = "SELECT id, adjustment_code AS adjustmentCode, semester_id AS semesterId, " +
            "original_entry_id AS originalEntryId, adjustment_type AS adjustmentType, " +
            "original_date AS originalDate, original_weekday AS originalWeekday, " +
            "original_slot AS originalSlot, original_classroom_id AS originalClassroomId, " +
            "original_teacher_id AS originalTeacherId, " +
            "new_date AS newDate, new_weekday AS newWeekday, new_slot AS newSlot, " +
            "new_classroom_id AS newClassroomId, new_teacher_id AS newTeacherId, " +
            "applicant_id AS applicantId, apply_reason AS applyReason, apply_time AS applyTime, " +
            "approval_status AS approvalStatus, approver_id AS approverId, " +
            "approval_time AS approvalTime, approval_comment AS approvalComment, " +
            "executed, executed_at AS executedAt, " +
            "created_at AS createdAt " +
            "FROM schedule_adjustments" + where +
            " ORDER BY created_at DESC LIMIT ? OFFSET ?";
        params.add(size);
        params.add((page - 1) * size);

        List<Map<String, Object>> records = jdbc.queryForList(sql, params.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return Result.success(result);
    }

    @GetMapping("/adjustments/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> getAdjustment(@PathVariable Long id) {
        Map<String, Object> adj = jdbc.queryForMap(
            "SELECT id, adjustment_code AS adjustmentCode, semester_id AS semesterId, " +
            "original_entry_id AS originalEntryId, adjustment_type AS adjustmentType, " +
            "original_date AS originalDate, original_weekday AS originalWeekday, " +
            "original_slot AS originalSlot, original_classroom_id AS originalClassroomId, " +
            "original_teacher_id AS originalTeacherId, " +
            "new_date AS newDate, new_weekday AS newWeekday, new_slot AS newSlot, " +
            "new_classroom_id AS newClassroomId, new_teacher_id AS newTeacherId, " +
            "applicant_id AS applicantId, apply_reason AS applyReason, apply_time AS applyTime, " +
            "approval_status AS approvalStatus, approver_id AS approverId, " +
            "approval_time AS approvalTime, approval_comment AS approvalComment, " +
            "executed, executed_at AS executedAt, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM schedule_adjustments WHERE id = ? AND deleted = 0", id
        );
        return Result.success(adj);
    }

    @PostMapping("/adjustments")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> createAdjustment(@RequestBody Map<String, Object> data) {
        long id = IdWorker.getId();
        String adjustmentCode = "ADJ" + id;
        Long semesterId = data.get("semesterId") != null ? ((Number) data.get("semesterId")).longValue() : null;
        Long originalEntryId = data.get("originalEntryId") != null ? ((Number) data.get("originalEntryId")).longValue() : null;
        Integer adjustmentType = data.get("adjustmentType") != null ? ((Number) data.get("adjustmentType")).intValue() : null;

        String originalDateStr = (String) data.get("originalDate");
        LocalDate originalDate = originalDateStr != null ? LocalDate.parse(originalDateStr) : null;
        Integer originalWeekday = data.get("originalWeekday") != null ? ((Number) data.get("originalWeekday")).intValue() : null;
        Integer originalSlot = data.get("originalSlot") != null ? ((Number) data.get("originalSlot")).intValue() : null;
        Long originalClassroomId = data.get("originalClassroomId") != null ? ((Number) data.get("originalClassroomId")).longValue() : null;
        Long originalTeacherId = data.get("originalTeacherId") != null ? ((Number) data.get("originalTeacherId")).longValue() : null;

        String newDateStr = (String) data.get("newDate");
        LocalDate newDate = newDateStr != null ? LocalDate.parse(newDateStr) : null;
        Integer newWeekday = data.get("newWeekday") != null ? ((Number) data.get("newWeekday")).intValue() : null;
        Integer newSlot = data.get("newSlot") != null ? ((Number) data.get("newSlot")).intValue() : null;
        Long newClassroomId = data.get("newClassroomId") != null ? ((Number) data.get("newClassroomId")).longValue() : null;
        Long newTeacherId = data.get("newTeacherId") != null ? ((Number) data.get("newTeacherId")).longValue() : null;

        Long applicantId = data.get("applicantId") != null
                ? ((Number) data.get("applicantId")).longValue()
                : SecurityUtils.requireCurrentUserId();
        String applyReason = (String) data.get("applyReason");

        jdbc.update(
            "INSERT INTO schedule_adjustments (id, adjustment_code, semester_id, original_entry_id, " +
            "adjustment_type, original_date, original_weekday, original_slot, " +
            "original_classroom_id, original_teacher_id, " +
            "new_date, new_weekday, new_slot, new_classroom_id, new_teacher_id, " +
            "applicant_id, apply_reason, apply_time, " +
            "approval_status, executed, created_at, updated_at, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), 0, 0, NOW(), NOW(), 0)",
            id, adjustmentCode, semesterId, originalEntryId,
            adjustmentType, originalDate, originalWeekday, originalSlot,
            originalClassroomId, originalTeacherId,
            newDate, newWeekday, newSlot, newClassroomId, newTeacherId,
            applicantId, applyReason
        );

        data.put("id", id);
        data.put("adjustmentCode", adjustmentCode);
        return Result.success(data);
    }

    @PostMapping("/adjustments/{id}/approve")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> approveAdjustment(@PathVariable Long id) {
        Long approverId = SecurityUtils.requireCurrentUserId();
        jdbc.update(
            "UPDATE schedule_adjustments SET approval_status = 1, approver_id = ?, " +
            "approval_time = NOW(), updated_at = NOW() WHERE id = ? AND deleted = 0",
            approverId, id
        );
        return Result.success();
    }

    @PostMapping("/adjustments/{id}/reject")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> rejectAdjustment(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long approverId = SecurityUtils.requireCurrentUserId();
        String comment = (String) data.get("approvalComment");
        jdbc.update(
            "UPDATE schedule_adjustments SET approval_status = 2, approver_id = ?, " +
            "approval_time = NOW(), approval_comment = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            approverId, comment, id
        );
        return Result.success();
    }

    @PostMapping("/adjustments/{id}/execute")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> executeAdjustment(@PathVariable Long id) {
        // 1. 标记已执行
        jdbc.update(
            "UPDATE schedule_adjustments SET executed = 1, executed_at = NOW(), " +
            "updated_at = NOW() WHERE id = ? AND deleted = 0", id
        );

        // 2. 联动更新实况课表
        try {
            Map<String, Object> adj = jdbc.queryForMap(
                "SELECT semester_id, original_entry_id, adjustment_type, " +
                "new_day_of_week, new_period_start, new_period_end, new_classroom_id, new_week " +
                "FROM schedule_adjustments WHERE id = ? AND deleted = 0", id);
            Long semesterId = ((Number) adj.get("semester_id")).longValue();
            Long entryId = adj.get("original_entry_id") != null ? ((Number) adj.get("original_entry_id")).longValue() : null;
            int adjType = ((Number) adj.get("adjustment_type")).intValue();

            if (entryId != null) {
                if (adjType == 2) {
                    // 停课: 取消对应实例
                    if (adj.get("new_week") != null) {
                        int week = ((Number) adj.get("new_week")).intValue();
                        jdbc.update(
                            "UPDATE schedule_instances SET status = 1, cancel_reason = '调课停课', source_id = ? " +
                            "WHERE entry_id = ? AND semester_id = ? AND week_number = ? AND status = 0 AND deleted = 0",
                            id, entryId, semesterId, week);
                    }
                } else if (adjType == 1) {
                    // 调课: 原实例标调走 + 生成新实例
                    if (adj.get("new_week") != null) {
                        int week = ((Number) adj.get("new_week")).intValue();
                        jdbc.update(
                            "UPDATE schedule_instances SET status = 2, cancel_reason = '已调课', source_id = ? " +
                            "WHERE entry_id = ? AND semester_id = ? AND week_number = ? AND status = 0 AND deleted = 0",
                            id, entryId, semesterId, week);
                    }
                    // 新实例在调课目标日期生成(如有新时间)
                }
            }
        } catch (Exception e) {
            log.warn("调课联动实况更新失败(非致命): {}", e.getMessage());
        }

        return Result.success();
    }

    @PostMapping("/adjustments/{id}/cancel")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> cancelAdjustment(@PathVariable Long id) {
        jdbc.update(
            "UPDATE schedule_adjustments SET approval_status = 3, " +
            "updated_at = NOW() WHERE id = ? AND deleted = 0", id
        );
        return Result.success();
    }

    @GetMapping("/adjustments/my-applications")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> myApplications(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long applicantId = SecurityUtils.requireCurrentUserId();
        StringBuilder where = new StringBuilder(" WHERE deleted = 0 AND applicant_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(applicantId);
        if (status != null) {
            where.append(" AND approval_status = ?");
            params.add(status);
        }

        String countSql = "SELECT COUNT(*) FROM schedule_adjustments" + where;
        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        String sql = "SELECT id, adjustment_code AS adjustmentCode, semester_id AS semesterId, " +
            "original_entry_id AS originalEntryId, adjustment_type AS adjustmentType, " +
            "original_date AS originalDate, original_weekday AS originalWeekday, " +
            "original_slot AS originalSlot, " +
            "new_date AS newDate, new_weekday AS newWeekday, new_slot AS newSlot, " +
            "applicant_id AS applicantId, apply_reason AS applyReason, apply_time AS applyTime, " +
            "approval_status AS approvalStatus, approval_comment AS approvalComment, " +
            "executed, created_at AS createdAt " +
            "FROM schedule_adjustments" + where +
            " ORDER BY created_at DESC LIMIT ? OFFSET ?";
        params.add(size);
        params.add((page - 1) * size);

        List<Map<String, Object>> records = jdbc.queryForList(sql, params.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return Result.success(result);
    }

    @GetMapping("/adjustments/pending-approvals")
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<Map<String, Object>> pendingApprovals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        String countSql = "SELECT COUNT(*) FROM schedule_adjustments WHERE deleted = 0 AND approval_status = 0";
        Long total = jdbc.queryForObject(countSql, Long.class);

        String sql = "SELECT id, adjustment_code AS adjustmentCode, semester_id AS semesterId, " +
            "original_entry_id AS originalEntryId, adjustment_type AS adjustmentType, " +
            "original_date AS originalDate, original_weekday AS originalWeekday, " +
            "original_slot AS originalSlot, " +
            "new_date AS newDate, new_weekday AS newWeekday, new_slot AS newSlot, " +
            "applicant_id AS applicantId, apply_reason AS applyReason, apply_time AS applyTime, " +
            "approval_status AS approvalStatus, " +
            "executed, created_at AS createdAt " +
            "FROM schedule_adjustments WHERE deleted = 0 AND approval_status = 0 " +
            "ORDER BY created_at DESC LIMIT ? OFFSET ?";

        List<Map<String, Object>> records = jdbc.queryForList(sql, size, (page - 1) * size);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return Result.success(result);
    }

    // ==================== 自习课填充 ====================

    /**
     * 自动填充自习课：扫描所有班级的空课位，插入 schedule_type=4 的自习课条目。
     * 连续空节次合并为一个条目。
     */
    @PostMapping("/self-study/fill")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> fillSelfStudy(@RequestBody Map<String, Object> body) {
        Long semesterId = ((Number) body.get("semesterId")).longValue();
        int maxPeriods = body.get("maxPeriods") != null ? ((Number) body.get("maxPeriods")).intValue() : 8;
        int maxWeekday = body.get("maxWeekday") != null ? ((Number) body.get("maxWeekday")).intValue() : 5;
        int startWeek = body.get("startWeek") != null ? ((Number) body.get("startWeek")).intValue() : 1;
        int endWeek = body.get("endWeek") != null ? ((Number) body.get("endWeek")).intValue() : getSemesterTeachingWeeks(semesterId);

        // 1. Get all classes
        List<Map<String, Object>> classes = jdbc.queryForList(
            "SELECT id FROM org_units WHERE unit_type = 'CLASS' AND deleted = 0"
        );

        // 2. Get occupied slots per class
        int inserted = 0;
        long userId = SecurityUtils.requireCurrentUserId();

        for (Map<String, Object> cls : classes) {
            Long orgUnitId = ((Number) cls.get("id")).longValue();

            // Get existing occupied slots for this class
            List<Map<String, Object>> occupied = jdbc.queryForList(
                "SELECT weekday, start_slot, end_slot FROM schedule_entries " +
                "WHERE semester_id = ? AND org_unit_id = ? AND deleted = 0 AND entry_status = 1",
                semesterId, orgUnitId
            );

            // Build occupied set: weekday -> set of periods
            Map<Integer, Set<Integer>> occupiedMap = new HashMap<>();
            for (Map<String, Object> row : occupied) {
                int wd = ((Number) row.get("weekday")).intValue();
                int ss = ((Number) row.get("start_slot")).intValue();
                int es = ((Number) row.get("end_slot")).intValue();
                occupiedMap.computeIfAbsent(wd, k -> new HashSet<>());
                for (int p = ss; p <= es; p++) occupiedMap.get(wd).add(p);
            }

            // 3. For each weekday, find consecutive empty slots and insert self-study
            for (int wd = 1; wd <= maxWeekday; wd++) {
                Set<Integer> occ = occupiedMap.getOrDefault(wd, Set.of());
                int runStart = -1;

                for (int p = 1; p <= maxPeriods + 1; p++) {
                    boolean empty = p <= maxPeriods && !occ.contains(p);
                    if (empty && runStart == -1) {
                        runStart = p;
                    } else if (!empty && runStart != -1) {
                        // Insert self-study block from runStart to p-1
                        long id = IdWorker.getId();
                        jdbc.update(
                            "INSERT INTO schedule_entries (id, semester_id, task_id, course_id, org_unit_id, " +
                            "teacher_id, classroom_id, weekday, start_slot, end_slot, " +
                            "start_week, end_week, week_type, schedule_type, " +
                            "entry_status, conflict_flag, created_by, created_at, updated_at, deleted) " +
                            "VALUES (?, ?, NULL, NULL, ?, NULL, NULL, ?, ?, ?, ?, ?, 0, 4, 1, 0, ?, NOW(), NOW(), 0)",
                            id, semesterId, orgUnitId, wd, runStart, p - 1,
                            startWeek, endWeek, userId
                        );
                        inserted++;
                        runStart = -1;
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("inserted", inserted);
        result.put("classCount", classes.size());
        return Result.success(result);
    }

    /**
     * 清除所有自习课条目 (schedule_type=4)
     */
    @DeleteMapping("/self-study/clear")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Map<String, Object>> clearSelfStudy(@RequestParam Long semesterId) {
        int deleted = jdbc.update(
            "UPDATE schedule_entries SET deleted = 1 WHERE semester_id = ? AND schedule_type = 4 AND deleted = 0",
            semesterId
        );
        Map<String, Object> result = new HashMap<>();
        result.put("cleared", deleted);
        return Result.success(result);
    }

    /** 获取学期最后一个教学周号（week_type=1） */
    private int getSemesterTeachingWeeks(Long semesterId) {
        try {
            Integer week = jdbc.queryForObject(
                "SELECT MAX(week_number) FROM academic_weeks WHERE semester_id = ? AND week_type = 1",
                Integer.class, semesterId);
            return week != null && week > 0 ? week : 16;
        } catch (Exception e) {
            return 16;
        }
    }
}
