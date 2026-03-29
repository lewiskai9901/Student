package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.ScheduleExportService;
import com.school.management.common.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

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

    // ==================== 课表条目 ====================

    @GetMapping("/schedules")
    public Result<List<Map<String, Object>>> listSchedules(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer status) {

        StringBuilder sql = new StringBuilder(
            "SELECT id, semester_id AS semesterId, task_id AS taskId, " +
            "course_id AS courseId, class_id AS classId, teacher_id AS teacherId, " +
            "classroom_id AS classroomId, weekday, start_slot AS startSlot, " +
            "end_slot AS endSlot, start_week AS startWeek, end_week AS endWeek, " +
            "week_type AS weekType, schedule_type AS scheduleType, " +
            "entry_status AS entryStatus, conflict_flag AS conflictFlag, " +
            "created_at AS createdAt " +
            "FROM schedule_entries WHERE deleted = 0"
        );
        if (semesterId != null) sql.append(" AND semester_id = ").append(semesterId);
        if (status != null) sql.append(" AND entry_status = ").append(status);
        sql.append(" ORDER BY weekday, start_slot");

        return Result.success(jdbc.queryForList(sql.toString()));
    }

    @GetMapping("/schedules/{id}")
    public Result<Map<String, Object>> getSchedule(@PathVariable Long id) {
        Map<String, Object> entry = jdbc.queryForMap(
            "SELECT id, semester_id AS semesterId, task_id AS taskId, " +
            "course_id AS courseId, class_id AS classId, teacher_id AS teacherId, " +
            "classroom_id AS classroomId, weekday, start_slot AS startSlot, " +
            "end_slot AS endSlot, start_week AS startWeek, end_week AS endWeek, " +
            "week_type AS weekType, schedule_type AS scheduleType, " +
            "entry_status AS entryStatus, conflict_flag AS conflictFlag, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM schedule_entries WHERE id = ? AND deleted = 0", id
        );
        return Result.success(entry);
    }

    @PostMapping("/schedules")
    public Result<Map<String, Object>> createSchedule(@RequestBody Map<String, Object> data) {
        long id = System.currentTimeMillis();
        Long semesterId = data.get("semesterId") != null ? ((Number) data.get("semesterId")).longValue() : null;
        Long taskId = data.get("taskId") != null ? ((Number) data.get("taskId")).longValue() : null;
        Long courseId = data.get("courseId") != null ? ((Number) data.get("courseId")).longValue() : null;
        Long classId = data.get("classId") != null ? ((Number) data.get("classId")).longValue() : null;
        Long teacherId = data.get("teacherId") != null ? ((Number) data.get("teacherId")).longValue() : null;
        Long classroomId = data.get("classroomId") != null ? ((Number) data.get("classroomId")).longValue() : null;
        Integer weekday = data.get("weekday") != null ? ((Number) data.get("weekday")).intValue() : null;
        Integer startSlot = data.get("startSlot") != null ? ((Number) data.get("startSlot")).intValue() : null;
        Integer endSlot = data.get("endSlot") != null ? ((Number) data.get("endSlot")).intValue() : null;
        Integer startWeek = data.get("startWeek") != null ? ((Number) data.get("startWeek")).intValue() : 1;
        Integer endWeek = data.get("endWeek") != null ? ((Number) data.get("endWeek")).intValue() : 16;
        Integer weekType = data.get("weekType") != null ? ((Number) data.get("weekType")).intValue() : 0;
        Integer scheduleType = data.get("scheduleType") != null ? ((Number) data.get("scheduleType")).intValue() : 1;

        jdbc.update(
            "INSERT INTO schedule_entries (id, semester_id, task_id, course_id, class_id, " +
            "teacher_id, classroom_id, weekday, start_slot, end_slot, " +
            "start_week, end_week, week_type, schedule_type, " +
            "entry_status, conflict_flag, created_by, created_at, updated_at, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, 1, NOW(), NOW(), 0)",
            id, semesterId, taskId, courseId, classId,
            teacherId, classroomId, weekday, startSlot, endSlot,
            startWeek, endWeek, weekType, scheduleType
        );

        data.put("id", id);
        return Result.success(data);
    }

    @PutMapping("/schedules/{id}")
    public Result<Void> updateSchedule(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long teacherId = data.get("teacherId") != null ? ((Number) data.get("teacherId")).longValue() : null;
        Long classroomId = data.get("classroomId") != null ? ((Number) data.get("classroomId")).longValue() : null;
        Integer weekday = data.get("weekday") != null ? ((Number) data.get("weekday")).intValue() : null;
        Integer startSlot = data.get("startSlot") != null ? ((Number) data.get("startSlot")).intValue() : null;
        Integer endSlot = data.get("endSlot") != null ? ((Number) data.get("endSlot")).intValue() : null;
        Integer startWeek = data.get("startWeek") != null ? ((Number) data.get("startWeek")).intValue() : null;
        Integer endWeek = data.get("endWeek") != null ? ((Number) data.get("endWeek")).intValue() : null;
        Integer weekType = data.get("weekType") != null ? ((Number) data.get("weekType")).intValue() : null;

        jdbc.update(
            "UPDATE schedule_entries SET teacher_id = ?, classroom_id = ?, weekday = ?, " +
            "start_slot = ?, end_slot = ?, start_week = ?, end_week = ?, " +
            "week_type = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            teacherId, classroomId, weekday, startSlot, endSlot,
            startWeek, endWeek, weekType, id
        );
        return Result.success();
    }

    @DeleteMapping("/schedules/{id}")
    public Result<Void> deleteSchedule(@PathVariable Long id) {
        jdbc.update("UPDATE schedule_entries SET deleted = 1 WHERE id = ?", id);
        return Result.success();
    }

    @GetMapping("/schedules/by-class/{classId}")
    public Result<List<Map<String, Object>>> getSchedulesByClass(
            @PathVariable Long classId,
            @RequestParam(required = false) Long semesterId) {

        StringBuilder sql = new StringBuilder(
            "SELECT se.id, se.semester_id AS semesterId, se.task_id AS taskId, " +
            "se.course_id AS courseId, se.class_id AS classId, se.teacher_id AS teacherId, " +
            "se.classroom_id AS classroomId, se.weekday, se.start_slot AS startSlot, " +
            "se.end_slot AS endSlot, se.start_week AS startWeek, se.end_week AS endWeek, " +
            "se.week_type AS weekType, se.schedule_type AS scheduleType, " +
            "se.entry_status AS entryStatus, " +
            "c.course_name AS courseName, u.real_name AS teacherName " +
            "FROM schedule_entries se " +
            "LEFT JOIN courses c ON c.id = se.course_id " +
            "LEFT JOIN users u ON u.id = se.teacher_id " +
            "WHERE se.class_id = ? AND se.deleted = 0"
        );
        List<Object> params = new ArrayList<>();
        params.add(classId);
        if (semesterId != null) {
            sql.append(" AND se.semester_id = ?");
            params.add(semesterId);
        }
        sql.append(" ORDER BY se.weekday, se.start_slot");

        return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
    }

    @GetMapping("/schedules/by-teacher/{teacherId}")
    public Result<List<Map<String, Object>>> getSchedulesByTeacher(
            @PathVariable Long teacherId,
            @RequestParam(required = false) Long semesterId) {

        StringBuilder sql = new StringBuilder(
            "SELECT se.id, se.semester_id AS semesterId, se.task_id AS taskId, " +
            "se.course_id AS courseId, se.class_id AS classId, se.teacher_id AS teacherId, " +
            "se.classroom_id AS classroomId, se.weekday, se.start_slot AS startSlot, " +
            "se.end_slot AS endSlot, se.start_week AS startWeek, se.end_week AS endWeek, " +
            "se.week_type AS weekType, se.schedule_type AS scheduleType, " +
            "se.entry_status AS entryStatus, " +
            "c.course_name AS courseName, u.real_name AS teacherName " +
            "FROM schedule_entries se " +
            "LEFT JOIN courses c ON c.id = se.course_id " +
            "LEFT JOIN users u ON u.id = se.teacher_id " +
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
    public Result<List<Map<String, Object>>> getSchedulesByClassroom(
            @PathVariable Long classroomId,
            @RequestParam(required = false) Long semesterId) {

        StringBuilder sql = new StringBuilder(
            "SELECT se.id, se.semester_id AS semesterId, se.task_id AS taskId, " +
            "se.course_id AS courseId, se.class_id AS classId, se.teacher_id AS teacherId, " +
            "se.classroom_id AS classroomId, se.weekday, se.start_slot AS startSlot, " +
            "se.end_slot AS endSlot, se.start_week AS startWeek, se.end_week AS endWeek, " +
            "se.week_type AS weekType, se.schedule_type AS scheduleType, " +
            "se.entry_status AS entryStatus, " +
            "c.course_name AS courseName, u.real_name AS teacherName " +
            "FROM schedule_entries se " +
            "LEFT JOIN courses c ON c.id = se.course_id " +
            "LEFT JOIN users u ON u.id = se.teacher_id " +
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
    public Result<Map<String, Object>> autoSchedule(@RequestBody Map<String, Object> params) {
        if (autoSchedulingService == null) {
            return Result.error("排课服务未启用");
        }
        Long semesterId = Long.parseLong(params.get("semesterId").toString());
        Map<String, Object> result = autoSchedulingService.autoSchedule(semesterId, params);
        return Result.success(result);
    }

    @PostMapping("/schedules/{id}/move")
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
    public Result<Map<String, Object>> checkMoveConflict(@RequestBody Map<String, Object> body) {
        Long entryId = Long.parseLong(body.get("entryId").toString());
        Long semesterId = Long.parseLong(body.get("semesterId").toString());
        int newDay = Integer.parseInt(body.get("dayOfWeek").toString());
        int newPeriod = Integer.parseInt(body.get("periodStart").toString());

        Map<String, Object> entry = jdbc.queryForMap(
            "SELECT teacher_id, class_id, end_slot - start_slot + 1 as span FROM schedule_entries WHERE id = ?", entryId);
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
        if (entry.get("class_id") != null) {
            Long classId = ((Number) entry.get("class_id")).longValue();
            List<Map<String, Object>> cc = jdbc.queryForList(
                "SELECT id FROM schedule_entries WHERE semester_id=? AND class_id=? AND weekday=? " +
                "AND start_slot <= ? AND end_slot >= ? AND id != ? AND deleted=0",
                semesterId, classId, newDay, newEndPeriod, newPeriod, entryId);
            if (!cc.isEmpty()) hasConflict = true;
            result.put("classConflicts", cc);
        }

        result.put("hasConflict", hasConflict);
        return Result.success(result);
    }

    // ==================== 课表导出 ====================

    @GetMapping("/schedules/export/class/{classId}")
    public void exportClassSchedule(
            @PathVariable Long classId,
            @RequestParam Long semesterId,
            HttpServletResponse response) throws java.io.IOException {
        byte[] data = exportService.exportClassSchedule(semesterId, classId);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=class_schedule.xlsx");
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }

    @GetMapping("/schedules/export/teacher/{teacherId}")
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
    public Result<Map<String, Object>> createAdjustment(@RequestBody Map<String, Object> data) {
        long id = System.currentTimeMillis();
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

        Long applicantId = data.get("applicantId") != null ? ((Number) data.get("applicantId")).longValue() : 1L;
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
    public Result<Void> approveAdjustment(@PathVariable Long id) {
        jdbc.update(
            "UPDATE schedule_adjustments SET approval_status = 1, approver_id = 1, " +
            "approval_time = NOW(), updated_at = NOW() WHERE id = ? AND deleted = 0", id
        );
        return Result.success();
    }

    @PostMapping("/adjustments/{id}/reject")
    public Result<Void> rejectAdjustment(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        String comment = (String) data.get("approvalComment");
        jdbc.update(
            "UPDATE schedule_adjustments SET approval_status = 2, approver_id = 1, " +
            "approval_time = NOW(), approval_comment = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            comment, id
        );
        return Result.success();
    }

    @PostMapping("/adjustments/{id}/execute")
    public Result<Void> executeAdjustment(@PathVariable Long id) {
        jdbc.update(
            "UPDATE schedule_adjustments SET executed = 1, executed_at = NOW(), " +
            "updated_at = NOW() WHERE id = ? AND deleted = 0", id
        );
        return Result.success();
    }

    @PostMapping("/adjustments/{id}/cancel")
    public Result<Void> cancelAdjustment(@PathVariable Long id) {
        jdbc.update(
            "UPDATE schedule_adjustments SET approval_status = 3, " +
            "updated_at = NOW() WHERE id = ? AND deleted = 0", id
        );
        return Result.success();
    }

    @GetMapping("/adjustments/my-applications")
    public Result<Map<String, Object>> myApplications(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long applicantId = 1L; // placeholder
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
}
