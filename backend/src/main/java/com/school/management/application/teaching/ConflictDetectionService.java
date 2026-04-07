package com.school.management.application.teaching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.teaching.model.scheduling.ConstraintLevel;
import com.school.management.domain.teaching.model.scheduling.ConstraintType;
import com.school.management.domain.teaching.model.scheduling.ScheduleConflictRecord;
import com.school.management.domain.teaching.model.scheduling.SchedulingConstraint;
import com.school.management.domain.teaching.repository.ScheduleConflictRecordRepository;
import com.school.management.domain.teaching.repository.SchedulingConstraintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class ConflictDetectionService {
    private final ScheduleConflictRecordRepository conflictRepo;
    private final SchedulingConstraintRepository constraintRepo;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Pre-scheduling feasibility check.
     * Checks: teacher time sufficiency, constraint contradictions, room capacity.
     */
    public Map<String, Object> feasibilityCheck(Long semesterId) {
        List<Map<String, Object>> blockingIssues = new ArrayList<>();
        List<Map<String, Object>> warnings = new ArrayList<>();
        int passedChecks = 0;

        // 1. Check teacher time slots availability
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
            "SELECT tt.teacher_id, u.real_name as teacher_name, " +
            "SUM(t.weekly_hours) as total_hours " +
            "FROM teaching_task_teachers tt " +
            "JOIN teaching_tasks t ON t.id = tt.task_id " +
            "LEFT JOIN users u ON u.id = tt.teacher_id " +
            "WHERE t.semester_id = ? AND t.deleted = 0 " +
            "GROUP BY tt.teacher_id", semesterId);

        List<SchedulingConstraint> allConstraints = constraintRepo.findEnabledBySemesterId(semesterId);
        List<SchedulingConstraint> globalConstraints = new ArrayList<>();
        Map<Long, List<SchedulingConstraint>> teacherConstraints = new HashMap<>();

        for (SchedulingConstraint c : allConstraints) {
            if (c.getConstraintLevel() == ConstraintLevel.GLOBAL) {
                globalConstraints.add(c);
            } else if (c.getConstraintLevel() == ConstraintLevel.TEACHER && c.getTargetId() != null) {
                teacherConstraints.computeIfAbsent(c.getTargetId(), k -> new ArrayList<>()).add(c);
            }
        }

        // Count available slots per teacher (5 days x 10 periods = 50 max)
        for (Map<String, Object> task : tasks) {
            Long teacherId = ((Number) task.get("teacher_id")).longValue();
            String teacherName = (String) task.getOrDefault("teacher_name", "Unknown");
            int requiredHours = ((Number) task.get("total_hours")).intValue();

            int availableSlots = countAvailableSlots(globalConstraints,
                teacherConstraints.getOrDefault(teacherId, Collections.emptyList()));

            if (availableSlots < requiredHours) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("type", "TEACHER_TIME_INSUFFICIENT");
                issue.put("target", teacherName);
                issue.put("description", String.format("%s 可用时间槽(%d个)不足以排完所有课时(%d节)",
                    teacherName, availableSlots, requiredHours));
                issue.put("suggestion", "减少该教师的禁排约束或减少课时分配");
                blockingIssues.add(issue);
            } else {
                passedChecks++;
            }
        }

        // 2. Basic room capacity check
        List<Map<String, Object>> oversizedClasses = jdbcTemplate.queryForList(
            "SELECT tc.class_name, tc.student_count, tc.required_capacity " +
            "FROM teaching_classes tc " +
            "WHERE tc.semester_id = ? AND tc.deleted = 0 AND tc.student_count > 0 " +
            "AND tc.student_count > COALESCE((SELECT MAX(capacity) FROM classrooms WHERE deleted = 0), 999)",
            semesterId);

        for (Map<String, Object> cls : oversizedClasses) {
            Map<String, Object> issue = new HashMap<>();
            issue.put("type", "ROOM_CAPACITY_INSUFFICIENT");
            issue.put("target", cls.get("class_name"));
            issue.put("description", String.format("教学班 %s (%d人) 超过最大教室容量",
                cls.get("class_name"), cls.get("student_count")));
            issue.put("suggestion", "拆分教学班或增加大容量教室");
            blockingIssues.add(issue);
        }
        passedChecks++;

        Map<String, Object> report = new HashMap<>();
        report.put("blockingIssues", blockingIssues);
        report.put("warnings", warnings);
        report.put("passedChecks", passedChecks);
        return report;
    }

    /**
     * Post-scheduling conflict detection.
     * Checks: teacher, classroom, class conflicts.
     */
    public List<ScheduleConflictRecord> detectConflicts(Long semesterId) {
        String batch = "DET-" + System.currentTimeMillis();
        List<ScheduleConflictRecord> conflicts = new ArrayList<>();

        // 1. Teacher conflicts: same teacher, same day+period, overlapping weeks
        List<Map<String, Object>> teacherConflicts = jdbcTemplate.queryForList(
            "SELECT e1.id as id1, e2.id as id2, e1.teacher_id, " +
            "e1.weekday, e1.start_slot, e1.end_slot " +
            "FROM schedule_entries e1 " +
            "JOIN schedule_entries e2 ON e1.teacher_id = e2.teacher_id " +
            "  AND e1.weekday = e2.weekday " +
            "  AND e1.start_slot <= e2.end_slot AND e1.end_slot >= e2.start_slot " +
            "  AND e1.start_week <= e2.end_week AND e1.end_week >= e2.start_week " +
            "  AND e1.id < e2.id " +
            "WHERE e1.semester_id = ? AND e1.deleted = 0 AND e2.deleted = 0",
            semesterId);

        for (Map<String, Object> tc : teacherConflicts) {
            ScheduleConflictRecord r = ScheduleConflictRecord.create(
                semesterId, batch, 1, "TEACHER_CONFLICT", 1,
                String.format("教师冲突: 周%d第%d-%d节存在重叠排课",
                    tc.get("weekday"), tc.get("start_slot"), tc.get("end_slot")),
                null,
                ((Number) tc.get("id1")).longValue(),
                ((Number) tc.get("id2")).longValue(),
                null
            );
            conflicts.add(conflictRepo.save(r));
        }

        // 2. Classroom conflicts
        List<Map<String, Object>> roomConflicts = jdbcTemplate.queryForList(
            "SELECT e1.id as id1, e2.id as id2, e1.classroom_id, " +
            "e1.weekday, e1.start_slot " +
            "FROM schedule_entries e1 " +
            "JOIN schedule_entries e2 ON e1.classroom_id = e2.classroom_id " +
            "  AND e1.weekday = e2.weekday " +
            "  AND e1.start_slot <= e2.end_slot AND e1.end_slot >= e2.start_slot " +
            "  AND e1.start_week <= e2.end_week AND e1.end_week >= e2.start_week " +
            "  AND e1.id < e2.id " +
            "WHERE e1.semester_id = ? AND e1.deleted = 0 AND e2.deleted = 0 " +
            "AND e1.classroom_id IS NOT NULL",
            semesterId);

        for (Map<String, Object> rc : roomConflicts) {
            ScheduleConflictRecord r = ScheduleConflictRecord.create(
                semesterId, batch, 1, "CLASSROOM_CONFLICT", 1,
                String.format("教室冲突: 周%d第%d节教室被重复占用",
                    rc.get("weekday"), rc.get("start_slot")),
                null,
                ((Number) rc.get("id1")).longValue(),
                ((Number) rc.get("id2")).longValue(),
                null
            );
            conflicts.add(conflictRepo.save(r));
        }

        // 3. Class conflicts (same admin class)
        List<Map<String, Object>> classConflicts = jdbcTemplate.queryForList(
            "SELECT e1.id as id1, e2.id as id2, e1.org_unit_id, " +
            "e1.weekday, e1.start_slot " +
            "FROM schedule_entries e1 " +
            "JOIN schedule_entries e2 ON e1.org_unit_id = e2.org_unit_id " +
            "  AND e1.weekday = e2.weekday " +
            "  AND e1.start_slot <= e2.end_slot AND e1.end_slot >= e2.start_slot " +
            "  AND e1.start_week <= e2.end_week AND e1.end_week >= e2.start_week " +
            "  AND e1.id < e2.id " +
            "WHERE e1.semester_id = ? AND e1.deleted = 0 AND e2.deleted = 0",
            semesterId);

        for (Map<String, Object> cc : classConflicts) {
            ScheduleConflictRecord r = ScheduleConflictRecord.create(
                semesterId, batch, 1, "CLASS_CONFLICT", 1,
                String.format("班级冲突: 周%d第%d节班级被重复排课",
                    cc.get("weekday"), cc.get("start_slot")),
                null,
                ((Number) cc.get("id1")).longValue(),
                ((Number) cc.get("id2")).longValue(),
                null
            );
            conflicts.add(conflictRepo.save(r));
        }

        return conflicts;
    }

    public List<ScheduleConflictRecord> listConflicts(Long semesterId, Integer status) {
        if (status != null) {
            return conflictRepo.findBySemesterIdAndStatus(semesterId, status);
        }
        return conflictRepo.findBySemesterId(semesterId);
    }

    public void resolveConflict(Long id, String note, Long userId) {
        ScheduleConflictRecord r = conflictRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("冲突记录不存在: " + id));
        r.resolve(note, userId);
        conflictRepo.save(r);
    }

    public void ignoreConflict(Long id, String note, Long userId) {
        ScheduleConflictRecord r = conflictRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("冲突记录不存在: " + id));
        r.ignore(note, userId);
        conflictRepo.save(r);
    }

    private int countAvailableSlots(List<SchedulingConstraint> global, List<SchedulingConstraint> teacher) {
        int available = 0;
        for (int day = 1; day <= 5; day++) { // Mon-Fri
            for (int period = 1; period <= 10; period++) {
                boolean forbidden = false;
                for (SchedulingConstraint c : global) {
                    if (isTimeForbidden(c, day, period)) { forbidden = true; break; }
                }
                if (!forbidden) {
                    for (SchedulingConstraint c : teacher) {
                        if (isTimeForbidden(c, day, period)) { forbidden = true; break; }
                    }
                }
                if (!forbidden) available++;
            }
        }
        return available;
    }

    @SuppressWarnings("unchecked")
    private boolean isTimeForbidden(SchedulingConstraint c, int day, int period) {
        if (c.getConstraintType() != ConstraintType.TIME_FORBIDDEN || !Boolean.TRUE.equals(c.getIsHard())) {
            return false;
        }
        try {
            Map<String, Object> params = objectMapper.readValue(c.getParams(), Map.class);
            List<Integer> days = (List<Integer>) params.getOrDefault("days", Collections.emptyList());
            List<Integer> periods = (List<Integer>) params.getOrDefault("periods", Collections.emptyList());
            return (days.isEmpty() || days.contains(day)) && (periods.isEmpty() || periods.contains(period));
        } catch (Exception e) {
            return false;
        }
    }
}
