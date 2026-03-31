package com.school.management.interfaces.rest.teaching;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 成绩管理 REST Controller
 * 处理成绩批次、成绩录入、成绩查询、统计与排名
 *
 * 使用 JdbcTemplate 直接操作已有的 DB 表:
 * - grade_batches
 * - student_grades
 */
@Slf4j
@RestController
@RequestMapping("/teaching/grades")
@RequiredArgsConstructor
public class TeachingGradeController {

    private final JdbcTemplate jdbc;

    // ==================== 成绩批次管理 ====================

    @GetMapping("/batches")
    public Result<Map<String, Object>> listBatches(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer gradeType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (semesterId != null) {
            where.append(" AND semester_id = ?");
            params.add(semesterId);
        }
        if (gradeType != null) {
            where.append(" AND grade_type = ?");
            params.add(gradeType);
        }
        if (status != null) {
            where.append(" AND status = ?");
            params.add(status);
        }

        // Count
        String countSql = "SELECT COUNT(*) FROM grade_batches" + where;
        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        // Page query
        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT id, batch_code AS batchCode, batch_name AS batchName, " +
            "semester_id AS semesterId, grade_type AS gradeType, " +
            "start_time AS startTime, end_time AS endTime, " +
            "status, created_by AS createdBy, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM grade_batches" + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?";
        params.add(pageSize);
        params.add(offset);
        List<Map<String, Object>> records = jdbc.queryForList(sql, params.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return Result.success(result);
    }

    @GetMapping("/batches/{id}")
    public Result<Map<String, Object>> getBatch(@PathVariable Long id) {
        Map<String, Object> batch = jdbc.queryForMap(
            "SELECT id, batch_code AS batchCode, batch_name AS batchName, " +
            "semester_id AS semesterId, grade_type AS gradeType, " +
            "start_time AS startTime, end_time AS endTime, " +
            "status, created_by AS createdBy, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM grade_batches WHERE id = ?", id
        );
        return Result.success(batch);
    }

    @PostMapping("/batches")
    public Result<Map<String, Object>> createBatch(@RequestBody Map<String, Object> data) {
        String batchName = (String) data.get("batchName");
        Long semesterId = data.get("semesterId") != null ? ((Number) data.get("semesterId")).longValue() : null;
        Integer gradeType = data.get("gradeType") != null ? ((Number) data.get("gradeType")).intValue() : 1;
        String startTimeStr = (String) data.get("startTime");
        String endTimeStr = (String) data.get("endTime");
        LocalDateTime startTime = startTimeStr != null ? LocalDateTime.parse(startTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        LocalDateTime endTime = endTimeStr != null ? LocalDateTime.parse(endTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        Integer status = data.get("status") != null ? ((Number) data.get("status")).intValue() : 0;
        Long createdBy = data.get("createdBy") != null ? ((Number) data.get("createdBy")).longValue() : null;

        // Auto-generate batch_code: GB + snowflake id suffix
        long id = IdWorker.getId();
        String batchCode = "GB" + id;
        jdbc.update(
            "INSERT INTO grade_batches (id, batch_code, batch_name, semester_id, grade_type, " +
            "start_time, end_time, status, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, batchCode, batchName, semesterId, gradeType,
            startTime, endTime, status, createdBy
        );

        data.put("id", id);
        data.put("batchCode", batchCode);
        return Result.success(data);
    }

    @PutMapping("/batches/{id}")
    public Result<Void> updateBatch(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        String batchName = (String) data.get("batchName");
        Long semesterId = data.get("semesterId") != null ? ((Number) data.get("semesterId")).longValue() : null;
        Integer gradeType = data.get("gradeType") != null ? ((Number) data.get("gradeType")).intValue() : null;
        String startTimeStr = (String) data.get("startTime");
        String endTimeStr = (String) data.get("endTime");
        LocalDateTime startTime = startTimeStr != null ? LocalDateTime.parse(startTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        LocalDateTime endTime = endTimeStr != null ? LocalDateTime.parse(endTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        Integer status = data.get("status") != null ? ((Number) data.get("status")).intValue() : null;

        jdbc.update(
            "UPDATE grade_batches SET batch_name = ?, semester_id = ?, grade_type = ?, " +
            "start_time = ?, end_time = ?, status = ?, updated_at = NOW() " +
            "WHERE id = ?",
            batchName, semesterId, gradeType, startTime, endTime, status, id
        );
        return Result.success();
    }

    @DeleteMapping("/batches/{id}")
    public Result<Void> deleteBatch(@PathVariable Long id) {
        // Physical delete
        jdbc.update("DELETE FROM grade_batches WHERE id = ?", id);
        return Result.success();
    }

    @PostMapping("/batches/{id}/submit")
    public Result<Void> submitBatch(@PathVariable Long id) {
        jdbc.update("UPDATE grade_batches SET status = 1, updated_at = NOW() WHERE id = ?", id);
        return Result.success();
    }

    @PostMapping("/batches/{id}/approve")
    public Result<Void> approveBatch(@PathVariable Long id) {
        jdbc.update("UPDATE grade_batches SET status = 2, updated_at = NOW() WHERE id = ?", id);
        return Result.success();
    }

    @PostMapping("/batches/{id}/publish")
    public Result<Void> publishBatch(@PathVariable Long id) {
        jdbc.update("UPDATE grade_batches SET status = 3, updated_at = NOW() WHERE id = ?", id);
        return Result.success();
    }

    // ==================== 成绩录入与查询 ====================

    @GetMapping("/batches/{batchId}/grades")
    public Result<List<Map<String, Object>>> listGradesByBatch(@PathVariable Long batchId) {
        List<Map<String, Object>> grades;
        try {
            grades = jdbc.queryForList(
                "SELECT g.id, g.batch_id AS batchId, g.semester_id AS semesterId, " +
                "g.task_id AS taskId, g.course_id AS courseId, g.student_id AS studentId, " +
                "g.class_id AS classId, g.total_score AS totalScore, " +
                "g.grade_point AS gradePoint, g.passed, g.credits_earned AS creditsEarned, " +
                "g.grade_status AS gradeStatus, g.remark, " +
                "g.created_at AS createdAt, g.updated_at AS updatedAt, " +
                "s.name AS studentName, s.student_no AS studentNo " +
                "FROM student_grades g " +
                "LEFT JOIN students s ON g.student_id = s.id " +
                "WHERE g.batch_id = ? AND g.deleted = 0 ORDER BY g.created_at DESC",
                batchId
            );
        } catch (Exception e) {
            // Fallback without JOIN if students table has different structure
            grades = jdbc.queryForList(
                "SELECT id, batch_id AS batchId, semester_id AS semesterId, " +
                "task_id AS taskId, course_id AS courseId, student_id AS studentId, " +
                "class_id AS classId, total_score AS totalScore, " +
                "grade_point AS gradePoint, passed, credits_earned AS creditsEarned, " +
                "grade_status AS gradeStatus, remark, " +
                "created_at AS createdAt, updated_at AS updatedAt " +
                "FROM student_grades WHERE batch_id = ? AND deleted = 0 ORDER BY created_at DESC",
                batchId
            );
        }
        return Result.success(grades);
    }

    @PostMapping("/batches/{batchId}/grades")
    public Result<Map<String, Object>> recordGrade(
            @PathVariable Long batchId,
            @RequestBody Map<String, Object> data) {
        Long semesterId = data.get("semesterId") != null ? ((Number) data.get("semesterId")).longValue() : null;
        Long taskId = data.get("taskId") != null ? ((Number) data.get("taskId")).longValue() : null;
        Long courseId = data.get("courseId") != null ? ((Number) data.get("courseId")).longValue() : null;
        Long studentId = data.get("studentId") != null ? ((Number) data.get("studentId")).longValue() : null;
        Long classId = data.get("classId") != null ? ((Number) data.get("classId")).longValue() : null;
        BigDecimal totalScore = data.get("totalScore") != null ? new BigDecimal(data.get("totalScore").toString()) : null;
        BigDecimal gradePoint = data.get("gradePoint") != null ? new BigDecimal(data.get("gradePoint").toString()) : null;
        Integer passed = data.get("passed") != null ? ((Number) data.get("passed")).intValue() : null;
        BigDecimal creditsEarned = data.get("creditsEarned") != null ? new BigDecimal(data.get("creditsEarned").toString()) : null;
        Integer gradeStatus = data.get("gradeStatus") != null ? ((Number) data.get("gradeStatus")).intValue() : 1;
        String remark = (String) data.get("remark");

        long id = IdWorker.getId();
        jdbc.update(
            "INSERT INTO student_grades (id, batch_id, semester_id, task_id, course_id, student_id, " +
            "class_id, total_score, grade_point, passed, credits_earned, grade_status, remark, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
            id, batchId, semesterId, taskId, courseId, studentId,
            classId, totalScore, gradePoint, passed, creditsEarned, gradeStatus, remark
        );

        data.put("id", id);
        data.put("batchId", batchId);
        return Result.success(data);
    }

    @PutMapping("/{gradeId}")
    public Result<Void> updateGrade(@PathVariable Long gradeId, @RequestBody Map<String, Object> data) {
        BigDecimal totalScore = data.get("totalScore") != null ? new BigDecimal(data.get("totalScore").toString()) : null;
        BigDecimal gradePoint = data.get("gradePoint") != null ? new BigDecimal(data.get("gradePoint").toString()) : null;
        Integer passed = data.get("passed") != null ? ((Number) data.get("passed")).intValue() : null;
        BigDecimal creditsEarned = data.get("creditsEarned") != null ? new BigDecimal(data.get("creditsEarned").toString()) : null;
        Integer gradeStatus = data.get("gradeStatus") != null ? ((Number) data.get("gradeStatus")).intValue() : null;
        String remark = (String) data.get("remark");

        jdbc.update(
            "UPDATE student_grades SET total_score = ?, grade_point = ?, passed = ?, " +
            "credits_earned = ?, grade_status = ?, remark = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            totalScore, gradePoint, passed, creditsEarned, gradeStatus, remark, gradeId
        );
        return Result.success();
    }

    @PostMapping("/batches/{batchId}/batch-record")
    @Transactional
    public Result<Void> batchRecordGrades(
            @PathVariable Long batchId,
            @RequestBody Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> grades = (List<Map<String, Object>>) data.get("grades");
        if (grades != null) {
            // Get batch info for semesterId
            Long semesterId = null;
            try {
                Map<String, Object> batch = jdbc.queryForMap(
                    "SELECT semester_id FROM grade_batches WHERE id = ?", batchId);
                semesterId = batch.get("semester_id") != null ? ((Number) batch.get("semester_id")).longValue() : null;
            } catch (Exception ignored) {
            }

            int seq = 1;
            for (Map<String, Object> grade : grades) {
                Long studentId = grade.get("studentId") != null ? ((Number) grade.get("studentId")).longValue() : null;
                BigDecimal totalScore = grade.get("totalScore") != null ? new BigDecimal(grade.get("totalScore").toString()) : null;
                String remark = (String) grade.get("remark");
                Long courseId = grade.get("courseId") != null ? ((Number) grade.get("courseId")).longValue() : null;
                Long classId = grade.get("classId") != null ? ((Number) grade.get("classId")).longValue() : null;
                Long taskId = grade.get("taskId") != null ? ((Number) grade.get("taskId")).longValue() : null;
                BigDecimal gradePoint = grade.get("gradePoint") != null ? new BigDecimal(grade.get("gradePoint").toString()) : null;
                Integer passed = grade.get("passed") != null ? ((Number) grade.get("passed")).intValue() : null;
                BigDecimal creditsEarned = grade.get("creditsEarned") != null ? new BigDecimal(grade.get("creditsEarned").toString()) : null;

                long id = IdWorker.getId();
                jdbc.update(
                    "INSERT INTO student_grades (id, batch_id, semester_id, task_id, course_id, student_id, " +
                    "class_id, total_score, grade_point, passed, credits_earned, grade_status, remark, deleted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, 0)",
                    id, batchId, semesterId, taskId, courseId, studentId,
                    classId, totalScore, gradePoint, passed, creditsEarned, remark
                );
                seq++;
            }
        }
        return Result.success();
    }

    // ==================== 按学生/班级查询 ====================

    @GetMapping("/by-student/{studentId}")
    public Result<List<Map<String, Object>>> getGradesByStudent(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long courseId) {

        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        try {
            sql.append(
                "SELECT g.id, g.batch_id AS batchId, g.semester_id AS semesterId, " +
                "g.task_id AS taskId, g.course_id AS courseId, g.student_id AS studentId, " +
                "g.class_id AS classId, g.total_score AS totalScore, " +
                "g.grade_point AS gradePoint, g.passed, g.credits_earned AS creditsEarned, " +
                "g.grade_status AS gradeStatus, g.remark, " +
                "c.course_name AS courseName " +
                "FROM student_grades g " +
                "LEFT JOIN courses c ON g.course_id = c.id " +
                "WHERE g.student_id = ? AND g.deleted = 0"
            );
        } catch (Exception e) {
            sql.setLength(0);
            sql.append(
                "SELECT id, batch_id AS batchId, semester_id AS semesterId, " +
                "task_id AS taskId, course_id AS courseId, student_id AS studentId, " +
                "class_id AS classId, total_score AS totalScore, " +
                "grade_point AS gradePoint, passed, credits_earned AS creditsEarned, " +
                "grade_status AS gradeStatus, remark " +
                "FROM student_grades WHERE student_id = ? AND deleted = 0"
            );
        }
        params.add(studentId);

        if (semesterId != null) {
            sql.append(" AND g.semester_id = ?");
            params.add(semesterId);
        }
        if (courseId != null) {
            sql.append(" AND g.course_id = ?");
            params.add(courseId);
        }
        sql.append(" ORDER BY g.created_at DESC");

        List<Map<String, Object>> grades;
        try {
            grades = jdbc.queryForList(sql.toString(), params.toArray());
        } catch (Exception e) {
            // Fallback: query without JOIN
            StringBuilder fallback = new StringBuilder(
                "SELECT id, batch_id AS batchId, semester_id AS semesterId, " +
                "task_id AS taskId, course_id AS courseId, student_id AS studentId, " +
                "class_id AS classId, total_score AS totalScore, " +
                "grade_point AS gradePoint, passed, credits_earned AS creditsEarned, " +
                "grade_status AS gradeStatus, remark " +
                "FROM student_grades WHERE student_id = ? AND deleted = 0"
            );
            List<Object> fbParams = new ArrayList<>();
            fbParams.add(studentId);
            if (semesterId != null) {
                fallback.append(" AND semester_id = ?");
                fbParams.add(semesterId);
            }
            if (courseId != null) {
                fallback.append(" AND course_id = ?");
                fbParams.add(courseId);
            }
            fallback.append(" ORDER BY created_at DESC");
            grades = jdbc.queryForList(fallback.toString(), fbParams.toArray());
        }
        return Result.success(grades);
    }

    @GetMapping("/by-class/{classId}")
    public Result<List<Map<String, Object>>> getGradesByClass(@PathVariable Long classId) {
        List<Map<String, Object>> grades;
        try {
            grades = jdbc.queryForList(
                "SELECT g.id, g.batch_id AS batchId, g.semester_id AS semesterId, " +
                "g.course_id AS courseId, g.student_id AS studentId, " +
                "g.class_id AS classId, g.total_score AS totalScore, " +
                "g.grade_point AS gradePoint, g.passed, g.credits_earned AS creditsEarned, " +
                "g.grade_status AS gradeStatus, g.remark, " +
                "c.course_name AS courseName, " +
                "s.name AS studentName, s.student_no AS studentNo " +
                "FROM student_grades g " +
                "LEFT JOIN courses c ON g.course_id = c.id " +
                "LEFT JOIN students s ON g.student_id = s.id " +
                "WHERE g.class_id = ? AND g.deleted = 0 " +
                "ORDER BY g.course_id, g.total_score DESC",
                classId
            );
        } catch (Exception e) {
            grades = jdbc.queryForList(
                "SELECT id, batch_id AS batchId, semester_id AS semesterId, " +
                "course_id AS courseId, student_id AS studentId, " +
                "class_id AS classId, total_score AS totalScore, " +
                "grade_point AS gradePoint, passed, credits_earned AS creditsEarned, " +
                "grade_status AS gradeStatus, remark " +
                "FROM student_grades WHERE class_id = ? AND deleted = 0 " +
                "ORDER BY course_id, total_score DESC",
                classId
            );
        }
        return Result.success(grades);
    }

    // ==================== 统计与排名 ====================

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long courseId) {

        StringBuilder where = new StringBuilder(" WHERE deleted = 0");
        List<Object> params = new ArrayList<>();

        if (batchId != null) {
            where.append(" AND batch_id = ?");
            params.add(batchId);
        }
        if (classId != null) {
            where.append(" AND class_id = ?");
            params.add(classId);
        }
        if (courseId != null) {
            where.append(" AND course_id = ?");
            params.add(courseId);
        }

        String statsSql = "SELECT " +
            "COUNT(*) AS totalCount, " +
            "AVG(total_score) AS avgScore, " +
            "MAX(total_score) AS maxScore, " +
            "MIN(total_score) AS minScore, " +
            "SUM(CASE WHEN passed = 1 THEN 1 ELSE 0 END) AS passedCount " +
            "FROM student_grades" + where;
        Map<String, Object> stats = jdbc.queryForMap(statsSql, params.toArray());

        // Score distribution
        String distSql = "SELECT " +
            "SUM(CASE WHEN total_score >= 90 THEN 1 ELSE 0 END) AS excellent, " +
            "SUM(CASE WHEN total_score >= 80 AND total_score < 90 THEN 1 ELSE 0 END) AS good, " +
            "SUM(CASE WHEN total_score >= 70 AND total_score < 80 THEN 1 ELSE 0 END) AS medium, " +
            "SUM(CASE WHEN total_score >= 60 AND total_score < 70 THEN 1 ELSE 0 END) AS pass, " +
            "SUM(CASE WHEN total_score < 60 THEN 1 ELSE 0 END) AS fail " +
            "FROM student_grades" + where;
        Map<String, Object> distribution = jdbc.queryForMap(distSql, params.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", stats.get("totalCount"));
        result.put("avgScore", stats.get("avgScore"));
        result.put("maxScore", stats.get("maxScore"));
        result.put("minScore", stats.get("minScore"));
        result.put("passedCount", stats.get("passedCount"));
        result.put("distribution", distribution);
        return Result.success(result);
    }

    @GetMapping("/ranking")
    public Result<List<Map<String, Object>>> getRanking(
            @RequestParam Long classId,
            @RequestParam(required = false) Long semesterId) {

        StringBuilder sql = new StringBuilder(
            "SELECT student_id AS studentId, " +
            "SUM(total_score) AS totalScore, " +
            "AVG(total_score) AS avgScore, " +
            "COUNT(*) AS courseCount "
        );

        // Try to join students table for name
        boolean joinStudents = true;
        try {
            jdbc.queryForObject("SELECT 1 FROM students LIMIT 1", Integer.class);
            sql.setLength(0);
            sql.append(
                "SELECT g.student_id AS studentId, " +
                "s.name AS studentName, s.student_no AS studentNo, " +
                "SUM(g.total_score) AS totalScore, " +
                "AVG(g.total_score) AS avgScore, " +
                "COUNT(*) AS courseCount " +
                "FROM student_grades g " +
                "LEFT JOIN students s ON g.student_id = s.id " +
                "WHERE g.class_id = ? AND g.deleted = 0"
            );
        } catch (Exception e) {
            joinStudents = false;
            sql.setLength(0);
            sql.append(
                "SELECT student_id AS studentId, " +
                "SUM(total_score) AS totalScore, " +
                "AVG(total_score) AS avgScore, " +
                "COUNT(*) AS courseCount " +
                "FROM student_grades " +
                "WHERE class_id = ? AND deleted = 0"
            );
        }

        List<Object> params = new ArrayList<>();
        params.add(classId);

        if (semesterId != null) {
            sql.append(joinStudents ? " AND g.semester_id = ?" : " AND semester_id = ?");
            params.add(semesterId);
        }

        sql.append(joinStudents ? " GROUP BY g.student_id, s.name, s.student_no" : " GROUP BY student_id");
        sql.append(" ORDER BY totalScore DESC");

        List<Map<String, Object>> ranking = jdbc.queryForList(sql.toString(), params.toArray());

        // Add rank number
        int rank = 1;
        for (Map<String, Object> row : ranking) {
            row.put("rank", rank++);
        }

        return Result.success(ranking);
    }
}
