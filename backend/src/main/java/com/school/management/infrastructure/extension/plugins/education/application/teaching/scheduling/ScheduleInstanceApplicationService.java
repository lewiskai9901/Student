package com.school.management.infrastructure.extension.plugins.education.application.teaching.scheduling;

import com.school.management.infrastructure.extension.plugins.education.application.teaching.InstanceGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 排课实例应用服务 — 实况课表 / 实例生成 / 课时统计 / 代课 / 取消 / 恢复
 *
 * 从 {@code TeachingScheduleController} 拆分而来 (M3.2.5 — 拆 service 不拆 controller).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleInstanceApplicationService {

    private final JdbcTemplate jdbc;

    @Autowired(required = false)
    private InstanceGenerationService instanceService;

    /** 实况课表查询 — 含调课链接 / 代课原教师 */
    public List<Map<String, Object>> listInstances(Long semesterId,
                                                   String date,
                                                   Integer weekNumber,
                                                   Long teacherId,
                                                   Long orgUnitId,
                                                   Long classroomId) {
        StringBuilder sql = new StringBuilder(
            "SELECT si.id, si.entry_id AS entryId, si.actual_date AS actualDate, si.weekday, " +
            "si.week_number AS weekNumber, si.start_slot AS startSlot, si.end_slot AS endSlot, " +
            "si.status, si.cancel_reason AS cancelReason, si.source_type AS sourceType, " +
            "si.actual_hours AS actualHours, si.teacher_id AS teacherId, " +
            "si.original_teacher_id AS originalTeacherId, si.classroom_id AS classroomId, " +
            "c.course_name AS courseName, c.course_code AS courseCode, " +
            "ou.unit_name AS className, " +
            "COALESCE(p.place_code, p.place_name, '') AS classroomName, " +
            "u.real_name AS teacherName, " +
            "(SELECT JSON_OBJECT('date', adj.new_date, 'slot', adj.new_slot, 'weekday', adj.new_weekday) " +
            " FROM schedule_adjustments adj " +
            " WHERE adj.original_entry_id = si.entry_id " +
            "   AND adj.original_date = si.actual_date AND adj.original_slot = si.start_slot " +
            "   AND adj.deleted = 0 AND adj.executed = 1 LIMIT 1) AS movedTo, " +
            "(SELECT JSON_OBJECT('date', adj.original_date, 'slot', adj.original_slot, 'weekday', adj.original_weekday) " +
            " FROM schedule_adjustments adj " +
            " WHERE adj.original_entry_id = si.entry_id " +
            "   AND adj.new_date = si.actual_date AND adj.new_slot = si.start_slot " +
            "   AND adj.deleted = 0 AND adj.executed = 1 LIMIT 1) AS movedFrom, " +
            "(SELECT u2.real_name FROM users u2 WHERE u2.id = si.original_teacher_id) AS originalTeacherName " +
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

        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    /** 调实例生成服务: 全学期展开实况 */
    public Map<String, Object> generateInstances(Long semesterId) {
        if (instanceService == null) {
            throw new IllegalStateException("服务未启用");
        }
        return instanceService.generateInstances(semesterId);
    }

    /** 应用某个校历事件到已生成的实况 */
    public int applyCalendarEvent(Long eventId) {
        if (instanceService == null) {
            throw new IllegalStateException("服务未启用");
        }
        return instanceService.applyCalendarEvent(eventId);
    }

    public boolean isInstanceServiceEnabled() {
        return instanceService != null;
    }

    /** 课时统计 — 按教师/班级/课程/教室分组 */
    public Map<String, Object> hoursStatistics(Long semesterId, String groupBy, String period,
                                               Integer weekNumber, Integer month) {
        String groupCol;
        String nameJoin;
        String nameCol;
        switch (groupBy) {
            case "teacher":
                groupCol = "si.teacher_id";
                nameCol = "COALESCE(u.real_name, u.username, CONCAT('教师',si.teacher_id))";
                nameJoin = "LEFT JOIN users u ON u.id = si.teacher_id";
                break;
            case "class":
                groupCol = "si.org_unit_id";
                nameCol = "cl.class_name";
                nameJoin = "LEFT JOIN classes cl ON cl.id = si.org_unit_id";
                break;
            case "course":
                groupCol = "si.course_id";
                nameCol = "c.course_name";
                nameJoin = "LEFT JOIN courses c ON c.id = si.course_id";
                break;
            case "classroom":
                groupCol = "si.classroom_id";
                nameCol = "COALESCE(p.place_name, CONCAT('教室',si.classroom_id))";
                nameJoin = "LEFT JOIN places p ON p.id = si.classroom_id";
                break;
            default:
                throw new IllegalArgumentException("无效的 groupBy 参数");
        }

        StringBuilder where = new StringBuilder("si.semester_id = ? AND si.deleted = 0");
        List<Object> params = new ArrayList<>();
        params.add(semesterId);

        if ("week".equals(period) && weekNumber != null) {
            where.append(" AND si.week_number = ?");
            params.add(weekNumber);
        }
        if ("month".equals(period) && month != null) {
            where.append(" AND MONTH(si.actual_date) = ?");
            params.add(month);
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

        double totalActual = items.stream()
            .mapToDouble(i -> ((Number) i.getOrDefault("actualHours", 0)).doubleValue())
            .sum();
        return Map.of(
            "items", items,
            "summary", Map.of("count", items.size(), "totalActualHours", totalActual)
        );
    }

    /** 代课: 原教师入档 / 切换为代课状态 */
    public void substituteTeacher(Long instanceId, Long newTeacherId, String reason) {
        jdbc.update(
            "UPDATE schedule_instances SET original_teacher_id = teacher_id, teacher_id = ?, " +
            "status = 4, cancel_reason = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            newTeacherId, reason, instanceId);
    }

    /** 临时取消实例 */
    public void cancelInstance(Long instanceId, String reason) {
        jdbc.update(
            "UPDATE schedule_instances SET status = 1, cancel_reason = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            reason, instanceId);
    }

    /** 恢复实例为正常 */
    public void restoreInstance(Long instanceId) {
        jdbc.update(
            "UPDATE schedule_instances SET status = 0, cancel_reason = NULL, " +
            "original_teacher_id = NULL, updated_at = NOW() WHERE id = ? AND deleted = 0",
            instanceId);
    }
}
