package com.school.management.interfaces.rest.teaching;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * 考试管理 REST Controller
 * 处理考试批次、考试安排、考场分配、监考分配的 CRUD
 *
 * 使用 JdbcTemplate 直接操作已有的 DB 表:
 * - exam_batches
 * - exam_arrangements
 * - exam_rooms
 * - exam_invigilators
 */
@Slf4j
@RestController
@RequestMapping("/teaching/examinations")
@RequiredArgsConstructor
public class TeachingExamController {

    private final JdbcTemplate jdbc;

    // ==================== 考试批次管理 ====================

    @GetMapping("/batches")
    public Result<Map<String, Object>> listBatches(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer examType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        StringBuilder where = new StringBuilder(" WHERE deleted = 0");
        List<Object> params = new ArrayList<>();

        if (semesterId != null) {
            where.append(" AND semester_id = ?");
            params.add(semesterId);
        }
        if (examType != null) {
            where.append(" AND exam_type = ?");
            params.add(examType);
        }
        if (status != null) {
            where.append(" AND status = ?");
            params.add(status);
        }

        // Count
        String countSql = "SELECT COUNT(*) FROM exam_batches" + where;
        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        // Page query
        int offset = (pageNum - 1) * pageSize;
        String sql = "SELECT id, batch_code AS batchCode, batch_name AS batchName, " +
            "semester_id AS semesterId, exam_type AS examType, " +
            "start_date AS startDate, end_date AS endDate, " +
            "status, remark, created_by AS createdBy, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM exam_batches" + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?";
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
            "semester_id AS semesterId, exam_type AS examType, " +
            "start_date AS startDate, end_date AS endDate, " +
            "status, remark, created_by AS createdBy, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM exam_batches WHERE id = ? AND deleted = 0", id
        );
        return Result.success(batch);
    }

    @PostMapping("/batches")
    public Result<Map<String, Object>> createBatch(@RequestBody Map<String, Object> data) {
        String batchName = (String) data.get("batchName");
        Long semesterId = data.get("semesterId") != null ? ((Number) data.get("semesterId")).longValue() : null;
        Integer examType = data.get("examType") != null ? ((Number) data.get("examType")).intValue() : 1;
        LocalDate startDate = data.get("startDate") != null ? LocalDate.parse((String) data.get("startDate")) : null;
        LocalDate endDate = data.get("endDate") != null ? LocalDate.parse((String) data.get("endDate")) : null;
        Integer status = data.get("status") != null ? ((Number) data.get("status")).intValue() : 0;
        String remark = (String) data.get("remark");
        Long createdBy = data.get("createdBy") != null ? ((Number) data.get("createdBy")).longValue() : null;

        // Auto-generate batch_code: EX + snowflake id suffix
        long id = IdWorker.getId();
        String batchCode = "EX" + id;
        jdbc.update(
            "INSERT INTO exam_batches (id, batch_code, batch_name, semester_id, exam_type, " +
            "start_date, end_date, status, remark, created_by, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
            id, batchCode, batchName, semesterId, examType,
            startDate, endDate, status, remark, createdBy
        );

        data.put("id", id);
        data.put("batchCode", batchCode);
        return Result.success(data);
    }

    @PutMapping("/batches/{id}")
    public Result<Void> updateBatch(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        String batchName = (String) data.get("batchName");
        Long semesterId = data.get("semesterId") != null ? ((Number) data.get("semesterId")).longValue() : null;
        Integer examType = data.get("examType") != null ? ((Number) data.get("examType")).intValue() : null;
        LocalDate startDate = data.get("startDate") != null ? LocalDate.parse((String) data.get("startDate")) : null;
        LocalDate endDate = data.get("endDate") != null ? LocalDate.parse((String) data.get("endDate")) : null;
        Integer status = data.get("status") != null ? ((Number) data.get("status")).intValue() : null;
        String remark = (String) data.get("remark");

        jdbc.update(
            "UPDATE exam_batches SET batch_name = ?, semester_id = ?, exam_type = ?, " +
            "start_date = ?, end_date = ?, status = ?, remark = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            batchName, semesterId, examType, startDate, endDate, status, remark, id
        );
        return Result.success();
    }

    @DeleteMapping("/batches/{id}")
    public Result<Void> deleteBatch(@PathVariable Long id) {
        jdbc.update("UPDATE exam_batches SET deleted = 1 WHERE id = ?", id);
        return Result.success();
    }

    @PostMapping("/batches/{id}/publish")
    public Result<Void> publishBatch(@PathVariable Long id) {
        jdbc.update("UPDATE exam_batches SET status = 2, updated_at = NOW() WHERE id = ? AND deleted = 0", id);
        return Result.success();
    }

    // ==================== 考试安排 ====================

    @GetMapping("/batches/{batchId}/arrangements")
    public Result<List<Map<String, Object>>> listArrangements(@PathVariable Long batchId) {
        List<Map<String, Object>> arrangements;
        try {
            arrangements = jdbc.queryForList(
                "SELECT a.id, a.batch_id AS batchId, a.course_id AS courseId, " +
                "a.exam_date AS examDate, a.start_time AS startTime, a.end_time AS endTime, " +
                "a.duration, a.exam_form AS examForm, a.total_students AS totalStudents, " +
                "a.remark, a.status, a.created_at AS createdAt, " +
                "c.course_name AS courseName " +
                "FROM exam_arrangements a " +
                "LEFT JOIN courses c ON a.course_id = c.id " +
                "WHERE a.batch_id = ? ORDER BY a.exam_date, a.start_time",
                batchId
            );
        } catch (Exception e) {
            // Fallback without JOIN if courses table has different structure
            arrangements = jdbc.queryForList(
                "SELECT id, batch_id AS batchId, course_id AS courseId, " +
                "exam_date AS examDate, start_time AS startTime, end_time AS endTime, " +
                "duration, exam_form AS examForm, total_students AS totalStudents, " +
                "remark, status, created_at AS createdAt " +
                "FROM exam_arrangements WHERE batch_id = ? ORDER BY exam_date, start_time",
                batchId
            );
        }
        return Result.success(arrangements);
    }

    @PostMapping("/batches/{batchId}/arrangements")
    public Result<Map<String, Object>> createArrangement(
            @PathVariable Long batchId,
            @RequestBody Map<String, Object> data) {
        Long courseId = data.get("courseId") != null ? ((Number) data.get("courseId")).longValue() : null;
        LocalDate examDate = data.get("examDate") != null ? LocalDate.parse((String) data.get("examDate")) : null;
        LocalTime startTime = data.get("startTime") != null ? LocalTime.parse((String) data.get("startTime")) : null;
        LocalTime endTime = data.get("endTime") != null ? LocalTime.parse((String) data.get("endTime")) : null;
        Integer duration = data.get("duration") != null ? ((Number) data.get("duration")).intValue() : null;
        Integer examForm = data.get("examForm") != null ? ((Number) data.get("examForm")).intValue() : 1;
        Integer totalStudents = data.get("totalStudents") != null ? ((Number) data.get("totalStudents")).intValue() : 0;
        String remark = (String) data.get("remark");
        Long createdBy = data.get("createdBy") != null ? ((Number) data.get("createdBy")).longValue() : null;

        long id = IdWorker.getId();
        jdbc.update(
            "INSERT INTO exam_arrangements (id, batch_id, course_id, exam_date, start_time, end_time, " +
            "duration, exam_form, total_students, remark, status, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)",
            id, batchId, courseId, examDate, startTime, endTime,
            duration, examForm, totalStudents, remark, createdBy
        );

        data.put("id", id);
        data.put("batchId", batchId);
        return Result.success(data);
    }

    @PutMapping("/batches/{batchId}/arrangements/{arrangementId}")
    public Result<Void> updateArrangement(
            @PathVariable Long batchId,
            @PathVariable Long arrangementId,
            @RequestBody Map<String, Object> data) {
        Long courseId = data.get("courseId") != null ? ((Number) data.get("courseId")).longValue() : null;
        LocalDate examDate = data.get("examDate") != null ? LocalDate.parse((String) data.get("examDate")) : null;
        LocalTime startTime = data.get("startTime") != null ? LocalTime.parse((String) data.get("startTime")) : null;
        LocalTime endTime = data.get("endTime") != null ? LocalTime.parse((String) data.get("endTime")) : null;
        Integer duration = data.get("duration") != null ? ((Number) data.get("duration")).intValue() : null;
        Integer examForm = data.get("examForm") != null ? ((Number) data.get("examForm")).intValue() : null;
        Integer totalStudents = data.get("totalStudents") != null ? ((Number) data.get("totalStudents")).intValue() : null;
        String remark = (String) data.get("remark");

        jdbc.update(
            "UPDATE exam_arrangements SET course_id = ?, exam_date = ?, start_time = ?, end_time = ?, " +
            "duration = ?, exam_form = ?, total_students = ?, remark = ?, updated_at = NOW() " +
            "WHERE id = ? AND batch_id = ?",
            courseId, examDate, startTime, endTime, duration, examForm, totalStudents, remark,
            arrangementId, batchId
        );
        return Result.success();
    }

    @DeleteMapping("/batches/{batchId}/arrangements/{arrangementId}")
    public Result<Void> deleteArrangement(
            @PathVariable Long batchId,
            @PathVariable Long arrangementId) {
        // Physical delete
        jdbc.update("DELETE FROM exam_arrangements WHERE id = ? AND batch_id = ?", arrangementId, batchId);
        return Result.success();
    }

    // ==================== 考场分配 ====================

    @PostMapping("/arrangements/{arrangementId}/rooms")
    @Transactional
    public Result<Void> assignRooms(
            @PathVariable Long arrangementId,
            @RequestBody Map<String, Object> data) {
        // Delete existing rooms for this arrangement
        jdbc.update("DELETE FROM exam_rooms WHERE arrangement_id = ?", arrangementId);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rooms = (List<Map<String, Object>>) data.get("rooms");
        if (rooms != null) {
            int seq = 1;
            for (Map<String, Object> room : rooms) {
                Long classroomId = room.get("classroomId") != null ? ((Number) room.get("classroomId")).longValue() : null;
                Integer capacity = room.get("capacity") != null ? ((Number) room.get("capacity")).intValue() : 0;
                String roomCode = room.get("roomCode") != null ? (String) room.get("roomCode") : "R" + seq;

                long roomId = IdWorker.getId();
                jdbc.update(
                    "INSERT INTO exam_rooms (id, arrangement_id, classroom_id, room_code, seat_count, student_count) " +
                    "VALUES (?, ?, ?, ?, ?, 0)",
                    roomId, arrangementId, classroomId, roomCode, capacity
                );
                seq++;
            }
        }
        return Result.success();
    }

    // ==================== 监考分配 ====================

    @PostMapping("/rooms/{roomId}/invigilators")
    @Transactional
    public Result<Void> assignInvigilators(
            @PathVariable Long roomId,
            @RequestBody Map<String, Object> data) {
        // Delete existing invigilators for this room
        jdbc.update("DELETE FROM exam_invigilators WHERE room_id = ?", roomId);

        @SuppressWarnings("unchecked")
        List<Number> teacherIds = (List<Number>) data.get("teacherIds");
        Long mainTeacherId = data.get("mainTeacherId") != null ? ((Number) data.get("mainTeacherId")).longValue() : null;

        if (teacherIds != null) {
            int seq = 1;
            for (Number tid : teacherIds) {
                Long teacherId = tid.longValue();
                // 1=主监考, 2=副监考
                int role = (mainTeacherId != null && mainTeacherId.equals(teacherId)) ? 1 : 2;

                long invId = IdWorker.getId();
                jdbc.update(
                    "INSERT INTO exam_invigilators (id, room_id, teacher_id, invigilator_role, status) " +
                    "VALUES (?, ?, ?, ?, 1)",
                    invId, roomId, teacherId, role
                );
                seq++;
            }
        }
        return Result.success();
    }
}
