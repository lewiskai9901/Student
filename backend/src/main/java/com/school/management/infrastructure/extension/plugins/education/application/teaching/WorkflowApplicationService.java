package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 教务工作流应用服务 (M3.2.1, 2026-05-20).
 * WorkflowController 8 处直 jdbc 全部下沉到此. 仅负责读统计 SQL,
 * 真正的流水线编排仍由 {@link TeachingWorkflowService} 承担.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowApplicationService {

    private final JdbcTemplate jdbc;

    /**
     * 流水线统计 — 返回 offerings / assignments / tasks 三段计数 + currentStep + teachingWeeks.
     */
    public Map<String, Object> getStats(Long semesterId) {
        Long offeringTotal = jdbc.queryForObject(
            "SELECT COUNT(1) FROM semester_course_offerings WHERE semester_id = ? AND deleted = 0",
            Long.class, semesterId);
        Long offeringConfirmed = jdbc.queryForObject(
            "SELECT COUNT(1) FROM semester_course_offerings WHERE semester_id = ? AND deleted = 0 AND status = 1",
            Long.class, semesterId);
        Long assignTotal = jdbc.queryForObject(
            "SELECT COUNT(1) FROM class_course_assignments WHERE semester_id = ?",
            Long.class, semesterId);
        Long assignConfirmed = jdbc.queryForObject(
            "SELECT COUNT(1) FROM class_course_assignments WHERE semester_id = ? AND status = 1",
            Long.class, semesterId);
        Long taskTotal = jdbc.queryForObject(
            "SELECT COUNT(1) FROM teaching_tasks WHERE semester_id = ? AND deleted = 0",
            Long.class, semesterId);
        Long teacherAssigned = jdbc.queryForObject(
            "SELECT COUNT(DISTINCT tt.id) FROM teaching_tasks tt " +
            "JOIN teaching_task_teachers ttt ON ttt.task_id = tt.id " +
            "WHERE tt.semester_id = ? AND tt.deleted = 0",
            Long.class, semesterId);
        Long scheduled = jdbc.queryForObject(
            "SELECT COUNT(1) FROM teaching_tasks WHERE semester_id = ? AND deleted = 0 AND scheduling_status = 2",
            Long.class, semesterId);

        long ot = offeringTotal == null ? 0 : offeringTotal;
        long oc = offeringConfirmed == null ? 0 : offeringConfirmed;
        long at = assignTotal == null ? 0 : assignTotal;
        long ac = assignConfirmed == null ? 0 : assignConfirmed;
        long tt = taskTotal == null ? 0 : taskTotal;
        long ta = teacherAssigned == null ? 0 : teacherAssigned;
        long sc = scheduled == null ? 0 : scheduled;

        String currentStep;
        if (ot == 0) currentStep = "offering_create";
        else if (oc < ot) currentStep = "offering_confirm";
        else if (at == 0) currentStep = "class_assign";
        else if (tt == 0) currentStep = "generate_tasks";
        else if (ta < tt) currentStep = "teacher_assign";
        else if (sc < tt) currentStep = "scheduling";
        else currentStep = "done";

        int teachingWeeks = 16;
        try {
            Integer tw = jdbc.queryForObject(
                "SELECT MAX(week_number) FROM academic_weeks WHERE semester_id = ? AND week_type = 1",
                Integer.class, semesterId);
            if (tw != null && tw > 0) teachingWeeks = tw;
        } catch (Exception ignored) {}

        Map<String, Object> result = new HashMap<>();
        result.put("offerings", Map.of("total", ot, "confirmed", oc));
        result.put("assignments", Map.of("total", at, "confirmed", ac));
        result.put("tasks", Map.of("total", tt, "teacherAssigned", ta, "scheduled", sc));
        result.put("currentStep", currentStep);
        result.put("teachingWeeks", teachingWeeks);
        return result;
    }
}
