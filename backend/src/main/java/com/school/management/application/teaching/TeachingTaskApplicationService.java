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
                (String) data.get("roomTypeRequired"),
                toInt(data.get("consecutivePeriods")),
                toInt(data.get("courseNature")),
                data.get("taskStatus") != null ? TaskStatus.fromCode(toInt(data.get("taskStatus"))) : TaskStatus.PENDING,
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
                (String) data.get("roomTypeRequired"),
                toInt(data.get("consecutivePeriods")),
                toInt(data.get("courseNature")),
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
     * 分配教师（替换所有）— 支持多教师+各自课时
     * 请求体: { "teachers": [{ "teacherId": "xxx", "role": 1, "weeklyHours": 2 }, ...] }
     */
    @SuppressWarnings("unchecked")
    public void assignTeachers(Long taskId, List<Map<String, Object>> teachers) {
        // 先删除旧分配
        teacherRepo.deleteByTaskId(taskId);

        if (teachers != null && !teachers.isEmpty()) {
            for (Map<String, Object> t : teachers) {
                Long teacherId = toLong(t.get("teacherId"));
                int role = t.get("role") != null ? toInt(t.get("role")) : 2;
                Integer weeklyHours = toInt(t.get("weeklyHours"));
                TaskTeacher tt = TaskTeacher.create(taskId, teacherId, role, weeklyHours);
                teacherRepo.save(tt);
            }
            // 自动更新任务状态为"已分配教师"
            taskRepo.findById(taskId).ifPresent(task -> {
                if (task.getTaskStatus() == TaskStatus.PENDING) {
                    task.updateStatus(TaskStatus.TEACHER_ASSIGNED);
                    taskRepo.save(task);
                }
            });
        } else {
            // 清空教师后回退为"待落实"
            taskRepo.findById(taskId).ifPresent(task -> {
                if (task.getTaskStatus() == TaskStatus.TEACHER_ASSIGNED) {
                    task.updateStatus(TaskStatus.PENDING);
                    taskRepo.save(task);
                }
            });
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
                        null, null, null,
                        TaskStatus.PENDING, null, userId
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
        map.put("studentCount", t.getStudentCount());
        map.put("weeklyHours", t.getWeeklyHours());
        map.put("totalHours", t.getTotalHours());
        map.put("startWeek", t.getStartWeek());
        map.put("endWeek", t.getEndWeek());
        map.put("roomTypeRequired", t.getRoomTypeRequired());
        map.put("consecutivePeriods", t.getConsecutivePeriods());
        map.put("courseNature", t.getCourseNature());
        map.put("schedulingStatus", t.getSchedulingStatus() != null ? t.getSchedulingStatus().getCode() : 0);
        map.put("taskStatus", t.getTaskStatus() != null ? t.getTaskStatus().getCode() : 0);
        map.put("remark", t.getRemark());
        return map;
    }

    private void enrichWithNames(Map<String, Object> record, Long courseId, Long orgUnitId) {
        // 课程名称 + 考核方式
        try {
            if (courseId != null) {
                Map<String, Object> course = jdbc.queryForMap(
                        "SELECT course_name, course_code, assessment_method FROM courses WHERE id = ?", courseId);
                record.put("courseName", course.get("course_name"));
                record.put("courseCode", course.get("course_code"));
                record.put("assessmentMethod", course.get("assessment_method"));
            }
        } catch (Exception e) {
            record.put("courseName", null);
        }
        // 班级名称
        try {
            if (orgUnitId != null) {
                String className = null;
                try {
                    className = jdbc.queryForObject(
                            "SELECT unit_name FROM org_units WHERE id = ?", String.class, orgUnitId);
                } catch (Exception ignored) {}
                if (className == null) {
                    try {
                        className = jdbc.queryForObject(
                                "SELECT class_name FROM classes WHERE id = ?", String.class, orgUnitId);
                    } catch (Exception ignored) {}
                }
                record.put("className", className);
            }
        } catch (Exception e) {
            record.put("className", null);
        }
        // 教室类型名称
        try {
            String roomType = (String) record.get("roomTypeRequired");
            if (roomType != null && !roomType.isEmpty()) {
                String roomTypeName = jdbc.queryForObject(
                    "SELECT type_name FROM entity_type_configs WHERE type_code = ? AND entity_type = 'PLACE' LIMIT 1",
                    String.class, roomType);
                record.put("roomTypeName", roomTypeName);
            }
        } catch (Exception e) {
            record.put("roomTypeName", null);
        }
        // 教师列表（含课时数）
        try {
            Long taskId = (Long) record.get("id");
            if (taskId != null) {
                List<Map<String, Object>> teacherList = jdbc.queryForList(
                    "SELECT ttt.teacher_id, u.real_name, ttt.teacher_role, ttt.weekly_hours " +
                    "FROM teaching_task_teachers ttt " +
                    "JOIN users u ON u.id = ttt.teacher_id " +
                    "WHERE ttt.task_id = ?", taskId);
                record.put("teachers", teacherList);
                // 保留 teacherName 兼容
                String names = teacherList.stream()
                    .map(t -> (String) t.get("real_name"))
                    .filter(n -> n != null)
                    .collect(java.util.stream.Collectors.joining(", "));
                record.put("teacherName", names.isEmpty() ? null : names);
            }
        } catch (Exception e) {
            record.put("teachers", List.of());
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
