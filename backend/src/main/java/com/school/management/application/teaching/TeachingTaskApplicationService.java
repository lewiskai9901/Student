package com.school.management.application.teaching;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.domain.teaching.model.task.*;
import com.school.management.domain.teaching.repository.TaskTeacherRepository;
import com.school.management.domain.teaching.repository.TeachingTaskRepository;
import com.school.management.infrastructure.persistence.teaching.task.TeachingTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class TeachingTaskApplicationService {
    private final TeachingTaskRepository taskRepo;
    private final TaskTeacherRepository teacherRepo;
    private final TeachingTaskMapper taskMapper;
    private final JdbcTemplate jdbc;

    /**
     * 分页查询教学任务（带课程/班级名称）
     */
    @Transactional(readOnly = true)
    public Map<String, Object> list(Long semesterId, Integer status, int page, int size) {
        long total = taskRepo.countByFilter(semesterId, status);
        int offset = (page - 1) * size;
        List<TeachingTask> tasks = taskRepo.findByFilter(semesterId, status, offset, size);

        // 补充课程名称和班级名称
        List<Map<String, Object>> records = new ArrayList<>();
        for (TeachingTask t : tasks) {
            Map<String, Object> record = toMap(t);
            enrichWithNames(record, t.getCourseId(), t.getOrgUnitId());
            records.add(record);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    /**
     * 获取教学任务详情（含课程/班级名称）
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getById(Long id) {
        TeachingTask task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("教学任务不存在: " + id));
        Map<String, Object> result = toMap(task);
        enrichWithNames(result, task.getCourseId(), task.getOrgUnitId());
        return result;
    }

    /**
     * 创建教学任务
     */
    public Map<String, Object> create(Map<String, Object> data, Long userId) {
        long id = IdWorker.getId();
        String taskCode = "TT" + id;

        TeachingTask task = TeachingTask.create(
                taskCode,
                toLong(data.get("semesterId")),
                toLong(data.get("courseId")),
                toLong(data.get("orgUnitId")),
                toInt(data.get("studentCount")),
                toInt(data.get("weeklyHours")),
                toInt(data.get("totalHours")),
                toInt(data.get("startWeek")),
                toInt(data.get("endWeek")),
                data.get("taskStatus") != null ? TaskStatus.fromCode(toInt(data.get("taskStatus"))) : TaskStatus.CONFIRMED,
                (String) data.get("remark"),
                userId
        );
        task.setId(id);
        TeachingTask saved = taskRepo.save(task);

        Map<String, Object> result = new HashMap<>(data);
        result.put("id", saved.getId());
        result.put("taskCode", taskCode);
        return result;
    }

    /**
     * 更新教学任务
     */
    public void update(Long id, Map<String, Object> data) {
        TeachingTask task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("教学任务不存在: " + id));
        task.update(
                toLong(data.get("courseId")),
                toLong(data.get("orgUnitId")),
                toInt(data.get("studentCount")),
                toInt(data.get("weeklyHours")),
                toInt(data.get("totalHours")),
                toInt(data.get("startWeek")),
                toInt(data.get("endWeek")),
                (String) data.get("remark")
        );
        taskRepo.save(task);
    }

    /**
     * 删除教学任务（逻辑删除）
     */
    public void delete(Long id) {
        taskRepo.deleteById(id);
    }

    /**
     * 更新任务状态
     */
    public void updateStatus(Long id, int statusCode) {
        TeachingTask task = taskRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("教学任务不存在: " + id));
        task.updateStatus(TaskStatus.fromCode(statusCode));
        taskRepo.save(task);
    }

    /**
     * 分配教师（替换所有）
     */
    public void assignTeachers(Long taskId, List<Number> teacherIds, Number mainTeacherId) {
        // 先删除旧分配
        teacherRepo.deleteByTaskId(taskId);

        if (teacherIds != null) {
            for (Number tid : teacherIds) {
                long teacherId = tid.longValue();
                int teacherRole = (mainTeacherId != null && teacherId == mainTeacherId.longValue()) ? 1 : 2;
                TaskTeacher tt = TaskTeacher.create(taskId, teacherId, teacherRole);
                teacherRepo.save(tt);
            }
        }
    }

    /**
     * 移除教师
     */
    public void removeTeacher(Long taskId, Long teacherId) {
        teacherRepo.deleteByTaskIdAndTeacherId(taskId, teacherId);
    }

    /**
     * 批量创建教学任务（从课程计划）
     */
    public List<Map<String, Object>> batchCreate(Long semesterId, Long planId,
            List<Number> classIds, Long userId) {
        // 从课程计划获取课程列表
        List<Map<String, Object>> planCourses = taskMapper.selectPlanCourses(planId);

        List<Map<String, Object>> createdTasks = new ArrayList<>();

        for (Number clsId : classIds) {
            long orgUnitId = clsId.longValue();
            for (Map<String, Object> pc : planCourses) {
                long taskId = IdWorker.getId();
                String taskCode = "TT" + taskId;
                Long courseId = ((Number) pc.get("course_id")).longValue();
                Integer weeklyHours = pc.get("weekly_hours") != null ? ((Number) pc.get("weekly_hours")).intValue() : null;
                Integer totalHours = pc.get("total_hours") != null ? ((Number) pc.get("total_hours")).intValue() : null;

                TeachingTask task = TeachingTask.create(
                        taskCode, semesterId, courseId, orgUnitId,
                        0, weeklyHours, totalHours, 1, 16,
                        TaskStatus.CONFIRMED, null, userId
                );
                task.setId(taskId);
                taskRepo.save(task);

                Map<String, Object> result = new HashMap<>();
                result.put("id", taskId);
                result.put("taskCode", taskCode);
                result.put("semesterId", semesterId);
                result.put("courseId", courseId);
                result.put("orgUnitId", orgUnitId);
                createdTasks.add(result);
            }
        }

        return createdTasks;
    }

    // ==================== Private helpers ====================

    private Map<String, Object> toMap(TeachingTask t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getId());
        map.put("taskCode", t.getTaskCode());
        map.put("semesterId", t.getSemesterId());
        map.put("courseId", t.getCourseId());
        map.put("orgUnitId", t.getOrgUnitId());
        map.put("orgUnitId", t.getOrgUnitId());
        map.put("studentCount", t.getStudentCount());
        map.put("weeklyHours", t.getWeeklyHours());
        map.put("totalHours", t.getTotalHours());
        map.put("startWeek", t.getStartWeek());
        map.put("endWeek", t.getEndWeek());
        map.put("schedulingStatus", t.getSchedulingStatus() != null ? t.getSchedulingStatus().getCode() : 0);
        map.put("taskStatus", t.getTaskStatus() != null ? t.getTaskStatus().getCode() : 1);
        map.put("remark", t.getRemark());
        return map;
    }

    private void enrichWithNames(Map<String, Object> record, Long courseId, Long orgUnitId) {
        try {
            if (courseId != null) {
                String courseName = jdbc.queryForObject(
                        "SELECT course_name FROM courses WHERE id = ?", String.class, courseId);
                record.put("courseName", courseName);
            }
        } catch (Exception e) {
            record.put("courseName", null);
        }
        try {
            if (orgUnitId != null) {
                String className = jdbc.queryForObject(
                        "SELECT class_name FROM classes WHERE id = ?", String.class, orgUnitId);
                record.put("className", className);
            }
        } catch (Exception e) {
            record.put("className", null);
        }
    }

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
}
