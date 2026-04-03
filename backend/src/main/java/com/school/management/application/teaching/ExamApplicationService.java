package com.school.management.application.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.infrastructure.persistence.teaching.exam.ExamArrangementMapper;
import com.school.management.infrastructure.persistence.teaching.exam.ExamArrangementPO;
import com.school.management.infrastructure.persistence.teaching.exam.ExamBatchMapper;
import com.school.management.infrastructure.persistence.teaching.exam.ExamBatchPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamApplicationService {

    private final ExamBatchMapper batchMapper;
    private final ExamArrangementMapper arrangementMapper;
    private final JdbcTemplate jdbc; // for exam_rooms and exam_invigilators join tables

    // ==================== Batch Methods ====================

    public Map<String, Object> listBatches(Long semesterId, Integer examType, Integer status,
                                           int pageNum, int pageSize) {
        LambdaQueryWrapper<ExamBatchPO> wrapper = new LambdaQueryWrapper<>();
        if (semesterId != null) wrapper.eq(ExamBatchPO::getSemesterId, semesterId);
        if (examType != null) wrapper.eq(ExamBatchPO::getExamType, examType);
        if (status != null) wrapper.eq(ExamBatchPO::getStatus, status);
        wrapper.orderByDesc(ExamBatchPO::getCreatedAt);

        Page<ExamBatchPO> page = batchMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        return result;
    }

    public ExamBatchPO getBatch(Long id) {
        return batchMapper.selectById(id);
    }

    @Transactional
    public ExamBatchPO createBatch(Map<String, Object> data, Long currentUserId) {
        ExamBatchPO po = new ExamBatchPO();
        long id = IdWorker.getId();
        po.setId(id);
        po.setBatchCode("EX" + id);
        po.setBatchName((String) data.get("batchName"));
        po.setSemesterId(toLong(data.get("semesterId")));
        po.setExamType(toInt(data.get("examType"), 1));
        po.setStartDate(toLocalDate(data.get("startDate")));
        po.setEndDate(toLocalDate(data.get("endDate")));
        po.setStatus(toInt(data.get("status"), 0));
        po.setRemark((String) data.get("remark"));
        po.setCreatedBy(currentUserId);
        po.setDeleted(0);

        batchMapper.insert(po);
        return po;
    }

    @Transactional
    public void updateBatch(Long id, Map<String, Object> data) {
        ExamBatchPO po = batchMapper.selectById(id);
        if (po == null) throw new RuntimeException("考试批次不存在: " + id);

        po.setBatchName((String) data.get("batchName"));
        po.setSemesterId(toLong(data.get("semesterId")));
        po.setExamType(toIntOrNull(data.get("examType")));
        po.setStartDate(toLocalDate(data.get("startDate")));
        po.setEndDate(toLocalDate(data.get("endDate")));
        po.setStatus(toIntOrNull(data.get("status")));
        po.setRemark((String) data.get("remark"));

        batchMapper.updateById(po);
    }

    @Transactional
    public void deleteBatch(Long id) {
        batchMapper.deleteById(id); // logical delete via @TableLogic
    }

    @Transactional
    public void publishBatch(Long id) {
        ExamBatchPO po = batchMapper.selectById(id);
        if (po == null) throw new RuntimeException("考试批次不存在: " + id);
        po.setStatus(2);
        batchMapper.updateById(po);
    }

    // ==================== Arrangement Methods ====================

    public List<Map<String, Object>> listArrangements(Long batchId) {
        try {
            return arrangementMapper.listWithCourseName(batchId);
        } catch (Exception e) {
            // Fallback without JOIN if courses table has different structure
            log.warn("Failed to query arrangements with course join, falling back: {}", e.getMessage());
            LambdaQueryWrapper<ExamArrangementPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ExamArrangementPO::getBatchId, batchId)
                   .orderByAsc(ExamArrangementPO::getExamDate)
                   .orderByAsc(ExamArrangementPO::getStartTime);
            List<ExamArrangementPO> list = arrangementMapper.selectList(wrapper);
            List<Map<String, Object>> result = new ArrayList<>();
            for (ExamArrangementPO a : list) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", a.getId());
                map.put("batchId", a.getBatchId());
                map.put("courseId", a.getCourseId());
                map.put("examDate", a.getExamDate());
                map.put("startTime", a.getStartTime());
                map.put("endTime", a.getEndTime());
                map.put("duration", a.getDuration());
                map.put("examForm", a.getExamForm());
                map.put("totalStudents", a.getTotalStudents());
                map.put("remark", a.getRemark());
                map.put("status", a.getStatus());
                map.put("createdAt", a.getCreatedAt());
                result.add(map);
            }
            return result;
        }
    }

    @Transactional
    public ExamArrangementPO createArrangement(Long batchId, Map<String, Object> data, Long currentUserId) {
        ExamArrangementPO po = new ExamArrangementPO();
        po.setBatchId(batchId);
        po.setCourseId(toLong(data.get("courseId")));
        po.setExamDate(toLocalDate(data.get("examDate")));
        po.setStartTime(toLocalTime(data.get("startTime")));
        po.setEndTime(toLocalTime(data.get("endTime")));
        po.setDuration(toIntOrNull(data.get("duration")));
        po.setExamForm(toInt(data.get("examForm"), 1));
        po.setTotalStudents(toInt(data.get("totalStudents"), 0));
        po.setRemark((String) data.get("remark"));
        po.setStatus(1);
        po.setCreatedBy(currentUserId);

        arrangementMapper.insert(po);
        return po;
    }

    @Transactional
    public void updateArrangement(Long batchId, Long arrangementId, Map<String, Object> data) {
        LambdaQueryWrapper<ExamArrangementPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamArrangementPO::getId, arrangementId)
               .eq(ExamArrangementPO::getBatchId, batchId);
        ExamArrangementPO po = arrangementMapper.selectOne(wrapper);
        if (po == null) throw new RuntimeException("考试安排不存在: " + arrangementId);

        po.setCourseId(toLong(data.get("courseId")));
        po.setExamDate(toLocalDate(data.get("examDate")));
        po.setStartTime(toLocalTime(data.get("startTime")));
        po.setEndTime(toLocalTime(data.get("endTime")));
        po.setDuration(toIntOrNull(data.get("duration")));
        po.setExamForm(toIntOrNull(data.get("examForm")));
        po.setTotalStudents(toIntOrNull(data.get("totalStudents")));
        po.setRemark((String) data.get("remark"));

        arrangementMapper.updateById(po);
    }

    @Transactional
    public void deleteArrangement(Long batchId, Long arrangementId) {
        LambdaQueryWrapper<ExamArrangementPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExamArrangementPO::getId, arrangementId)
               .eq(ExamArrangementPO::getBatchId, batchId);
        arrangementMapper.delete(wrapper); // physical delete, matching original behavior
    }

    // ==================== Room & Invigilator Methods (via JdbcTemplate) ====================

    @Transactional
    public void assignRooms(Long arrangementId, List<Map<String, Object>> rooms) {
        jdbc.update("DELETE FROM exam_rooms WHERE arrangement_id = ?", arrangementId);

        if (rooms != null) {
            int seq = 1;
            for (Map<String, Object> room : rooms) {
                Long classroomId = toLong(room.get("classroomId"));
                Integer capacity = toInt(room.get("capacity"), 0);
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
    }

    @Transactional
    public void assignInvigilators(Long roomId, List<Number> teacherIds, Long mainTeacherId) {
        jdbc.update("DELETE FROM exam_invigilators WHERE room_id = ?", roomId);

        if (teacherIds != null) {
            for (Number tid : teacherIds) {
                Long teacherId = tid.longValue();
                int role = (mainTeacherId != null && mainTeacherId.equals(teacherId)) ? 1 : 2;

                long invId = IdWorker.getId();
                jdbc.update(
                    "INSERT INTO exam_invigilators (id, room_id, teacher_id, invigilator_role, status) " +
                    "VALUES (?, ?, ?, ?, 1)",
                    invId, roomId, teacherId, role
                );
            }
        }
    }

    // ==================== Utility Methods ====================

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private Integer toIntOrNull(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }

    private int toInt(Object val, int defaultVal) {
        if (val == null) return defaultVal;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }

    private LocalDate toLocalDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate) return (LocalDate) val;
        return LocalDate.parse(val.toString());
    }

    private LocalTime toLocalTime(Object val) {
        if (val == null) return null;
        if (val instanceof LocalTime) return (LocalTime) val;
        return LocalTime.parse(val.toString());
    }
}
