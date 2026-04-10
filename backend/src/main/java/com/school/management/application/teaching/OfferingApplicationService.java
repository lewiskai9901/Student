package com.school.management.application.teaching;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.domain.teaching.model.offering.SemesterOffering;
import com.school.management.domain.teaching.model.offering.ClassCourseAssignment;
import com.school.management.domain.teaching.model.task.TaskStatus;
import com.school.management.domain.teaching.model.task.TeachingTask;
import com.school.management.domain.teaching.repository.SemesterOfferingRepository;
import com.school.management.domain.teaching.repository.ClassCourseAssignmentRepository;
import com.school.management.domain.teaching.repository.TeachingTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OfferingApplicationService {
    private final SemesterOfferingRepository offeringRepo;
    private final ClassCourseAssignmentRepository assignmentRepo;
    private final TeachingTaskRepository taskRepo;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOfferingsWithCourse(Long semesterId) {
        return jdbc.queryForList(
            "SELECT o.id, o.semester_id AS semesterId, o.course_id AS courseId, " +
            "c.course_code AS courseCode, c.course_name AS courseName, " +
            "o.applicable_grade AS applicableGrade, o.weekly_hours AS weeklyHours, " +
            "o.start_week AS startWeek, o.end_week AS endWeek, " +
            "o.course_category AS courseCategory, o.course_type AS courseType, " +
            "o.allow_combined AS allowCombined, o.max_combined_classes AS maxCombinedClasses, " +
            "o.allow_walking AS allowWalking, o.status, o.remark " +
            "FROM semester_course_offerings o " +
            "LEFT JOIN courses c ON c.id = o.course_id " +
            "WHERE o.semester_id = ? AND o.deleted = 0 " +
            "ORDER BY o.id", semesterId);
    }

    public SemesterOffering createOffering(Map<String, Object> data, Long userId) {
        SemesterOffering offering = SemesterOffering.create(
            toLong(data.get("semesterId")),
            toLong(data.get("courseId")),
            (String) data.get("applicableGrade"),
            toInt(data.get("weeklyHours")),
            toInt(data.getOrDefault("startWeek", 1)),
            toInt(data.get("endWeek")),
            userId
        );
        // Apply optional fields
        if (data.containsKey("courseCategory") || data.containsKey("allowCombined")) {
            offering.update(
                toInt(data.get("weeklyHours")),
                toInt(data.getOrDefault("startWeek", 1)),
                toInt(data.get("endWeek")),
                toInt(data.get("courseCategory")),
                toInt(data.get("courseType")),
                toBool(data.get("allowCombined")),
                toInt(data.getOrDefault("maxCombinedClasses", 2)),
                toBool(data.get("allowWalking")),
                (String) data.get("remark")
            );
        }
        return offeringRepo.save(offering);
    }

    public SemesterOffering updateOffering(Long id, Map<String, Object> data) {
        SemesterOffering offering = offeringRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("开课计划不存在: " + id));
        offering.update(
            toInt(data.get("weeklyHours")),
            toInt(data.get("startWeek")),
            toInt(data.get("endWeek")),
            toInt(data.get("courseCategory")),
            toInt(data.get("courseType")),
            toBool(data.get("allowCombined")),
            toInt(data.get("maxCombinedClasses")),
            toBool(data.get("allowWalking")),
            (String) data.get("remark")
        );
        return offeringRepo.save(offering);
    }

    public void deleteOffering(Long id) {
        offeringRepo.deleteById(id);
    }

    public void confirmOffering(Long id) {
        SemesterOffering offering = offeringRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("开课计划不存在: " + id));
        offering.confirm();
        offeringRepo.save(offering);
    }

    // --- Class Assignments ---

    public List<ClassCourseAssignment> listAssignments(Long semesterId, Long orgUnitId) {
        if (orgUnitId != null) {
            return assignmentRepo.findBySemesterIdAndClassId(semesterId, orgUnitId);
        }
        return assignmentRepo.findBySemesterId(semesterId);
    }

    public ClassCourseAssignment createAssignment(Map<String, Object> data) {
        ClassCourseAssignment a = ClassCourseAssignment.create(
            toLong(data.get("semesterId")),
            toLong(data.get("orgUnitId")),
            toLong(data.get("offeringId")),
            toLong(data.get("courseId")),
            toInt(data.get("weeklyHours")),
            toInt(data.get("studentCount"))
        );
        return assignmentRepo.save(a);
    }

    public void deleteAssignment(Long id) {
        assignmentRepo.deleteById(id);
    }

    public void batchConfirmAssignments(Long semesterId, Long orgUnitId) {
        List<ClassCourseAssignment> assignments = assignmentRepo.findBySemesterIdAndClassId(semesterId, orgUnitId);
        for (ClassCourseAssignment a : assignments) {
            a.confirm();
            assignmentRepo.save(a);
        }
    }

    public int importFromPlan(Long semesterId, Long planId, Long userId) {
        Map<String, Object> plan = jdbc.queryForMap(
            "SELECT grade_year FROM curriculum_plans WHERE id = ? AND deleted = 0", planId);
        String grade = plan.get("grade_year") != null ? plan.get("grade_year").toString() + "级" : "全年级";

        List<Map<String, Object>> planCourses = jdbc.queryForList(
            "SELECT course_id, weekly_hours, total_hours, course_category, course_type " +
            "FROM curriculum_plan_courses WHERE plan_id = ?", planId);

        List<SemesterOffering> existing = offeringRepo.findBySemesterId(semesterId);
        Set<String> existingKeys = new HashSet<>();
        for (SemesterOffering o : existing) {
            existingKeys.add(o.getCourseId() + "_" + o.getApplicableGrade());
        }

        int created = 0;
        for (Map<String, Object> pc : planCourses) {
            Long courseId = Long.valueOf(pc.get("course_id").toString());
            if (existingKeys.contains(courseId + "_" + grade)) continue;

            Integer weeklyHours = pc.get("weekly_hours") != null ? Integer.valueOf(pc.get("weekly_hours").toString()) : 2;
            SemesterOffering offering = SemesterOffering.create(
                semesterId, courseId, grade, weeklyHours, 1, null, userId);
            offeringRepo.save(offering);
            created++;
        }
        return created;
    }

    public int generateTasksFromAssignments(Long semesterId, Long userId) {
        List<ClassCourseAssignment> assignments = assignmentRepo.findBySemesterId(semesterId);
        assignments = assignments.stream()
            .filter(a -> a.getStatus() != null && a.getStatus() == 1)
            .collect(Collectors.toList());

        List<Map<String, Object>> existingTasks = jdbc.queryForList(
            "SELECT course_id, org_unit_id FROM teaching_tasks WHERE semester_id = ? AND deleted = 0",
            semesterId);
        Set<String> existingKeys = new HashSet<>();
        for (Map<String, Object> t : existingTasks) {
            existingKeys.add(t.get("course_id") + "_" + t.get("org_unit_id"));
        }

        int created = 0;
        for (ClassCourseAssignment a : assignments) {
            String key = a.getCourseId() + "_" + a.getOrgUnitId();
            if (existingKeys.contains(key)) continue;

            SemesterOffering offering = a.getOfferingId() != null ?
                offeringRepo.findById(a.getOfferingId()).orElse(null) : null;

            int weeklyHours = a.getWeeklyHours() != null ? a.getWeeklyHours() :
                (offering != null && offering.getWeeklyHours() != null ? offering.getWeeklyHours() : 2);
            int startWeek = offering != null && offering.getStartWeek() != null ? offering.getStartWeek() : 1;
            Integer endWeek = offering != null ? offering.getEndWeek() : null;

            // 查班级学生数
            int studentCount = 0;
            try {
                Long cnt = jdbc.queryForObject(
                    "SELECT COUNT(1) FROM students WHERE org_unit_id = ? AND deleted = 0", Long.class, a.getOrgUnitId());
                if (cnt != null) studentCount = cnt.intValue();
            } catch (Exception ignored) {}

            long taskId = IdWorker.getId();
            TeachingTask task = TeachingTask.create(
                "TT" + taskId, semesterId, a.getCourseId(), a.getOrgUnitId(),
                studentCount,
                weeklyHours, null, startWeek, endWeek,
                TaskStatus.CONFIRMED, null, userId);
            task.setId(taskId);
            taskRepo.save(task);
            created++;
        }
        return created;
    }

    // Helper methods for type conversion from Map
    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }

    private Boolean toBool(Object val) {
        if (val == null) return null;
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(val.toString());
    }
}
