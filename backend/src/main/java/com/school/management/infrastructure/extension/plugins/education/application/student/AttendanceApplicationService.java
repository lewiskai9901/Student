package com.school.management.infrastructure.extension.plugins.education.application.student;

import com.school.management.application.event.TriggerService;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.access.OrgScopeHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.school.management.infrastructure.extension.plugins.education.constants.EducationTriggerPoints.ATTENDANCE_RECORDED;

/**
 * 考勤管理应用服务 (M3.2.2, 2026-05-20).
 * AttendanceController 21 处直 jdbc 全部下沉.
 *
 * 涉及 DB 表:
 * - attendance_records  考勤记录
 * - leave_requests      请假申请
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceApplicationService {

    private final JdbcTemplate jdbc;
    private final OrgScopeHelper orgScopeHelper;

    @Autowired(required = false)
    private TriggerService triggerService;

    // ==================== 考勤记录 CRUD ====================

    @Transactional
    public int createRecord(Long semesterId, Long courseId, Long orgUnitId, Long studentId,
                            String dateStr, Integer period, Integer attendanceType, Integer status,
                            String checkMethod, String remark, Long recordedBy) {
        if (!orgScopeHelper.isOrgAllowed(orgUnitId)) {
            throw new SecurityException("无权在该组织范围内创建考勤记录: orgUnitId=" + orgUnitId);
        }
        return jdbc.update(
            "INSERT INTO attendance_records (semester_id, course_id, org_unit_id, student_id, attendance_date, " +
            "period, attendance_type, status, check_in_time, check_method, remark, recorded_by) " +
            "VALUES (?,?,?,?,?,?,?,?,NOW(),?,?,?)",
            semesterId, courseId, orgUnitId, studentId, dateStr,
            period, attendanceType, status, checkMethod, remark, recordedBy
        );
    }

    public List<Map<String, Object>> listRecords(Long semesterId, Long orgUnitId, Long studentId,
                                                  Long courseId, String date, String startDate, String endDate,
                                                  Integer status, Integer attendanceType, int page, int size) {
        StringBuilder sql = new StringBuilder(
            "SELECT ar.id, ar.semester_id AS semesterId, ar.course_id AS courseId, " +
            "ar.org_unit_id AS orgUnitId, ar.student_id AS studentId, " +
            "ar.attendance_date AS attendanceDate, ar.period, " +
            "ar.attendance_type AS attendanceType, ar.status, " +
            "ar.check_in_time AS checkInTime, ar.check_method AS checkMethod, " +
            "ar.remark, ar.recorded_by AS recordedBy, " +
            "s.name AS studentName, s.student_no AS studentNo, " +
            "c.name AS courseName " +
            "FROM attendance_records ar " +
            "LEFT JOIN user_student s ON ar.student_id = s.id " +
            "LEFT JOIN courses c ON ar.course_id = c.id " +
            "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (semesterId != null) { sql.append(" AND ar.semester_id = ?"); params.add(semesterId); }
        if (orgUnitId != null) { sql.append(" AND ar.org_unit_id = ?"); params.add(orgUnitId); }
        if (studentId != null) { sql.append(" AND ar.student_id = ?"); params.add(studentId); }
        if (courseId != null) { sql.append(" AND ar.course_id = ?"); params.add(courseId); }
        if (date != null) { sql.append(" AND ar.attendance_date = ?"); params.add(date); }
        if (startDate != null) { sql.append(" AND ar.attendance_date >= ?"); params.add(startDate); }
        if (endDate != null) { sql.append(" AND ar.attendance_date <= ?"); params.add(endDate); }
        if (status != null) { sql.append(" AND ar.status = ?"); params.add(status); }
        if (attendanceType != null) { sql.append(" AND ar.attendance_type = ?"); params.add(attendanceType); }

        sql.append(orgScopeHelper.orgScopeClause("ar.org_unit_id"));

        sql.append(" ORDER BY ar.attendance_date DESC, ar.period ASC, s.student_no ASC");

        int offset = (page - 1) * size;
        sql.append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);

        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    public List<Map<String, Object>> getByClass(Long orgUnitId, String date, Long courseId, Integer period) {
        // 1. 获取班级所有学生 (user_student 表无 name / status 列; 姓名在 users, 状态列名 student_status)
        List<Map<String, Object>> students = jdbc.queryForList(
            "SELECT s.id AS studentId, s.student_no AS studentNo, u.real_name AS studentName " +
            "FROM user_student s LEFT JOIN users u ON s.user_id = u.id " +
            "WHERE s.org_unit_id = ? AND s.student_status = 1 AND s.deleted = 0" +
            orgScopeHelper.orgScopeClause("s.org_unit_id") +
            " ORDER BY s.student_no",
            orgUnitId
        );

        // 2. 获取已有考勤记录
        StringBuilder recordSql = new StringBuilder(
            "SELECT student_id AS studentId, status, remark, id AS recordId " +
            "FROM attendance_records WHERE org_unit_id = ? AND attendance_date = ?" +
            orgScopeHelper.orgScopeClause("org_unit_id")
        );
        List<Object> params = new ArrayList<>();
        params.add(orgUnitId);
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
        return result;
    }

    @Transactional
    public void updateRecord(Long id, Integer status, String remark) {
        String scope = orgScopeHelper.orgScopeClause("org_unit_id");
        if (status != null && remark != null) {
            jdbc.update("UPDATE attendance_records SET status = ?, remark = ?, updated_at = NOW() WHERE id = ?" + scope,
                status, remark, id);
        } else if (status != null) {
            jdbc.update("UPDATE attendance_records SET status = ?, updated_at = NOW() WHERE id = ?" + scope,
                status, id);
        } else if (remark != null) {
            jdbc.update("UPDATE attendance_records SET remark = ?, updated_at = NOW() WHERE id = ?" + scope,
                remark, id);
        }
    }

    @Transactional
    public void deleteRecord(Long id) {
        jdbc.update("DELETE FROM attendance_records WHERE id = ?" + orgScopeHelper.orgScopeClause("org_unit_id"), id);
    }

    // ==================== 批量考勤 ====================

    @Transactional
    public int batchRecord(Long semesterId, Long orgUnitId, Long courseId, String dateStr,
                           Integer period, Integer attendanceType, List<Map<String, Object>> students) {
        if (!orgScopeHelper.isOrgAllowed(orgUnitId)) {
            throw new SecurityException("无权在该组织范围内批量录入考勤: orgUnitId=" + orgUnitId);
        }
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
                jdbc.update(
                    "INSERT INTO attendance_records (semester_id, course_id, org_unit_id, student_id, " +
                    "attendance_date, period, attendance_type, status, check_method, remark, recorded_by) " +
                    "VALUES (?,?,?,?,?,?,?,?,'MANUAL',?,?)",
                    semesterId, courseId, orgUnitId, studentId, dateStr, period,
                    attendanceType, status, remark, recordedBy
                );
            }
            // Fire trigger for abnormal attendance (not normal=1)
            if (triggerService != null && status != 1) {
                try {
                    String statusName = status == 2 ? "迟到" : status == 3 ? "早退" : status == 5 ? "旷课" : "异常";
                    String eventHint = status == 2 ? "LATE" : status == 3 ? "EARLY_LEAVE" : status == 5 ? "ABSENCE" : null;
                    if (eventHint != null) {
                        String studentName = "";
                        String className = "";
                        try {
                            Map<String, Object> stuInfo = jdbc.queryForMap(
                                "SELECT s.name, sc.name AS class_name FROM user_student s " +
                                "LEFT JOIN school_classes sc ON s.org_unit_id = sc.id WHERE s.id = ?", studentId);
                            studentName = (String) stuInfo.getOrDefault("name", "");
                            className = (String) stuInfo.getOrDefault("class_name", "");
                        } catch (Exception ignored) {}
                        Map<String, Object> ctx = new HashMap<>();
                        ctx.put("studentId", studentId);
                        ctx.put("studentName", studentName != null ? studentName : "");
                        ctx.put("orgUnitId", orgUnitId != null ? orgUnitId : 0);
                        ctx.put("className", className != null ? className : "");
                        ctx.put("status", status);
                        ctx.put("statusName", statusName);
                        ctx.put("eventTypeHint", eventHint);
                        ctx.put("date", dateStr != null ? dateStr : "");
                        ctx.put("_refType", "attendance_record");
                        triggerService.fire(ATTENDANCE_RECORDED, ctx);
                    }
                } catch (Exception ignored) {}
            }

            count++;
        }
        return count;
    }

    // ==================== 考勤统计 ====================

    public Map<String, Object> getStatistics(Long semesterId, Long orgUnitId,
                                              String startDate, String endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT status, COUNT(*) as cnt FROM attendance_records WHERE semester_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(semesterId);
        if (orgUnitId != null) { sql.append(" AND org_unit_id = ?"); params.add(orgUnitId); }
        if (startDate != null) { sql.append(" AND attendance_date >= ?"); params.add(startDate); }
        if (endDate != null) { sql.append(" AND attendance_date <= ?"); params.add(endDate); }
        sql.append(orgScopeHelper.orgScopeClause("org_unit_id"));
        sql.append(" GROUP BY status");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());
        return aggregateStatusCounts(rows, null);
    }

    public Map<String, Object> getStudentStatistics(Long studentId, Long semesterId,
                                                     String startDate, String endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT status, COUNT(*) as cnt FROM attendance_records WHERE student_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(studentId);
        if (semesterId != null) { sql.append(" AND semester_id = ?"); params.add(semesterId); }
        if (startDate != null) { sql.append(" AND attendance_date >= ?"); params.add(startDate); }
        if (endDate != null) { sql.append(" AND attendance_date <= ?"); params.add(endDate); }
        sql.append(orgScopeHelper.orgScopeClause("org_unit_id"));
        sql.append(" GROUP BY status");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());
        Map<String, Object> stats = aggregateStatusCounts(rows, studentId);

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
        recentSql.append(orgScopeHelper.orgScopeClause("ar.org_unit_id"));
        recentSql.append(" ORDER BY ar.attendance_date DESC, ar.period DESC LIMIT 10");

        stats.put("recentRecords", jdbc.queryForList(recentSql.toString(), recentParams.toArray()));
        return stats;
    }

    private Map<String, Object> aggregateStatusCounts(List<Map<String, Object>> rows, Long studentId) {
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
        if (studentId != null) stats.put("studentId", studentId);
        stats.put("total", total);
        stats.put("present", present);
        stats.put("late", late);
        stats.put("earlyLeave", earlyLeave);
        stats.put("leave", leave);
        stats.put("absent", absent);
        stats.put("attendanceRate", total > 0 ? Math.round((present + late) * 1000.0 / total) / 10.0 : 0);
        stats.put("absentRate", total > 0 ? Math.round(absent * 1000.0 / total) / 10.0 : 0);
        return stats;
    }

    // ==================== 请假管理 ====================

    @Transactional
    public int createLeaveRequest(Long studentId, Integer leaveType, String startDate, String endDate,
                                   Integer startPeriod, Integer endPeriod, String reason, String attachmentUrls) {
        return jdbc.update(
            "INSERT INTO leave_requests (student_id, leave_type, start_date, end_date, " +
            "start_period, end_period, reason, attachment_urls, approval_status) " +
            "VALUES (?,?,?,?,?,?,?,?,0)",
            studentId, leaveType, startDate, endDate, startPeriod, endPeriod, reason, attachmentUrls
        );
    }

    public List<Map<String, Object>> listLeaveRequests(Long studentId, Long orgUnitId, Integer approvalStatus,
                                                        String startDate, String endDate) {
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
            "LEFT JOIN user_student s ON lr.student_id = s.id " +
            "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (studentId != null) { sql.append(" AND lr.student_id = ?"); params.add(studentId); }
        if (orgUnitId != null) { sql.append(" AND s.org_unit_id = ?"); params.add(orgUnitId); }
        if (approvalStatus != null) { sql.append(" AND lr.approval_status = ?"); params.add(approvalStatus); }
        if (startDate != null) { sql.append(" AND lr.start_date >= ?"); params.add(startDate); }
        if (endDate != null) { sql.append(" AND lr.end_date <= ?"); params.add(endDate); }

        sql.append(orgScopeHelper.orgScopeClause("s.org_unit_id"));

        sql.append(" ORDER BY lr.created_at DESC");

        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    @Transactional
    public void approveLeave(Long id, Long approverId, String comment) {
        verifyLeaveOrgAllowed(id);
        jdbc.update(
            "UPDATE leave_requests SET approval_status = 1, approver_id = ?, " +
            "approval_time = NOW(), approval_comment = ? WHERE id = ?",
            approverId, comment, id
        );
    }

    @Transactional
    public void rejectLeave(Long id, Long approverId, String comment) {
        verifyLeaveOrgAllowed(id);
        jdbc.update(
            "UPDATE leave_requests SET approval_status = 2, approver_id = ?, " +
            "approval_time = NOW(), approval_comment = ? WHERE id = ?",
            approverId, comment, id
        );
    }

    /**
     * leave_requests 表无 org_unit_id 列 — 写操作无法直接拼 scope 子句.
     * 通过 student_id 关联 user_student 解析归属 org, 再用 OrgScopeHelper 预校验.
     */
    private void verifyLeaveOrgAllowed(Long leaveRequestId) {
        if (orgScopeHelper.isUnbounded()) {
            return;
        }
        Long orgUnitId;
        try {
            orgUnitId = jdbc.queryForObject(
                "SELECT s.org_unit_id FROM leave_requests lr " +
                "LEFT JOIN user_student s ON lr.student_id = s.id WHERE lr.id = ?",
                Long.class, leaveRequestId);
        } catch (EmptyResultDataAccessException e) {
            throw new SecurityException("请假申请不存在: id=" + leaveRequestId);
        }
        if (!orgScopeHelper.isOrgAllowed(orgUnitId)) {
            throw new SecurityException("无权审批该组织范围内的请假申请: leaveRequestId=" + leaveRequestId);
        }
    }

    public List<Map<String, Object>> pendingLeaves() {
        return jdbc.queryForList(
            "SELECT lr.id, lr.student_id AS studentId, lr.leave_type AS leaveType, " +
            "lr.start_date AS startDate, lr.end_date AS endDate, " +
            "lr.start_period AS startPeriod, lr.end_period AS endPeriod, " +
            "lr.reason, lr.created_at AS createdAt, " +
            "s.name AS studentName, s.student_no AS studentNo, s.org_unit_id AS classId " +
            "FROM leave_requests lr " +
            "LEFT JOIN user_student s ON lr.student_id = s.id " +
            "WHERE lr.approval_status = 0" +
            orgScopeHelper.orgScopeClause("s.org_unit_id") +
            " ORDER BY lr.created_at ASC"
        );
    }

    // ==================== 导出 ====================

    public List<Map<String, Object>> queryExportRecords(Long semesterId, Long orgUnitId,
                                                        String startDate, String endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT ar.attendance_date, ar.period, ar.status, ar.remark, " +
            "s.student_no, s.name AS student_name, " +
            "c.name AS course_name, sc.name AS class_name " +
            "FROM attendance_records ar " +
            "LEFT JOIN user_student s ON s.id = ar.student_id " +
            "LEFT JOIN courses c ON c.id = ar.course_id " +
            "LEFT JOIN school_classes sc ON sc.id = ar.org_unit_id " +
            "WHERE ar.semester_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(semesterId);
        if (orgUnitId != null) { sql.append(" AND ar.org_unit_id = ?"); params.add(orgUnitId); }
        if (startDate != null) { sql.append(" AND ar.attendance_date >= ?"); params.add(startDate); }
        if (endDate != null) { sql.append(" AND ar.attendance_date <= ?"); params.add(endDate); }
        sql.append(orgScopeHelper.orgScopeClause("ar.org_unit_id"));
        sql.append(" ORDER BY ar.attendance_date DESC, sc.name, s.student_no");

        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    // ==================== Helpers ====================

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
