package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.model.task.*;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository.TaskTeacherRepository;
import com.school.management.infrastructure.extension.plugins.education.domain.teaching.repository.TeachingTaskRepository;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.task.TeachingTaskMapper;
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

        List<Map<String, Object>> records = new ArrayList<>(tasks.size());
        for (TeachingTask t : tasks) {
            records.add(toMap(t));
        }
        // 批量 enrich, 避免 N+1
        enrichListWithNames(records);

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

    /**
     * 单条 enrich (用于详情查询).
     */
    private void enrichWithNames(Map<String, Object> record, Long courseId, Long orgUnitId) {
        enrichListWithNames(Collections.singletonList(record));
    }

    /**
     * 批量 enrich: 5 次 IN 查询代替 N×5 单条查询, 解决 N+1.
     * Per-record 字段: courseName/courseCode/assessmentMethod, className,
     *                  roomTypeName, teachers (List<Map>), teacherName (String).
     */
    private void enrichListWithNames(List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) return;

        Set<Long> courseIds  = new HashSet<>();
        Set<Long> orgUnitIds = new HashSet<>();
        Set<Long> taskIds    = new HashSet<>();
        Set<String> roomTypes = new HashSet<>();
        for (Map<String, Object> r : records) {
            Object cid = r.get("courseId");      if (cid != null) courseIds.add(((Number) cid).longValue());
            Object oid = r.get("orgUnitId");     if (oid != null) orgUnitIds.add(((Number) oid).longValue());
            Object tid = r.get("id");            if (tid != null) taskIds.add(((Number) tid).longValue());
            Object rt  = r.get("roomTypeRequired");
            if (rt instanceof String && !((String) rt).isEmpty()) roomTypes.add((String) rt);
        }

        // 1. courses
        Map<Long, Map<String, Object>> coursesById = batchSelect(
                "SELECT id, course_name, course_code, assessment_method FROM courses WHERE id IN ",
                courseIds);

        // 2. org_units (优先) + classes (fallback)
        Map<Long, String> orgUnitNames = batchSelectName(
                "SELECT id, unit_name AS name FROM org_units WHERE id IN ",
                orgUnitIds);
        Set<Long> missing = new HashSet<>(orgUnitIds);
        missing.removeAll(orgUnitNames.keySet());
        if (!missing.isEmpty()) {
            try {
                Map<Long, String> classNames = batchSelectName(
                        "SELECT id, class_name AS name FROM classes WHERE id IN ",
                        missing);
                orgUnitNames.putAll(classNames);
            } catch (Exception ignored) { /* classes 表可能已废弃 */ }
        }

        // 3. room types
        Map<String, String> roomTypeNames = new HashMap<>();
        if (!roomTypes.isEmpty()) {
            String placeholders = String.join(",", Collections.nCopies(roomTypes.size(), "?"));
            String sql = "SELECT type_code, type_name FROM entity_type_configs " +
                    "WHERE entity_type = 'PLACE' AND type_code IN (" + placeholders + ")";
            jdbc.query(sql, roomTypes.toArray(), rs -> {
                roomTypeNames.put(rs.getString("type_code"), rs.getString("type_name"));
            });
        }

        // 4. teachers (一次查所有, 按 task_id 分组)
        Map<Long, List<Map<String, Object>>> teachersByTask = new HashMap<>();
        if (!taskIds.isEmpty()) {
            String placeholders = String.join(",", Collections.nCopies(taskIds.size(), "?"));
            String sql = "SELECT ttt.task_id, ttt.teacher_id, u.real_name, ttt.teacher_role, ttt.weekly_hours " +
                    "FROM teaching_task_teachers ttt " +
                    "JOIN users u ON u.id = ttt.teacher_id " +
                    "WHERE ttt.task_id IN (" + placeholders + ")";
            List<Map<String, Object>> rows = jdbc.queryForList(sql, taskIds.toArray());
            for (Map<String, Object> row : rows) {
                Long taskId = ((Number) row.get("task_id")).longValue();
                teachersByTask.computeIfAbsent(taskId, k -> new ArrayList<>()).add(row);
            }
        }

        // 5. 落到每条 record
        for (Map<String, Object> r : records) {
            Object cid = r.get("courseId");
            if (cid != null) {
                Map<String, Object> course = coursesById.get(((Number) cid).longValue());
                if (course != null) {
                    r.put("courseName", course.get("course_name"));
                    r.put("courseCode", course.get("course_code"));
                    r.put("assessmentMethod", course.get("assessment_method"));
                }
            }
            Object oid = r.get("orgUnitId");
            if (oid != null) {
                r.put("className", orgUnitNames.get(((Number) oid).longValue()));
            }
            Object rt = r.get("roomTypeRequired");
            if (rt instanceof String && !((String) rt).isEmpty()) {
                r.put("roomTypeName", roomTypeNames.get(rt));
            }
            Object tid = r.get("id");
            if (tid != null) {
                List<Map<String, Object>> teachers = teachersByTask.getOrDefault(
                        ((Number) tid).longValue(), Collections.emptyList());
                r.put("teachers", teachers);
                String names = teachers.stream()
                        .map(t -> (String) t.get("real_name"))
                        .filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.joining(", "));
                r.put("teacherName", names.isEmpty() ? null : names);
            } else {
                r.put("teachers", Collections.emptyList());
            }
        }
    }

    private Map<Long, Map<String, Object>> batchSelect(String sqlPrefix, Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        List<Map<String, Object>> rows = jdbc.queryForList(sqlPrefix + "(" + placeholders + ")", ids.toArray());
        Map<Long, Map<String, Object>> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get("id");
            if (id != null) map.put(((Number) id).longValue(), row);
        }
        return map;
    }

    private Map<Long, String> batchSelectName(String sqlPrefix, Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return new HashMap<>();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        Map<Long, String> map = new HashMap<>();
        jdbc.query(sqlPrefix + "(" + placeholders + ")", ids.toArray(), rs -> {
            map.put(rs.getLong("id"), rs.getString("name"));
        });
        return map;
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
