package com.school.management.interfaces.rest.student;

import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 考勤管理 REST Controller
 *
 * 使用 JdbcTemplate 直接操作 DB 表:
 * - attendance_records  考勤记录
 * - leave_requests      请假申请
 */
@Slf4j
@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final JdbcTemplate jdbc;

    // ==================== 考勤记录 CRUD ====================

    /**
     * 创建考勤记录（单个或批量）
     */
    @PostMapping("/records")
    public Result<Map<String, Object>> createRecord(@RequestBody Map<String, Object> body) {
        Long recordedBy = SecurityUtils.getCurrentUserId();
        Long semesterId = toLong(body.get("semesterId"));
        Long courseId = toLong(body.get("courseId"));
        Long classId = toLong(body.get("classId"));
        Long studentId = toLong(body.get("studentId"));
        String dateStr = (String) body.get("attendanceDate");
        Integer period = toInt(body.get("period"));
        Integer attendanceType = toInt(body.get("attendanceType"));
        if (attendanceType == null) attendanceType = 1;
        Integer status = toInt(body.get("status"));
        String checkMethod = body.get("checkMethod") != null ? (String) body.get("checkMethod") : "MANUAL";
        String remark = (String) body.get("remark");

        jdbc.update(
            "INSERT INTO attendance_records (semester_id, course_id, class_id, student_id, attendance_date, " +
            "period, attendance_type, status, check_in_time, check_method, remark, recorded_by) " +
            "VALUES (?,?,?,?,?,?,?,?,NOW(),?,?,?)",
            semesterId, courseId, classId, studentId, dateStr,
            period, attendanceType, status, checkMethod, remark, recordedBy
        );
        return Result.success(Map.of("created", 1));
    }

    /**
     * 查询考勤记录（支持筛选）
     */
    @GetMapping("/records")
    public Result<List<Map<String, Object>>> listRecords(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer attendanceType,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer size) {

        StringBuilder sql = new StringBuilder(
            "SELECT ar.id, ar.semester_id AS semesterId, ar.course_id AS courseId, " +
            "ar.class_id AS classId, ar.student_id AS studentId, " +
            "ar.attendance_date AS attendanceDate, ar.period, " +
            "ar.attendance_type AS attendanceType, ar.status, " +
            "ar.check_in_time AS checkInTime, ar.check_method AS checkMethod, " +
            "ar.remark, ar.recorded_by AS recordedBy, " +
            "s.name AS studentName, s.student_no AS studentNo, " +
            "c.name AS courseName " +
            "FROM attendance_records ar " +
            "LEFT JOIN students s ON ar.student_id = s.id " +
            "LEFT JOIN courses c ON ar.course_id = c.id " +
            "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (semesterId != null) { sql.append(" AND ar.semester_id = ?"); params.add(semesterId); }
        if (classId != null) { sql.append(" AND ar.class_id = ?"); params.add(classId); }
        if (studentId != null) { sql.append(" AND ar.student_id = ?"); params.add(studentId); }
        if (courseId != null) { sql.append(" AND ar.course_id = ?"); params.add(courseId); }
        if (date != null) { sql.append(" AND ar.attendance_date = ?"); params.add(date); }
        if (startDate != null) { sql.append(" AND ar.attendance_date >= ?"); params.add(startDate); }
        if (endDate != null) { sql.append(" AND ar.attendance_date <= ?"); params.add(endDate); }
        if (status != null) { sql.append(" AND ar.status = ?"); params.add(status); }
        if (attendanceType != null) { sql.append(" AND ar.attendance_type = ?"); params.add(attendanceType); }

        sql.append(" ORDER BY ar.attendance_date DESC, ar.period ASC, s.student_no ASC");

        int offset = (page - 1) * size;
        sql.append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());
        return Result.success(rows);
    }

    /**
     * 班级考勤视图：某天某班所有学生的考勤
     */
    @GetMapping("/records/by-class")
    public Result<List<Map<String, Object>>> getByClass(
            @RequestParam Long classId,
            @RequestParam String date,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Integer period) {

        // 1. 获取班级所有学生
        List<Map<String, Object>> students = jdbc.queryForList(
            "SELECT id AS studentId, student_no AS studentNo, name AS studentName " +
            "FROM students WHERE class_id = ? AND status = 0 ORDER BY student_no",
            classId
        );

        // 2. 获取已有考勤记录
        StringBuilder recordSql = new StringBuilder(
            "SELECT student_id AS studentId, status, remark, id AS recordId " +
            "FROM attendance_records WHERE class_id = ? AND attendance_date = ?"
        );
        List<Object> params = new ArrayList<>();
        params.add(classId);
        params.add(date);
        if (courseId != null) { recordSql.append(" AND course_id = ?"); params.add(courseId); }
        if (period != null) { recordSql.append(" AND period = ?"); params.add(period); }

        List<Map<String, Object>> records = jdbc.queryForList(recordSql.toString(), params.toArray());
        Map<Long, Map<String, Object>> recordMap = new HashMap<>();
        for (Map<String, Object> r : records) {
            Long sid = ((Number) r.get("studentId")).longValue();
            recordMap.put(sid, r);
        }

        // 3. 合并结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> stu : students) {
            Map<String, Object> row = new HashMap<>(stu);
            Long sid = ((Number) stu.get("studentId")).longValue();
            Map<String, Object> rec = recordMap.get(sid);
            if (rec != null) {
                row.put("status", rec.get("status"));
                row.put("remark", rec.get("remark"));
                row.put("recordId", rec.get("recordId"));
            } else {
                row.put("status", null);
                row.put("remark", null);
                row.put("recordId", null);
            }
            result.add(row);
        }
        return Result.success(result);
    }

    /**
     * 修改考勤状态
     */
    @PutMapping("/records/{id}")
    public Result<Void> updateRecord(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer status = toInt(body.get("status"));
        String remark = (String) body.get("remark");

        if (status != null && remark != null) {
            jdbc.update("UPDATE attendance_records SET status = ?, remark = ?, updated_at = NOW() WHERE id = ?",
                status, remark, id);
        } else if (status != null) {
            jdbc.update("UPDATE attendance_records SET status = ?, updated_at = NOW() WHERE id = ?",
                status, id);
        } else if (remark != null) {
            jdbc.update("UPDATE attendance_records SET remark = ?, updated_at = NOW() WHERE id = ?",
                remark, id);
        }
        return Result.success();
    }

    /**
     * 删除考勤记录
     */
    @DeleteMapping("/records/{id}")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        jdbc.update("DELETE FROM attendance_records WHERE id = ?", id);
        return Result.success();
    }

    // ==================== 批量考勤 ====================

    /**
     * 批量考勤（一个班级一次课的所有学生）
     */
    @PostMapping("/batch")
    @Transactional
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> batchRecord(@RequestBody Map<String, Object> body) {
        Long semesterId = toLong(body.get("semesterId"));
        Long classId = toLong(body.get("classId"));
        Long courseId = toLong(body.get("courseId"));
        String dateStr = (String) body.get("date");
        Integer period = toInt(body.get("period"));
        Integer attendanceType = toInt(body.get("attendanceType"));
        if (attendanceType == null) attendanceType = 1;
        List<Map<String, Object>> students = (List<Map<String, Object>>) body.get("students");

        Long recordedBy = SecurityUtils.getCurrentUserId();
        int count = 0;

        for (Map<String, Object> s : students) {
            Long studentId = toLong(s.get("studentId"));
            int status = toInt(s.get("status"));
            String remark = (String) s.get("remark");

            // Check if record already exists for this student/date/period/course
            Integer existing = countSafe(
                "SELECT COUNT(*) FROM attendance_records WHERE student_id=? AND attendance_date=? AND period" +
                (period != null ? "=?" : " IS NULL") + " AND course_id" + (courseId != null ? "=?" : " IS NULL"),
                buildExistParams(studentId, dateStr, period, courseId)
            );

            if (existing > 0) {
                // Update existing
                StringBuilder updateSql = new StringBuilder(
                    "UPDATE attendance_records SET status=?, remark=?, recorded_by=?, updated_at=NOW() " +
                    "WHERE student_id=? AND attendance_date=? AND period"
                );
                List<Object> updateParams = new ArrayList<>();
                updateParams.add(status);
                updateParams.add(remark);
                updateParams.add(recordedBy);
                updateParams.add(studentId);
                updateParams.add(dateStr);
                if (period != null) {
                    updateSql.append("=?");
                    updateParams.add(period);
                } else {
                    updateSql.append(" IS NULL");
                }
                updateSql.append(" AND course_id");
                if (courseId != null) {
                    updateSql.append("=?");
                    updateParams.add(courseId);
                } else {
                    updateSql.append(" IS NULL");
                }
                jdbc.update(updateSql.toString(), updateParams.toArray());
            } else {
                // Insert new
                jdbc.update(
                    "INSERT INTO attendance_records (semester_id, course_id, class_id, student_id, " +
                    "attendance_date, period, attendance_type, status, check_method, remark, recorded_by) " +
                    "VALUES (?,?,?,?,?,?,?,?,'MANUAL',?,?)",
                    semesterId, courseId, classId, studentId, dateStr, period,
                    attendanceType, status, remark, recordedBy
                );
            }
            count++;
        }
        return Result.success(Map.of("recorded", count));
    }

    // ==================== 考勤统计 ====================

    /**
     * 考勤统计（出勤率、迟到率等）
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        StringBuilder sql = new StringBuilder(
            "SELECT status, COUNT(*) as cnt FROM attendance_records WHERE semester_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(semesterId);
        if (classId != null) { sql.append(" AND class_id = ?"); params.add(classId); }
        if (startDate != null) { sql.append(" AND attendance_date >= ?"); params.add(startDate); }
        if (endDate != null) { sql.append(" AND attendance_date <= ?"); params.add(endDate); }
        sql.append(" GROUP BY status");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());

        int total = 0, present = 0, late = 0, earlyLeave = 0, leave = 0, absent = 0;
        for (Map<String, Object> row : rows) {
            int st = ((Number) row.get("status")).intValue();
            int cnt = ((Number) row.get("cnt")).intValue();
            total += cnt;
            switch (st) {
                case 1: present = cnt; break;
                case 2: late = cnt; break;
                case 3: earlyLeave = cnt; break;
                case 4: leave = cnt; break;
                case 5: absent = cnt; break;
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("present", present);
        stats.put("late", late);
        stats.put("earlyLeave", earlyLeave);
        stats.put("leave", leave);
        stats.put("absent", absent);
        stats.put("attendanceRate", total > 0 ? Math.round((present + late) * 1000.0 / total) / 10.0 : 0);
        stats.put("absentRate", total > 0 ? Math.round(absent * 1000.0 / total) / 10.0 : 0);
        return Result.success(stats);
    }

    /**
     * 个人考勤统计
     */
    @GetMapping("/statistics/student/{studentId}")
    public Result<Map<String, Object>> getStudentStatistics(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        StringBuilder sql = new StringBuilder(
            "SELECT status, COUNT(*) as cnt FROM attendance_records WHERE student_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(studentId);
        if (semesterId != null) { sql.append(" AND semester_id = ?"); params.add(semesterId); }
        if (startDate != null) { sql.append(" AND attendance_date >= ?"); params.add(startDate); }
        if (endDate != null) { sql.append(" AND attendance_date <= ?"); params.add(endDate); }
        sql.append(" GROUP BY status");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());

        int total = 0, present = 0, late = 0, earlyLeave = 0, leave = 0, absent = 0;
        for (Map<String, Object> row : rows) {
            int st = ((Number) row.get("status")).intValue();
            int cnt = ((Number) row.get("cnt")).intValue();
            total += cnt;
            switch (st) {
                case 1: present = cnt; break;
                case 2: late = cnt; break;
                case 3: earlyLeave = cnt; break;
                case 4: leave = cnt; break;
                case 5: absent = cnt; break;
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("studentId", studentId);
        stats.put("total", total);
        stats.put("present", present);
        stats.put("late", late);
        stats.put("earlyLeave", earlyLeave);
        stats.put("leave", leave);
        stats.put("absent", absent);
        stats.put("attendanceRate", total > 0 ? Math.round((present + late) * 1000.0 / total) / 10.0 : 0);
        stats.put("absentRate", total > 0 ? Math.round(absent * 1000.0 / total) / 10.0 : 0);

        // 最近10条记录
        StringBuilder recentSql = new StringBuilder(
            "SELECT ar.id, ar.attendance_date AS attendanceDate, ar.period, ar.status, " +
            "ar.remark, c.name AS courseName " +
            "FROM attendance_records ar LEFT JOIN courses c ON ar.course_id = c.id " +
            "WHERE ar.student_id = ?"
        );
        List<Object> recentParams = new ArrayList<>();
        recentParams.add(studentId);
        if (semesterId != null) { recentSql.append(" AND ar.semester_id = ?"); recentParams.add(semesterId); }
        recentSql.append(" ORDER BY ar.attendance_date DESC, ar.period DESC LIMIT 10");

        stats.put("recentRecords", jdbc.queryForList(recentSql.toString(), recentParams.toArray()));
        return Result.success(stats);
    }

    // ==================== 请假管理 ====================

    /**
     * 提交请假申请
     */
    @PostMapping("/leave-requests")
    public Result<Map<String, Object>> createLeaveRequest(@RequestBody Map<String, Object> body) {
        Long studentId = toLong(body.get("studentId"));
        Integer leaveType = toInt(body.get("leaveType"));
        String startDate = (String) body.get("startDate");
        String endDate = (String) body.get("endDate");
        Integer startPeriod = toInt(body.get("startPeriod"));
        Integer endPeriod = toInt(body.get("endPeriod"));
        String reason = (String) body.get("reason");
        String attachmentUrls = body.get("attachmentUrls") != null ? body.get("attachmentUrls").toString() : null;

        jdbc.update(
            "INSERT INTO leave_requests (student_id, leave_type, start_date, end_date, " +
            "start_period, end_period, reason, attachment_urls, approval_status) " +
            "VALUES (?,?,?,?,?,?,?,?,0)",
            studentId, leaveType, startDate, endDate, startPeriod, endPeriod, reason, attachmentUrls
        );
        return Result.success(Map.of("created", 1));
    }

    /**
     * 查询请假列表
     */
    @GetMapping("/leave-requests")
    public Result<List<Map<String, Object>>> listLeaveRequests(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Integer approvalStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        StringBuilder sql = new StringBuilder(
            "SELECT lr.id, lr.student_id AS studentId, lr.leave_type AS leaveType, " +
            "lr.start_date AS startDate, lr.end_date AS endDate, " +
            "lr.start_period AS startPeriod, lr.end_period AS endPeriod, " +
            "lr.reason, lr.attachment_urls AS attachmentUrls, " +
            "lr.approval_status AS approvalStatus, lr.approver_id AS approverId, " +
            "lr.approval_time AS approvalTime, lr.approval_comment AS approvalComment, " +
            "lr.created_at AS createdAt, " +
            "s.name AS studentName, s.student_no AS studentNo " +
            "FROM leave_requests lr " +
            "LEFT JOIN students s ON lr.student_id = s.id " +
            "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (studentId != null) { sql.append(" AND lr.student_id = ?"); params.add(studentId); }
        if (classId != null) { sql.append(" AND s.class_id = ?"); params.add(classId); }
        if (approvalStatus != null) { sql.append(" AND lr.approval_status = ?"); params.add(approvalStatus); }
        if (startDate != null) { sql.append(" AND lr.start_date >= ?"); params.add(startDate); }
        if (endDate != null) { sql.append(" AND lr.end_date <= ?"); params.add(endDate); }

        sql.append(" ORDER BY lr.created_at DESC");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());
        return Result.success(rows);
    }

    /**
     * 审批通过
     */
    @PostMapping("/leave-requests/{id}/approve")
    public Result<Void> approveLeave(@PathVariable Long id,
                                      @RequestBody(required = false) Map<String, Object> body) {
        Long approverId = SecurityUtils.getCurrentUserId();
        String comment = body != null ? (String) body.get("comment") : null;
        jdbc.update(
            "UPDATE leave_requests SET approval_status = 1, approver_id = ?, " +
            "approval_time = NOW(), approval_comment = ? WHERE id = ?",
            approverId, comment, id
        );
        return Result.success();
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/leave-requests/{id}/reject")
    public Result<Void> rejectLeave(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, Object> body) {
        Long approverId = SecurityUtils.getCurrentUserId();
        String comment = body != null ? (String) body.get("comment") : null;
        jdbc.update(
            "UPDATE leave_requests SET approval_status = 2, approver_id = ?, " +
            "approval_time = NOW(), approval_comment = ? WHERE id = ?",
            approverId, comment, id
        );
        return Result.success();
    }

    /**
     * 待审批列表
     */
    @GetMapping("/leave-requests/pending")
    public Result<List<Map<String, Object>>> pendingLeaves() {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT lr.id, lr.student_id AS studentId, lr.leave_type AS leaveType, " +
            "lr.start_date AS startDate, lr.end_date AS endDate, " +
            "lr.start_period AS startPeriod, lr.end_period AS endPeriod, " +
            "lr.reason, lr.created_at AS createdAt, " +
            "s.name AS studentName, s.student_no AS studentNo, s.class_id AS classId " +
            "FROM leave_requests lr " +
            "LEFT JOIN students s ON lr.student_id = s.id " +
            "WHERE lr.approval_status = 0 " +
            "ORDER BY lr.created_at ASC"
        );
        return Result.success(rows);
    }

    // ==================== Helper Methods ====================

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.valueOf(val.toString());
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.valueOf(val.toString());
    }

    private Integer countSafe(String sql, Object... args) {
        try {
            Integer count = jdbc.queryForObject(sql, Integer.class, args);
            return count != null ? count : 0;
        } catch (EmptyResultDataAccessException e) {
            return 0;
        }
    }

    private Object[] buildExistParams(Long studentId, String dateStr, Integer period, Long courseId) {
        List<Object> params = new ArrayList<>();
        params.add(studentId);
        params.add(dateStr);
        if (period != null) params.add(period);
        if (courseId != null) params.add(courseId);
        return params.toArray();
    }
}
