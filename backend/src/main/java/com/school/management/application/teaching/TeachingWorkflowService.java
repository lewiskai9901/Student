package com.school.management.application.teaching;

import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.util.List;
import java.util.Map;

/**
 * 教务工作流编排服务
 *
 * 串联: 培养方案 → 开课计划 → 教学任务 → 考试安排 → 成绩批次
 *
 * 数据链路:
 * curriculum_plans → semester_course_offerings (plan_id, plan_course_id)
 *   → class_course_assignments (offering_id)
 *   → teaching_tasks (offering_id, 从 assignment 生成)
 *   → schedule_entries (task_id)
 *   → exam_arrangements (task_id, 从 task 生成)
 *   → grade_batches + student_grades (task_id, 从 exam 生成)
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
            "SELECT CAST(start_year AS UNSIGNED) AS start_year, semester_type FROM semesters WHERE id = ? AND deleted = 0", semesterId);
        int semStartYear = ((Number) sem.get("start_year")).intValue();
        int semType = ((Number) sem.get("semester_type")).intValue();

        List<Map<String, Object>> cohorts = jdbc.queryForList(
            "SELECT id, CAST(enrollment_year AS UNSIGNED) AS enrollment_year, CAST(graduation_year AS UNSIGNED) AS graduation_year FROM grades WHERE status = 1 AND deleted = 0");

        int count = 0;
        for (Map<String, Object> c : cohorts) {
            Long cohortId = toLong(c.get("id"));
            int enrollYear = ((Number) c.get("enrollment_year")).intValue();
            int gradYear = c.get("graduation_year") != null ? ((Number) c.get("graduation_year")).intValue() : enrollYear + 3;
            int schoolingYears = gradYear - enrollYear;
            if (schoolingYears <= 0) schoolingYears = 3;
            int programSem = (semStartYear - enrollYear) * 2 + semType;

            if (programSem < 1 || programSem > schoolingYears * 2) continue;

            Long exists = jdbc.queryForObject(
                "SELECT COUNT(1) FROM cohort_semester_mapping WHERE cohort_id = ? AND semester_id = ?",
                Long.class, cohortId, semesterId);
            if (exists != null && exists > 0) continue;

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

    // ==================== 开课计划生成 ====================

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
     * 为每个已确认的 class_course_assignment 生成一个 teaching_task
     * 关键关联: teaching_tasks.offering_id → semester_course_offerings.id
     */
    @Transactional
    public int generateTasksFromOfferings(Long semesterId, Long createdBy) {
        log.info("从开课计划生成教学任务: semesterId={}", semesterId);

        // 从校历教学周表获取实际周数
        int startWeek = 1;
        int endWeek = getSemesterTeachingWeeks(semesterId);

        List<Map<String, Object>> assignments = jdbc.queryForList(
            "SELECT a.id AS assignmentId, a.offering_id AS offeringId, a.course_id AS courseId, " +
            "a.org_unit_id AS orgUnitId, a.weekly_hours AS weeklyHours, a.student_count AS studentCount, " +
            "o.start_week AS offeringStartWeek, o.end_week AS offeringEndWeek, o.week_type AS offeringWeekType " +
            "FROM class_course_assignments a " +
            "LEFT JOIN semester_course_offerings o ON o.id = a.offering_id " +
            "WHERE a.semester_id = ? AND a.status = 1 AND a.deleted = 0 " +
            "AND NOT EXISTS (SELECT 1 FROM teaching_tasks t WHERE t.semester_id = a.semester_id " +
            "AND t.course_id = a.course_id AND t.org_unit_id = a.org_unit_id AND t.deleted = 0)",
            semesterId
        );

        int count = 0;
        for (Map<String, Object> a : assignments) {
            Long offeringId = toLong(a.get("offeringId"));
            Long courseId = toLong(a.get("courseId"));
            Long orgUnitId = toLong(a.get("orgUnitId"));
            int weeklyHours = ((Number) a.get("weeklyHours")).intValue();
            int studentCount = a.get("studentCount") != null ? ((Number) a.get("studentCount")).intValue() : 0;

            // 从开课计划继承周次范围和周次类型
            int taskStartWeek = a.get("offeringStartWeek") != null ? ((Number) a.get("offeringStartWeek")).intValue() : startWeek;
            int taskEndWeek = a.get("offeringEndWeek") != null ? ((Number) a.get("offeringEndWeek")).intValue() : endWeek;
            int taskWeekType = a.get("offeringWeekType") != null ? ((Number) a.get("offeringWeekType")).intValue() : 0;
            // 单双周总学时要减半
            int totalHours = weeklyHours * (taskEndWeek - taskStartWeek + 1);
            if (taskWeekType == 1 || taskWeekType == 2) totalHours = Math.round(totalHours / 2.0f);

            long taskId = IdWorker.getId();
            String taskCode = "TK" + taskId;

            jdbc.update(
                "INSERT INTO teaching_tasks (id, task_code, semester_id, course_id, org_unit_id, offering_id, " +
                "student_count, weekly_hours, total_hours, start_week, end_week, week_type, " +
                "scheduling_status, task_status, created_by, deleted, tenant_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, 0, 1)",
                taskId, taskCode, semesterId, courseId, orgUnitId, offeringId,
                studentCount, weeklyHours, totalHours, taskStartWeek, taskEndWeek, taskWeekType, createdBy
            );
            count++;
        }

        log.info("生成教学任务 {} 条", count);
        return count;
    }

    // ==================== 考试安排生成 ====================

    /**
     * 从教学任务批量创建考试安排
     * 关键关联: exam_arrangements.task_id → teaching_tasks.id
     * 考试时间从 exam_batch 的 start_date 推算，而非硬编码
     */
    @Transactional
    public int generateExamFromTasks(Long batchId, List<Long> taskIds, Long createdBy) {
        if (taskIds == null || taskIds.isEmpty()) {
            throw new BusinessException("请选择教学任务");
        }

        log.info("从教学任务创建考试安排: batchId={}, tasks={}", batchId, taskIds.size());

        // 从考试批次获取默认考试日期和类型
        Map<String, Object> batch = jdbc.queryForMap(
            "SELECT batch_name, start_date, end_date, exam_type FROM exam_batches WHERE id = ? AND deleted = 0", batchId);
        Object batchStartDate = batch.get("start_date");
        int examType = batch.get("exam_type") != null ? ((Number) batch.get("exam_type")).intValue() : 1;

        // 考试形式：从课程的 exam_type 字段读取，默认笔试
        int count = 0;
        for (Long taskId : taskIds) {
            Map<String, Object> task = jdbc.queryForMap(
                "SELECT t.course_id, t.org_unit_id, t.student_count, " +
                "COALESCE(c.assessment_method, 1) AS course_exam_form, " +
                "COALESCE(c.total_hours, 120) AS course_hours " +
                "FROM teaching_tasks t " +
                "LEFT JOIN courses c ON c.id = t.course_id " +
                "WHERE t.id = ? AND t.deleted = 0", taskId
            );

            Long courseId = toLong(task.get("course_id"));
            Long orgUnitId = toLong(task.get("org_unit_id"));
            int studentCount = task.get("student_count") != null ? ((Number) task.get("student_count")).intValue() : 0;
            int examForm = ((Number) task.get("course_exam_form")).intValue();
            // 考试时长：根据课程总学时推算，最少60分钟，最多180分钟
            int courseHours = ((Number) task.get("course_hours")).intValue();
            int duration = Math.max(60, Math.min(180, courseHours >= 64 ? 120 : 90));

            // 检查重复
            Long exists = jdbc.queryForObject(
                "SELECT COUNT(1) FROM exam_arrangements WHERE batch_id = ? AND task_id = ?",
                Long.class, batchId, taskId);
            if (exists != null && exists > 0) continue;

            jdbc.update(
                "INSERT INTO exam_arrangements (batch_id, course_id, task_id, org_unit_id, exam_date, " +
                "start_time, end_time, duration, exam_form, total_students, status, created_by) " +
                "VALUES (?, ?, ?, ?, ?, '08:00', ADDTIME('08:00', SEC_TO_TIME(? * 60)), ?, ?, ?, 0, ?)",
                batchId, courseId, taskId, orgUnitId,
                batchStartDate != null ? batchStartDate : "CURDATE()",
                duration, duration, examForm, studentCount, createdBy
            );
            count++;
        }

        log.info("生成考试安排 {} 条", count);
        return count;
    }

    // ==================== 成绩批次生成 ====================

    /**
     * 从考试批次创建成绩批次
     * 同时为该批次涉及的每个学生生成 student_grades 待录入记录
     */
    @Transactional
    public Long generateGradeBatchFromExam(Long examBatchId, Long createdBy) {
        Map<String, Object> examBatch = jdbc.queryForMap(
            "SELECT batch_name, semester_id, exam_type FROM exam_batches WHERE id = ? AND deleted = 0",
            examBatchId);

        String batchName = examBatch.get("batch_name") + " - 成绩录入";
        Long semesterId = toLong(examBatch.get("semester_id"));
        int examType = ((Number) examBatch.get("exam_type")).intValue();
        // 考试类型→成绩类型映射: 1(期中)→2, 2(期末)→3, 3(补考)→3, 4(重修)→3
        int gradeType = examType == 1 ? 2 : 3;

        String batchCode = "GB-" + semesterId + "-" + System.currentTimeMillis();
        jdbc.update(
            "INSERT INTO grade_batches (batch_code, batch_name, semester_id, grade_type, status, created_by) " +
            "VALUES (?, ?, ?, ?, 0, ?)",
            batchCode, batchName, semesterId, gradeType, createdBy
        );

        Long gradeBatchId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        // 为考试批次中的每个安排，生成对应的学生成绩待录入记录
        List<Map<String, Object>> arrangements = jdbc.queryForList(
            "SELECT ea.task_id, ea.course_id, ea.org_unit_id " +
            "FROM exam_arrangements ea WHERE ea.batch_id = ?", examBatchId);

        int gradeCount = 0;
        for (Map<String, Object> arr : arrangements) {
            Long taskId = toLong(arr.get("task_id"));
            Long courseId = toLong(arr.get("course_id"));
            Long orgUnitId = toLong(arr.get("org_unit_id"));
            if (taskId == null || courseId == null) continue;

            // 查找班级学生，为每人生成一条待录入记录
            List<Map<String, Object>> students = jdbc.queryForList(
                "SELECT id FROM students WHERE org_unit_id = ? AND status = 1 AND deleted = 0", orgUnitId);

            for (Map<String, Object> s : students) {
                Long studentId = toLong(s.get("id"));
                // 避免重复
                Long exists = jdbc.queryForObject(
                    "SELECT COUNT(1) FROM student_grades " +
                    "WHERE batch_id = ? AND student_id = ? AND course_id = ? AND semester_id = ?",
                    Long.class, gradeBatchId, studentId, courseId, semesterId);
                if (exists != null && exists > 0) continue;

                jdbc.update(
                    "INSERT INTO student_grades (batch_id, semester_id, task_id, course_id, student_id, org_unit_id, " +
                    "grade_status, deleted) VALUES (?, ?, ?, ?, ?, ?, 0, 0)",
                    gradeBatchId, semesterId, taskId, courseId, studentId, orgUnitId);
                gradeCount++;
            }
        }

        log.info("从考试批次 {} 创建成绩批次 {}, 生成 {} 条待录入成绩", examBatchId, gradeBatchId, gradeCount);
        return gradeBatchId;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
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
