package com.school.management.infrastructure.extension.plugins.education.application.teaching.scheduling;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.application.event.TriggerService;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.extension.plugins.education.application.teaching.AutoSchedulingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.school.management.infrastructure.extension.plugins.education.constants.EducationTriggerPoints.SCHEDULE_PUBLISHED;

/**
 * 排课条目 / 方案 / 配置 / 移动冲突 / 自习课 应用服务 — 兜底.
 *
 * 从 {@code TeachingScheduleController} 拆分而来 (M3.2.5 — 拆 service 不拆 controller).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleEntryApplicationService {

    private final JdbcTemplate jdbc;

    @Autowired(required = false)
    private AutoSchedulingService autoSchedulingService;

    @Autowired(required = false)
    private TriggerService triggerService;

    // ==================== 节次配置 ====================

    /** 节次配置: 读取或返回默认值 */
    public Map<String, Object> getScheduleConfig(Long semesterId) {
        try {
            String json = jdbc.queryForObject(
                "SELECT config_value FROM system_configs WHERE config_key = ? AND deleted = 0",
                String.class, "schedule.periods." + semesterId);
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> config = om.readValue(json, Map.class);
            return config;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return defaultScheduleConfig();
        } catch (Exception e) {
            log.error("Failed to load schedule config", e);
            return Map.of("periodsPerDay", 8);
        }
    }

    private Map<String, Object> defaultScheduleConfig() {
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
        return def;
    }

    /** 节次配置: 保存 — upsert */
    public boolean saveScheduleConfig(Map<String, Object> data) {
        Long semesterId = Long.valueOf(data.get("semesterId").toString());
        String key = "schedule.periods." + semesterId;
        try {
            Map<String, Object> config = new LinkedHashMap<>(data);
            config.remove("semesterId");
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = om.writeValueAsString(config);
            Long exists = jdbc.queryForObject("SELECT COUNT(1) FROM system_configs WHERE config_key = ?", Long.class, key);
            if (exists != null && exists > 0) {
                jdbc.update("UPDATE system_configs SET config_value = ?, updated_by = ? WHERE config_key = ?",
                    json, SecurityUtils.requireCurrentUserId(), key);
            } else {
                jdbc.update("INSERT INTO system_configs (config_key, config_value, config_type, description, created_by, deleted) " +
                    "VALUES (?, ?, 'JSON', '排课节次配置', ?, 0)",
                    key, json, SecurityUtils.requireCurrentUserId());
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to save schedule config", e);
            return false;
        }
    }

    // ==================== 数据就绪检查 ====================

    public Map<String, Object> checkReadiness(Long semesterId) {
        Map<String, Object> result = new LinkedHashMap<>();
        Long offeringCount = jdbc.queryForObject(
            "SELECT COUNT(1) FROM semester_course_offerings WHERE semester_id=? AND deleted=0", Long.class, semesterId);
        result.put("offerings", Map.of("count", offeringCount, "status", offeringCount > 0 ? "ready" : "empty"));

        Long taskCount = jdbc.queryForObject(
            "SELECT COUNT(1) FROM teaching_tasks WHERE semester_id=? AND deleted=0", Long.class, semesterId);
        Long noTeacher = jdbc.queryForObject(
            "SELECT COUNT(1) FROM teaching_tasks t WHERE t.semester_id=? AND t.deleted=0 AND NOT EXISTS " +
            "(SELECT 1 FROM teaching_task_teachers tt WHERE tt.task_id=t.id)", Long.class, semesterId);
        String taskStatus = taskCount == 0 ? "empty" : (noTeacher > 0 ? "warning" : "ready");
        result.put("tasks", Map.of("count", taskCount, "withoutTeacher", noTeacher, "status", taskStatus));

        Long classroomCount = jdbc.queryForObject(
            "SELECT COUNT(1) FROM places WHERE deleted=0 AND type_code IN " +
            "('TYPE_CLASSROOM','CLASSROOM','TYPE_MULTIMEDIA','TYPE_COMPUTER_LAB','TYPE_LAB','TYPE_SMART_CLASS','TYPE_TRAINING')",
            Long.class);
        result.put("classrooms", Map.of("count", classroomCount, "status", classroomCount > 0 ? "ready" : "empty"));

        Long constraintCount = jdbc.queryForObject(
            "SELECT COUNT(1) FROM scheduling_constraints WHERE semester_id=? AND deleted=0 AND enabled=1", Long.class, semesterId);
        result.put("constraints", Map.of("count", constraintCount, "status", constraintCount > 0 ? "ready" : "empty"));

        Long planCount = jdbc.queryForObject(
            "SELECT COUNT(1) FROM course_schedules WHERE semester_id=? AND deleted=0", Long.class, semesterId);
        Long entryCount = jdbc.queryForObject(
            "SELECT COUNT(1) FROM schedule_entries WHERE semester_id=? AND deleted=0", Long.class, semesterId);
        result.put("plans", Map.of("count", planCount, "entryCount", entryCount, "status", entryCount > 0 ? "ready" : "empty"));

        return result;
    }

    // ==================== 排课方案 (CourseSchedule) ====================

    public List<Map<String, Object>> listSchedulePlans(Long semesterId, Integer status) {
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
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> getSchedulePlan(Long id) {
        return jdbc.queryForMap(
            "SELECT id, semester_id AS semesterId, name, description, status, entry_count AS entryCount, " +
            "generated_at AS generatedAt, published_at AS publishedAt, remark, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM course_schedules WHERE id = ? AND deleted = 0", id);
    }

    public Map<String, Object> createSchedulePlan(Map<String, Object> data) {
        Long semesterId = Long.valueOf(data.get("semesterId").toString());
        String name = (String) data.get("name");
        String description = (String) data.getOrDefault("description", "");
        jdbc.update(
            "INSERT INTO course_schedules (semester_id, name, description, status, entry_count, created_by, deleted) " +
            "VALUES (?, ?, ?, 0, 0, ?, 0)",
            semesterId, name, description, SecurityUtils.requireCurrentUserId());
        Long newId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return Map.of("id", newId, "name", name, "status", 0);
    }

    public void updateSchedulePlan(Long id, Map<String, Object> data) {
        String name = (String) data.get("name");
        String description = (String) data.getOrDefault("description", "");
        jdbc.update("UPDATE course_schedules SET name = ?, description = ?, updated_by = ? WHERE id = ? AND deleted = 0",
            name, description, SecurityUtils.requireCurrentUserId(), id);
    }

    public void deleteSchedulePlan(Long id) {
        jdbc.update("UPDATE course_schedules SET deleted = 1 WHERE id = ?", id);
        // 同时软删关联的条目
        jdbc.update("UPDATE schedule_entries SET deleted = 1 WHERE schedule_id = ?", id);
    }

    public void publishSchedulePlan(Long id) {
        jdbc.update("UPDATE course_schedules SET status = 1, published_at = NOW(), updated_by = ? WHERE id = ? AND deleted = 0",
            SecurityUtils.requireCurrentUserId(), id);
    }

    public void archiveSchedulePlan(Long id) {
        jdbc.update("UPDATE course_schedules SET status = 2, updated_by = ? WHERE id = ? AND deleted = 0",
            SecurityUtils.requireCurrentUserId(), id);
    }

    // ==================== 课表条目 ====================

    public List<Map<String, Object>> listSchedules(Long semesterId, Integer status) {
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
        if (semesterId != null) { sql.append(" AND semester_id = ?"); params.add(semesterId); }
        if (status != null) { sql.append(" AND entry_status = ?"); params.add(status); }
        sql.append(" ORDER BY weekday, start_slot");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> getSchedule(Long id) {
        return jdbc.queryForMap(
            "SELECT id, semester_id AS semesterId, task_id AS taskId, " +
            "course_id AS courseId, org_unit_id AS orgUnitId, teacher_id AS teacherId, " +
            "classroom_id AS classroomId, weekday, start_slot AS startSlot, " +
            "end_slot AS endSlot, start_week AS startWeek, end_week AS endWeek, " +
            "week_type AS weekType, schedule_type AS scheduleType, is_locked AS isLocked, " +
            "entry_status AS entryStatus, conflict_flag AS conflictFlag, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM schedule_entries WHERE id = ? AND deleted = 0", id);
    }

    public Map<String, Object> createSchedule(Map<String, Object> data) {
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

        Map<String, Object> result = new HashMap<>(data);
        result.put("id", id);
        return result;
    }

    public void updateSchedule(Long id, Map<String, Object> data) {
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
        if (sets.isEmpty()) return;
        sets.add("updated_at = NOW()");
        params.add(id);
        jdbc.update(
            "UPDATE schedule_entries SET " + String.join(", ", sets) + " WHERE id = ? AND deleted = 0",
            params.toArray());
    }

    public void deleteSchedule(Long id) {
        jdbc.update("UPDATE schedule_entries SET deleted = 1 WHERE id = ?", id);
    }

    /** 锁定/解锁: 翻转 is_locked 并回查 */
    public Map<String, Object> toggleLock(Long id) {
        jdbc.update("UPDATE schedule_entries SET is_locked = 1 - is_locked WHERE id = ? AND deleted = 0", id);
        Integer locked = jdbc.queryForObject("SELECT is_locked FROM schedule_entries WHERE id = ?", Integer.class, id);
        return Map.of("isLocked", locked != null ? locked : 0);
    }

    /** 批量锁定/解锁 */
    public Map<String, Object> batchLock(List<Number> ids, int lock) {
        if (ids == null || ids.isEmpty()) return Map.of("updated", 0);
        String placeholders = String.join(",", ids.stream().map(i -> "?").toList());
        Object[] args = new Object[ids.size() + 1];
        args[0] = lock;
        for (int i = 0; i < ids.size(); i++) args[i + 1] = ids.get(i).longValue();
        int updated = jdbc.update(
            "UPDATE schedule_entries SET is_locked = ? WHERE id IN (" + placeholders + ") AND deleted = 0", args);
        return Map.of("updated", updated);
    }

    /** 重置排课: 清条目 + 重置任务状态. keepLocked=true 不清锁定条目 */
    public Map<String, Object> resetSchedule(Long semesterId, boolean keepLocked) {
        int cleared;
        if (keepLocked) {
            cleared = jdbc.update(
                "UPDATE schedule_entries SET deleted = 1 WHERE semester_id = ? AND deleted = 0 AND is_locked = 0", semesterId);
        } else {
            cleared = jdbc.update(
                "UPDATE schedule_entries SET deleted = 1 WHERE semester_id = ? AND deleted = 0", semesterId);
        }
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
        return Map.of("cleared", cleared, "reset", reset, "keepLocked", keepLocked);
    }

    public List<Map<String, Object>> getTeachersGroupedByDept() {
        return jdbc.queryForList(
            "SELECT u.id, u.real_name AS realName, u.username, " +
            "COALESCE(ou.unit_name, '未分配') AS deptName, ou.id AS deptId " +
            "FROM users u " +
            "LEFT JOIN org_units ou ON ou.id = u.primary_org_unit_id AND ou.deleted = 0 " +
            "WHERE u.user_type_code = 'TEACHER' AND u.deleted = 0 " +
            "ORDER BY ou.unit_name, u.real_name");
    }

    /** 按班级查课表 — 含 join 名称字段 */
    public List<Map<String, Object>> getSchedulesByClass(Long orgUnitId, Long semesterId) {
        StringBuilder sql = new StringBuilder(scheduleJoinSelect() + " WHERE se.org_unit_id = ? AND se.deleted = 0");
        List<Object> params = new ArrayList<>();
        params.add(orgUnitId);
        if (semesterId != null) { sql.append(" AND se.semester_id = ?"); params.add(semesterId); }
        sql.append(" ORDER BY se.weekday, se.start_slot");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getSchedulesByTeacher(Long teacherId, Long semesterId) {
        StringBuilder sql = new StringBuilder(scheduleJoinSelect() + " WHERE se.teacher_id = ? AND se.deleted = 0");
        List<Object> params = new ArrayList<>();
        params.add(teacherId);
        if (semesterId != null) { sql.append(" AND se.semester_id = ?"); params.add(semesterId); }
        sql.append(" ORDER BY se.weekday, se.start_slot");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getSchedulesByClassroom(Long classroomId, Long semesterId) {
        StringBuilder sql = new StringBuilder(scheduleJoinSelect() + " WHERE se.classroom_id = ? AND se.deleted = 0");
        List<Object> params = new ArrayList<>();
        params.add(classroomId);
        if (semesterId != null) { sql.append(" AND se.semester_id = ?"); params.add(semesterId); }
        sql.append(" ORDER BY se.weekday, se.start_slot");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    private String scheduleJoinSelect() {
        return "SELECT se.id, se.semester_id AS semesterId, se.task_id AS taskId, " +
            "se.course_id AS courseId, se.org_unit_id AS orgUnitId, se.teacher_id AS teacherId, " +
            "se.classroom_id AS classroomId, " +
            "se.weekday AS dayOfWeek, se.start_slot AS periodStart, " +
            "se.end_slot AS periodEnd, se.start_week AS weekStart, se.end_week AS weekEnd, " +
            "se.week_type AS weekType, se.schedule_type AS scheduleType, se.is_locked AS isLocked, " +
            "se.entry_status AS entryStatus, " +
            "c.course_name AS courseName, u.real_name AS teacherName, " +
            "ou.unit_name AS className, " +
            "COALESCE(p.place_code, p.place_name) AS classroomName " +
            "FROM schedule_entries se " +
            "LEFT JOIN courses c ON c.id = se.course_id " +
            "LEFT JOIN users u ON u.id = se.teacher_id " +
            "LEFT JOIN org_units ou ON ou.id = se.org_unit_id " +
            "LEFT JOIN places p ON p.id = se.classroom_id";
    }

    // ==================== 智能排课 / 拖拽 / 冲突 ====================

    /** 自动排课 — 调度服务 + 触发 SCHEDULE_PUBLISHED 事件 */
    public Map<String, Object> autoSchedule(Map<String, Object> params) {
        if (autoSchedulingService == null) {
            throw new IllegalStateException("排课服务未启用");
        }
        Long semesterId = Long.parseLong(params.get("semesterId").toString());
        Map<String, Object> result = autoSchedulingService.autoSchedule(semesterId, params);

        if (triggerService != null) {
            try {
                triggerService.fire(SCHEDULE_PUBLISHED, Map.of(
                    "semesterId", semesterId,
                    "scheduledCount", result.getOrDefault("scheduledCount", 0),
                    "action", "AUTO_SCHEDULE"
                ));
            } catch (Exception ignored) {}
        }
        return result;
    }

    /** 拖拽移动课位: weekday + slot 改写; 可选 classroom */
    public void moveEntry(Long id, int newDay, int newPeriod, Long classroomId) {
        Map<String, Object> entry = jdbc.queryForMap(
            "SELECT end_slot - start_slot as span FROM schedule_entries WHERE id = ? AND deleted = 0", id);
        int span = ((Number) entry.get("span")).intValue();
        int newEndPeriod = newPeriod + span;

        jdbc.update(
            "UPDATE schedule_entries SET weekday = ?, start_slot = ?, end_slot = ? WHERE id = ? AND deleted = 0",
            newDay, newPeriod, newEndPeriod, id);

        if (classroomId != null) {
            jdbc.update("UPDATE schedule_entries SET classroom_id = ? WHERE id = ?", classroomId, id);
        }
    }

    /** 移动冲突检测: 教师 / 班级 双维度 */
    public Map<String, Object> checkMoveConflict(Long entryId, Long semesterId, int newDay, int newPeriod) {
        Map<String, Object> entry = jdbc.queryForMap(
            "SELECT teacher_id, org_unit_id, end_slot - start_slot + 1 as span FROM schedule_entries WHERE id = ?", entryId);
        int span = ((Number) entry.get("span")).intValue();
        int newEndPeriod = newPeriod + span - 1;

        Map<String, Object> result = new HashMap<>();
        boolean hasConflict = false;

        if (entry.get("teacher_id") != null) {
            Long teacherId = ((Number) entry.get("teacher_id")).longValue();
            List<Map<String, Object>> tc = jdbc.queryForList(
                "SELECT id FROM schedule_entries WHERE semester_id=? AND teacher_id=? AND weekday=? " +
                "AND start_slot <= ? AND end_slot >= ? AND id != ? AND deleted=0",
                semesterId, teacherId, newDay, newEndPeriod, newPeriod, entryId);
            if (!tc.isEmpty()) hasConflict = true;
            result.put("teacherConflicts", tc);
        }

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
        return result;
    }

    // ==================== 自习课 ====================

    /**
     * 扫描所有班级空课位, 连续空节合并为 schedule_type=4 自习课条目.
     */
    public Map<String, Object> fillSelfStudy(Long semesterId, int maxPeriods, int maxWeekday,
                                             int startWeek, int endWeek) {
        List<Map<String, Object>> classes = jdbc.queryForList(
            "SELECT id FROM org_units WHERE unit_type = 'CLASS' AND deleted = 0");

        int inserted = 0;
        long userId = SecurityUtils.requireCurrentUserId();

        for (Map<String, Object> cls : classes) {
            Long orgUnitId = ((Number) cls.get("id")).longValue();

            List<Map<String, Object>> occupied = jdbc.queryForList(
                "SELECT weekday, start_slot, end_slot FROM schedule_entries " +
                "WHERE semester_id = ? AND org_unit_id = ? AND deleted = 0 AND entry_status = 1",
                semesterId, orgUnitId);

            Map<Integer, Set<Integer>> occupiedMap = new HashMap<>();
            for (Map<String, Object> row : occupied) {
                int wd = ((Number) row.get("weekday")).intValue();
                int ss = ((Number) row.get("start_slot")).intValue();
                int es = ((Number) row.get("end_slot")).intValue();
                occupiedMap.computeIfAbsent(wd, k -> new HashSet<>());
                for (int p = ss; p <= es; p++) occupiedMap.get(wd).add(p);
            }

            for (int wd = 1; wd <= maxWeekday; wd++) {
                Set<Integer> occ = occupiedMap.getOrDefault(wd, Set.of());
                int runStart = -1;

                for (int p = 1; p <= maxPeriods + 1; p++) {
                    boolean empty = p <= maxPeriods && !occ.contains(p);
                    if (empty && runStart == -1) {
                        runStart = p;
                    } else if (!empty && runStart != -1) {
                        long id = IdWorker.getId();
                        jdbc.update(
                            "INSERT INTO schedule_entries (id, semester_id, task_id, course_id, org_unit_id, " +
                            "teacher_id, classroom_id, weekday, start_slot, end_slot, " +
                            "start_week, end_week, week_type, schedule_type, " +
                            "entry_status, conflict_flag, created_by, created_at, updated_at, deleted) " +
                            "VALUES (?, ?, NULL, NULL, ?, NULL, NULL, ?, ?, ?, ?, ?, 0, 4, 1, 0, ?, NOW(), NOW(), 0)",
                            id, semesterId, orgUnitId, wd, runStart, p - 1,
                            startWeek, endWeek, userId);
                        inserted++;
                        runStart = -1;
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("inserted", inserted);
        result.put("classCount", classes.size());
        return result;
    }

    /** 清除所有自习课条目 */
    public Map<String, Object> clearSelfStudy(Long semesterId) {
        int deleted = jdbc.update(
            "UPDATE schedule_entries SET deleted = 1 WHERE semester_id = ? AND schedule_type = 4 AND deleted = 0",
            semesterId);
        Map<String, Object> result = new HashMap<>();
        result.put("cleared", deleted);
        return result;
    }

    /** 获取学期最后一个教学周号 (week_type=1), fallback 16 */
    public int getSemesterTeachingWeeks(Long semesterId) {
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
