package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.exception.TeachingDomainException;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.progress.TeachingProgressMapper;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.progress.TeachingProgressPO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 教学进度跟踪.
 *   - 教师按周/课节记录实际授课内容
 *   - 进度查询: 按任务/班级/学期统计完成度
 *   - enrich 课程/班级/教师名称做批量查询 (无 N+1)
 */
@Service
@RequiredArgsConstructor
public class TeachingProgressService {

    private final TeachingProgressMapper mapper;
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<TeachingProgressPO> listByTask(Long taskId) {
        LambdaQueryWrapper<TeachingProgressPO> w = new LambdaQueryWrapper<TeachingProgressPO>()
                .eq(TeachingProgressPO::getTaskId, taskId)
                .orderByAsc(TeachingProgressPO::getWeekNumber)
                .orderByAsc(TeachingProgressPO::getLessonNo);
        List<TeachingProgressPO> rows = mapper.selectList(w);
        enrich(rows);
        return rows;
    }

    @Transactional(readOnly = true)
    public List<TeachingProgressPO> listBySemester(Long semesterId, Long orgUnitId,
                                                   Integer weekNumber, Integer status) {
        LambdaQueryWrapper<TeachingProgressPO> w = new LambdaQueryWrapper<TeachingProgressPO>()
                .eq(TeachingProgressPO::getSemesterId, semesterId)
                .orderByAsc(TeachingProgressPO::getWeekNumber)
                .orderByAsc(TeachingProgressPO::getLessonNo);
        if (orgUnitId != null) w.eq(TeachingProgressPO::getOrgUnitId, orgUnitId);
        if (weekNumber != null) w.eq(TeachingProgressPO::getWeekNumber, weekNumber);
        if (status != null) w.eq(TeachingProgressPO::getProgressStatus, status);
        List<TeachingProgressPO> rows = mapper.selectList(w);
        enrich(rows);
        return rows;
    }

    @Transactional
    public TeachingProgressPO create(TeachingProgressPO body, Long userId) {
        if (body.getTaskId() == null) {
            throw new TeachingDomainException("taskId 不能为空");
        }
        // 自动派生 semesterId / orgUnitId
        if (body.getSemesterId() == null || body.getOrgUnitId() == null) {
            try {
                Map<String, Object> task = jdbc.queryForMap(
                    "SELECT semester_id, org_unit_id FROM teaching_tasks WHERE id = ? AND deleted = 0",
                    body.getTaskId());
                if (body.getSemesterId() == null) {
                    body.setSemesterId(((Number) task.get("semester_id")).longValue());
                }
                if (body.getOrgUnitId() == null && task.get("org_unit_id") != null) {
                    body.setOrgUnitId(((Number) task.get("org_unit_id")).longValue());
                }
            } catch (Exception e) {
                throw TeachingDomainException.taskNotFound(body.getTaskId());
            }
        }
        if (body.getProgressStatus() == null) body.setProgressStatus(0);
        if (body.getLessonNo() == null) body.setLessonNo(1);
        if (body.getRecordedBy() == null) body.setRecordedBy(userId);
        body.setCreatedBy(userId);
        if (body.getProgressStatus() != null && body.getProgressStatus() == 1 && body.getRecordedAt() == null) {
            body.setRecordedAt(LocalDateTime.now());
        }
        mapper.insert(body);
        return body;
    }

    @Transactional
    public TeachingProgressPO update(Long id, TeachingProgressPO body, Long userId) {
        TeachingProgressPO po = mapper.selectById(id);
        if (po == null) throw new TeachingDomainException("教学进度记录不存在: " + id);
        if (body.getActualTopic() != null) po.setActualTopic(body.getActualTopic());
        if (body.getChapter() != null) po.setChapter(body.getChapter());
        if (body.getProgressStatus() != null) {
            po.setProgressStatus(body.getProgressStatus());
            if (body.getProgressStatus() == 1 && po.getRecordedAt() == null) {
                po.setRecordedAt(LocalDateTime.now());
                po.setRecordedBy(userId);
            }
        }
        if (body.getAttendanceCount() != null) po.setAttendanceCount(body.getAttendanceCount());
        if (body.getTotalStudents() != null) po.setTotalStudents(body.getTotalStudents());
        if (body.getNote() != null) po.setNote(body.getNote());
        mapper.updateById(po);
        return po;
    }

    @Transactional
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    /** 任务完成度统计 */
    @Transactional(readOnly = true)
    public Map<String, Object> taskSummary(Long taskId) {
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT progress_status, COUNT(*) AS cnt FROM teaching_progress " +
                "WHERE task_id = ? AND deleted = 0 GROUP BY progress_status", taskId);
            int total = 0, done = 0, missed = 0, adjusted = 0;
            for (Map<String, Object> r : rows) {
                int st = ((Number) r.get("progress_status")).intValue();
                int cnt = ((Number) r.get("cnt")).intValue();
                total += cnt;
                if (st == 1) done = cnt;
                else if (st == 2) missed = cnt;
                else if (st == 3) adjusted = cnt;
            }
            result.put("totalLessons", total);
            result.put("completed", done);
            result.put("missed", missed);
            result.put("adjusted", adjusted);
            result.put("completionRate", total > 0 ? Math.round(100.0 * done / total) : 0);
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    /* ==================== enrich (无 N+1) ==================== */
    private void enrich(List<TeachingProgressPO> rows) {
        if (rows == null || rows.isEmpty()) return;
        Set<Long> taskIds = new HashSet<>();
        Set<Long> orgUnitIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (TeachingProgressPO p : rows) {
            if (p.getTaskId() != null) taskIds.add(p.getTaskId());
            if (p.getOrgUnitId() != null) orgUnitIds.add(p.getOrgUnitId());
            if (p.getRecordedBy() != null) userIds.add(p.getRecordedBy());
        }

        Map<Long, String> courseByTask = new HashMap<>();
        if (!taskIds.isEmpty()) {
            String ph = String.join(",", Collections.nCopies(taskIds.size(), "?"));
            try {
                List<Map<String, Object>> r = jdbc.queryForList(
                    "SELECT t.id, c.course_name FROM teaching_tasks t " +
                    "LEFT JOIN courses c ON c.id = t.course_id " +
                    "WHERE t.id IN (" + ph + ")", taskIds.toArray());
                for (Map<String, Object> row : r) {
                    courseByTask.put(((Number) row.get("id")).longValue(), (String) row.get("course_name"));
                }
            } catch (Exception ignored) {}
        }
        Map<Long, String> orgNames = batchName("org_units", "unit_name", orgUnitIds);
        Map<Long, String> teacherNames = batchName("users", "COALESCE(real_name, username)", userIds);

        for (TeachingProgressPO p : rows) {
            if (p.getTaskId() != null) p.setCourseName(courseByTask.get(p.getTaskId()));
            if (p.getOrgUnitId() != null) p.setOrgUnitName(orgNames.get(p.getOrgUnitId()));
            if (p.getRecordedBy() != null) p.setTeacherName(teacherNames.get(p.getRecordedBy()));
        }
    }

    private Map<Long, String> batchName(String table, String nameExpr, Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        String ph = String.join(",", Collections.nCopies(ids.size(), "?"));
        Map<Long, String> map = new HashMap<>();
        try {
            org.springframework.jdbc.core.RowCallbackHandler handler = rs ->
                map.put(rs.getLong("id"), rs.getString("name"));
            jdbc.query(
                "SELECT id, " + nameExpr + " AS name FROM " + table + " WHERE id IN (" + ph + ")",
                handler, ids.toArray());
        } catch (Exception ignored) {}
        return map;
    }
}
