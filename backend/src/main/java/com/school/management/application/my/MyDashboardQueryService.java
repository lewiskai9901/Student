package com.school.management.application.my;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /my/dashboard 首页查询服务。
 *
 * <p>所有方法以 userId 作为强制过滤条件，由 Controller 通过
 * {@code SecurityUtils.requireCurrentUserId()} 传入。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MyDashboardQueryService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 首页头部四个数字。为避免"编造"指标：
     *   todayLessons — 今天有效课次（同 getTodaySchedule 过滤）
     *   weeklyHoursCurrent — 本周截止今日、非取消/调走的课次数
     *   weeklyHoursTotal — 本周内总计（同过滤）
     *   substituteRequests — 代课待办数（同 getSubstituteTasks 过滤）
     */
    public DashboardSummary getSummary(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        Integer todayLessons = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schedule_instances " +
                "WHERE teacher_id = ? AND actual_date = ? AND deleted = 0 AND status IN (0,3,4)",
                Integer.class, userId, today);

        Integer weekTotal = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schedule_instances " +
                "WHERE teacher_id = ? AND actual_date BETWEEN ? AND ? AND deleted = 0 AND status IN (0,3,4)",
                Integer.class, userId, weekStart, weekEnd);

        Integer weekCurrent = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schedule_instances " +
                "WHERE teacher_id = ? AND actual_date BETWEEN ? AND ? AND deleted = 0 AND status IN (0,3,4)",
                Integer.class, userId, weekStart, today);

        Integer substituteRequests = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM schedule_adjustments " +
                "WHERE new_teacher_id = ? AND adjustment_type = 3 AND executed = 0 " +
                "  AND approval_status IN (0,1) AND deleted = 0",
                Integer.class, userId);

        return new DashboardSummary(
                nz(todayLessons),
                nz(weekCurrent),
                nz(weekTotal),
                nz(substituteRequests)
        );
    }

    private static int nz(Integer v) { return v == null ? 0 : v; }

    /**
     * 今日课表 — 从 schedule_instances 查询（物化实况表，原生含代课/调课）。
     * 返回字段尽量扁平，前端一次渲染。
     */
    public List<TodayLesson> getTodaySchedule(Long userId, LocalDate date) {
        String sql =
                "SELECT si.id, si.start_slot, si.end_slot, si.status, " +
                "       c.course_name, " +
                "       ou.unit_name AS class_name, " +
                "       COALESCE(p.place_name, p.place_code, '') AS classroom_name " +
                "FROM schedule_instances si " +
                "LEFT JOIN courses c ON c.id = si.course_id " +
                "LEFT JOIN org_units ou ON ou.id = si.org_unit_id " +
                "LEFT JOIN places p ON p.id = si.classroom_id " +
                "WHERE si.teacher_id = ? " +
                "  AND si.actual_date = ? " +
                "  AND si.deleted = 0 " +
                "  AND si.status IN (0, 3, 4) " +  // 正常 / 补课 / 代课 — 排除已取消/调走
                "ORDER BY si.start_slot";

        return jdbcTemplate.query(sql, (rs, i) -> {
            Integer status = rs.getObject("status", Integer.class);
            Integer startSlot = rs.getObject("start_slot", Integer.class);
            Integer endSlot = rs.getObject("end_slot", Integer.class);
            return new TodayLesson(
                    rs.getLong("id"),
                    startSlot,
                    endSlot,
                    null,  // startTime — 前端通过 slot 映射，未来可扩展 period_configs
                    null,  // endTime
                    rs.getString("course_name"),
                    rs.getString("class_name"),
                    rs.getString("classroom_name"),
                    status,
                    status != null && status == 0   // 正常课次才可签到
            );
        }, userId, date);
    }

    /**
     * 我的班级 — 当前任职（班主任 / 副班主任 / 辅导员 / 科任老师）所在班级。
     * 一个老师可能兼多角色，同一班级取权重最高的角色（HEAD_TEACHER > DEPUTY_HEAD_TEACHER > COUNSELOR > SUBJECT_TEACHER）。
     * 学生数由子查询聚合，subjects 由 schedule_instances 区分课程推断（没排课则为空）。
     */
    public List<MyClass> getMyClasses(Long userId) {
        // Step 1: 拉取该老师当前生效任职记录（去重后的班级集合）
        String assignSql =
                "SELECT ta.org_unit_id, ta.role_type, ou.unit_name " +
                "FROM teacher_assignments ta " +
                "LEFT JOIN org_units ou ON ou.id = ta.org_unit_id " +
                "WHERE ta.teacher_id = ? " +
                "  AND ta.status = 'ACTIVE' " +
                "  AND ta.deleted = 0";

        // (orgUnitId → best role)
        Map<Long, String> classRoles = new HashMap<>();
        Map<Long, String> classNames = new HashMap<>();
        jdbcTemplate.query(assignSql, rs -> {
            long orgUnitId = rs.getLong("org_unit_id");
            String role = rs.getString("role_type");
            String name = rs.getString("unit_name");
            classRoles.merge(orgUnitId, role == null ? "" : role, MyDashboardQueryService::preferHigherRole);
            classNames.putIfAbsent(orgUnitId, name);
        }, userId);

        if (classRoles.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 2: 学生数（一次 SQL 拉全，内存 group）
        String studentSql =
                "SELECT org_unit_id, COUNT(*) AS cnt " +
                "FROM students " +
                "WHERE deleted = 0 AND org_unit_id IN (" +
                inPlaceholders(classRoles.size()) + ") " +
                "GROUP BY org_unit_id";
        Map<Long, Integer> counts = new HashMap<>();
        jdbcTemplate.query(studentSql, rs -> {
            counts.put(rs.getLong("org_unit_id"), rs.getInt("cnt"));
        }, classRoles.keySet().toArray());

        // Step 3: 授课科目 — 从 schedule_instances DISTINCT 课程名
        String subjectSql =
                "SELECT DISTINCT si.org_unit_id, c.course_name " +
                "FROM schedule_instances si " +
                "LEFT JOIN courses c ON c.id = si.course_id " +
                "WHERE si.teacher_id = ? " +
                "  AND si.deleted = 0 " +
                "  AND c.course_name IS NOT NULL " +
                "  AND si.org_unit_id IN (" + inPlaceholders(classRoles.size()) + ")";
        Map<Long, List<String>> subjects = new HashMap<>();
        Object[] args = new Object[classRoles.size() + 1];
        args[0] = userId;
        int i = 1;
        for (Long id : classRoles.keySet()) args[i++] = id;
        jdbcTemplate.query(subjectSql, rs -> {
            subjects.computeIfAbsent(rs.getLong("org_unit_id"), k -> new ArrayList<>())
                    .add(rs.getString("course_name"));
        }, args);

        List<MyClass> result = new ArrayList<>(classRoles.size());
        for (Map.Entry<Long, String> e : classRoles.entrySet()) {
            Long classId = e.getKey();
            String role = e.getValue();
            result.add(new MyClass(
                    classId,
                    classNames.get(classId),
                    counts.getOrDefault(classId, 0),
                    "HEAD_TEACHER".equals(role),
                    subjects.getOrDefault(classId, Collections.emptyList())
            ));
        }
        result.sort((a, b) -> {
            // 班主任优先，其次按 classId
            if (a.isHeadTeacher() != b.isHeadTeacher()) return a.isHeadTeacher() ? -1 : 1;
            return Long.compare(a.classId(), b.classId());
        });
        return result;
    }

    static String preferHigherRole(String a, String b) {
        return roleRank(a) <= roleRank(b) ? a : b;
    }

    static int roleRank(String role) {
        if (role == null) return 99;
        return switch (role) {
            case "HEAD_TEACHER" -> 0;
            case "DEPUTY_HEAD", "DEPUTY_HEAD_TEACHER" -> 1;
            case "COUNSELOR" -> 2;
            case "SUBJECT_TEACHER" -> 3;
            default -> 99;
        };
    }

    private static String inPlaceholders(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) { if (i > 0) sb.append(','); sb.append('?'); }
        return sb.toString();
    }

    /**
     * 分配给我的代课请求 — schedule_adjustments 中 new_teacher_id = me, adjustment_type=3(代课),
     * 未执行 (executed=0)，非已拒绝/撤回 (approval_status IN 0,1)。
     * JOIN schedule_entries → courses 得课程名，JOIN users 得申请人姓名。
     */
    public List<SubstituteTask> getSubstituteTasks(Long userId) {
        String sql =
                "SELECT sa.id, c.course_name, " +
                "       COALESCE(sa.new_date, sa.original_date) AS scheduled_date, " +
                "       COALESCE(sa.new_slot, sa.original_slot) AS start_slot, " +
                "       COALESCE(sa.new_slot, sa.original_slot) AS end_slot, " +
                "       u.real_name AS requester_name, u.username AS requester_username, " +
                "       sa.apply_time " +
                "FROM schedule_adjustments sa " +
                "LEFT JOIN schedule_entries se ON se.id = sa.original_entry_id " +
                "LEFT JOIN courses c ON c.id = se.course_id " +
                "LEFT JOIN users u ON u.id = sa.applicant_id " +
                "WHERE sa.new_teacher_id = ? " +
                "  AND sa.adjustment_type = 3 " +
                "  AND sa.executed = 0 " +
                "  AND sa.approval_status IN (0, 1) " +
                "  AND sa.deleted = 0 " +
                "ORDER BY COALESCE(sa.new_date, sa.original_date), sa.apply_time DESC";

        return jdbcTemplate.query(sql, (rs, i) -> {
            java.sql.Date d = rs.getDate("scheduled_date");
            java.sql.Timestamp applyTime = rs.getTimestamp("apply_time");
            String requesterName = rs.getString("requester_name");
            if (requesterName == null || requesterName.isBlank()) {
                requesterName = rs.getString("requester_username");
            }
            Integer startSlot = rs.getObject("start_slot", Integer.class);
            Integer endSlot = rs.getObject("end_slot", Integer.class);
            return new SubstituteTask(
                    rs.getLong("id"),
                    rs.getString("course_name"),
                    d != null ? d.toLocalDate() : null,
                    startSlot,
                    endSlot,
                    requesterName,
                    applyTime != null ? applyTime.toInstant().toString() : null
            );
        }, userId);
    }
}
