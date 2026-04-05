package com.school.management.application.teaching;

import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 教务工作流编排服务
 *
 * 串联: 培养方案 → 开课计划 → 教学任务 → 考试安排 → 成绩批次
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingWorkflowService {

    private final JdbcTemplate jdbc;

    // ==================== 年级-学期映射 ====================

    /**
     * 为当前学期自动生成所有年级的映射
     * 根据每个年级的 enrollmentYear 和学期的 startYear/semesterType 计算 programSemester
     */
    @Transactional
    public int generateCohortSemesterMappings(Long semesterId) {
        log.info("自动生成年级-学期映射: semesterId={}", semesterId);

        Map<String, Object> sem = jdbc.queryForMap(
            "SELECT start_year, semester_type FROM semesters WHERE id = ? AND deleted = 0", semesterId);
        int semStartYear = ((Number) sem.get("start_year")).intValue();
        int semType = ((Number) sem.get("semester_type")).intValue();

        List<Map<String, Object>> cohorts = jdbc.queryForList(
            "SELECT id, enrollment_year, schooling_years FROM grades WHERE status != 'GRADUATED' AND deleted = 0");

        int count = 0;
        for (Map<String, Object> c : cohorts) {
            Long cohortId = toLong(c.get("id"));
            int enrollYear = ((Number) c.get("enrollment_year")).intValue();
            int schoolingYears = c.get("schooling_years") != null ? ((Number) c.get("schooling_years")).intValue() : 3;
            int programSem = (semStartYear - enrollYear) * 2 + semType;

            // 跳过不合理的学期（还没入学或已超学制）
            if (programSem < 1 || programSem > schoolingYears * 2) continue;

            // 检查是否已存在
            Long exists = jdbc.queryForObject(
                "SELECT COUNT(1) FROM cohort_semester_mapping WHERE cohort_id = ? AND semester_id = ?",
                Long.class, cohortId, semesterId);
            if (exists != null && exists > 0) continue;

            // 查找匹配的培养方案
            Long planId = null;
            try {
                planId = jdbc.queryForObject(
                    "SELECT id FROM curriculum_plans WHERE grade_year = ? AND status = 1 AND deleted = 0 LIMIT 1",
                    Long.class, enrollYear);
            } catch (Exception ignored) {}

            jdbc.update(
                "INSERT INTO cohort_semester_mapping (cohort_id, semester_id, program_semester, plan_id, status) " +
                "VALUES (?, ?, ?, ?, 1)",
                cohortId, semesterId, programSem, planId);
            count++;
        }

        log.info("生成年级-学期映射 {} 条", count);
        return count;
    }

    /**
     * 从培养方案自动导入开课计划（基于年级-学期映射）
     * 根据 cohort_semester_mapping 找到每个年级本学期应开的课程，自动创建 semester_course_offerings
     */
    @Transactional
    public int generateOfferingsFromMappings(Long semesterId, Long createdBy) {
        log.info("从培养方案自动导入开课计划: semesterId={}", semesterId);

        List<Map<String, Object>> mappings = jdbc.queryForList(
            "SELECT m.cohort_id, m.program_semester, m.plan_id, g.grade_name " +
            "FROM cohort_semester_mapping m " +
            "JOIN grades g ON g.id = m.cohort_id " +
            "WHERE m.semester_id = ? AND m.status = 1 AND m.plan_id IS NOT NULL",
            semesterId);

        int count = 0;
        for (Map<String, Object> m : mappings) {
            Long planId = toLong(m.get("plan_id"));
            int programSem = ((Number) m.get("program_semester")).intValue();
            String gradeName = (String) m.get("grade_name");

            // 查培养方案中该学期的课程
            List<Map<String, Object>> courses = jdbc.queryForList(
                "SELECT pc.course_id, pc.weekly_hours, pc.course_category, pc.course_type, pc.id AS plan_course_id " +
                "FROM curriculum_plan_courses pc " +
                "WHERE pc.plan_id = ? AND pc.semester_number = ?",
                planId, programSem);

            for (Map<String, Object> c : courses) {
                Long courseId = toLong(c.get("course_id"));

                // 检查是否已存在
                Long exists = jdbc.queryForObject(
                    "SELECT COUNT(1) FROM semester_course_offerings " +
                    "WHERE semester_id = ? AND course_id = ? AND applicable_grade = ? AND deleted = 0",
                    Long.class, semesterId, courseId, gradeName);
                if (exists != null && exists > 0) continue;

                jdbc.update(
                    "INSERT INTO semester_course_offerings (semester_id, plan_id, plan_course_id, course_id, " +
                    "applicable_grade, weekly_hours, course_category, course_type, status, created_by, deleted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?, 0)",
                    semesterId, planId, c.get("plan_course_id"), courseId,
                    gradeName, c.get("weekly_hours"), c.get("course_category"), c.get("course_type"), createdBy);
                count++;
            }
        }

        log.info("从培养方案导入开课计划 {} 条", count);
        return count;
    }

    // ==================== 教学任务生成 ====================

    /**
     * 从开课计划批量创建教学任务
     * 为每个 class_course_assignment (已确认) 生成一个 teaching_task
     */
    @Transactional
    public int generateTasksFromOfferings(Long semesterId, Long createdBy) {
        log.info("从开课计划生成教学任务: semesterId={}", semesterId);

        List<Map<String, Object>> assignments = jdbc.queryForList(
            "SELECT a.id AS assignmentId, a.offering_id AS offeringId, a.course_id AS courseId, " +
            "a.class_id AS classId, a.weekly_hours AS weeklyHours, a.student_count AS studentCount " +
            "FROM class_course_assignments a " +
            "WHERE a.semester_id = ? AND a.status = 1 AND a.deleted = 0 " +
            "AND NOT EXISTS (SELECT 1 FROM teaching_tasks t WHERE t.semester_id = a.semester_id " +
            "AND t.course_id = a.course_id AND t.class_id = a.class_id AND t.deleted = 0)",
            semesterId
        );

        int count = 0;
        for (Map<String, Object> a : assignments) {
            Long offeringId = toLong(a.get("offeringId"));
            Long courseId = toLong(a.get("courseId"));
            Long classId = toLong(a.get("classId"));
            int weeklyHours = ((Number) a.get("weeklyHours")).intValue();
            int studentCount = a.get("studentCount") != null ? ((Number) a.get("studentCount")).intValue() : 0;

            String taskCode = String.format("TK-%d-%d-%d", semesterId, courseId, classId);

            jdbc.update(
                "INSERT INTO teaching_tasks (task_code, semester_id, course_id, class_id, offering_id, " +
                "student_count, weekly_hours, total_hours, start_week, end_week, " +
                "scheduling_status, task_status, created_by, deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 16, 0, 1, ?, 0)",
                taskCode, semesterId, courseId, classId, offeringId,
                studentCount, weeklyHours, weeklyHours * 16, createdBy
            );
            count++;
        }

        log.info("生成教学任务 {} 条", count);
        return count;
    }

    /**
     * 从教学任务批量创建考试安排
     */
    @Transactional
    public int generateExamFromTasks(Long batchId, List<Long> taskIds, Long createdBy) {
        if (taskIds == null || taskIds.isEmpty()) {
            throw new BusinessException("请选择教学任务");
        }

        log.info("从教学任务创建考试安排: batchId={}, tasks={}", batchId, taskIds.size());

        int count = 0;
        for (Long taskId : taskIds) {
            Map<String, Object> task = jdbc.queryForMap(
                "SELECT course_id, class_id FROM teaching_tasks WHERE id = ? AND deleted = 0", taskId
            );

            jdbc.update(
                "INSERT INTO exam_arrangements (batch_id, course_id, task_id, exam_date, " +
                "start_time, end_time, duration, exam_form, total_students, status, created_by) " +
                "SELECT ?, ?, ?, CURDATE(), '08:00', '10:00', 120, 1, " +
                "COALESCE((SELECT COUNT(*) FROM students s JOIN classes c ON s.class_id = c.id " +
                "WHERE c.id = ? AND s.deleted = 0), 0), 1, ?",
                batchId, task.get("course_id"), taskId, task.get("class_id"), createdBy
            );
            count++;
        }

        log.info("生成考试安排 {} 条", count);
        return count;
    }

    /**
     * 从考试批次创建成绩批次
     */
    @Transactional
    public Long generateGradeBatchFromExam(Long examBatchId, Long createdBy) {
        Map<String, Object> examBatch = jdbc.queryForMap(
            "SELECT batch_name, semester_id, exam_type FROM exam_batches WHERE id = ? AND deleted = 0",
            examBatchId
        );

        String batchName = examBatch.get("batch_name") + " - 成绩录入";
        Long semesterId = toLong(examBatch.get("semester_id"));
        int examType = ((Number) examBatch.get("exam_type")).intValue();
        int gradeType = examType <= 2 ? examType + 1 : 3; // midterm→2, final→3

        jdbc.update(
            "INSERT INTO grade_batches (batch_code, batch_name, semester_id, grade_type, status, created_by) " +
            "VALUES (?, ?, ?, ?, 0, ?)",
            "GB-" + System.currentTimeMillis(), batchName, semesterId, gradeType, createdBy
        );

        Long gradeBatchId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        log.info("从考试批次 {} 创建成绩批次 {}", examBatchId, gradeBatchId);
        return gradeBatchId;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }
}
