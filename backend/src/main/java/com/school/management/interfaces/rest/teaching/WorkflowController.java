package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.TeachingWorkflowService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.persistence.teaching.CohortSemesterMappingMapper;
import com.school.management.infrastructure.persistence.teaching.CohortSemesterMappingPO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教务工作流 Controller
 *
 * 串联: 培养方案 → 开课 → 教学任务 → 考试 → 成绩 的全链路操作
 */
@RestController
@RequestMapping("/teaching/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final TeachingWorkflowService workflowService;
    private final CohortSemesterMappingMapper mappingMapper;
    private final JdbcTemplate jdbc;

    // ==================== 流水线统计 ====================

    @GetMapping("/stats")
    @CasbinAccess(resource = "teaching:workflow", action = "view")
    public Result<Map<String, Object>> getStats(@RequestParam Long semesterId) {
        Long offeringTotal = jdbc.queryForObject(
            "SELECT COUNT(1) FROM semester_course_offerings WHERE semester_id = ? AND deleted = 0", Long.class, semesterId);
        Long offeringConfirmed = jdbc.queryForObject(
            "SELECT COUNT(1) FROM semester_course_offerings WHERE semester_id = ? AND deleted = 0 AND status = 1", Long.class, semesterId);
        Long assignTotal = jdbc.queryForObject(
            "SELECT COUNT(1) FROM class_course_assignments WHERE semester_id = ?", Long.class, semesterId);
        Long assignConfirmed = jdbc.queryForObject(
            "SELECT COUNT(1) FROM class_course_assignments WHERE semester_id = ? AND status = 1", Long.class, semesterId);
        Long taskTotal = jdbc.queryForObject(
            "SELECT COUNT(1) FROM teaching_tasks WHERE semester_id = ? AND deleted = 0", Long.class, semesterId);
        Long teacherAssigned = jdbc.queryForObject(
            "SELECT COUNT(DISTINCT tt.id) FROM teaching_tasks tt JOIN teaching_task_teachers ttt ON ttt.task_id = tt.id WHERE tt.semester_id = ? AND tt.deleted = 0", Long.class, semesterId);
        Long scheduled = jdbc.queryForObject(
            "SELECT COUNT(1) FROM teaching_tasks WHERE semester_id = ? AND deleted = 0 AND scheduling_status = 2", Long.class, semesterId);

        String currentStep;
        if (offeringTotal == 0) currentStep = "offering_create";
        else if (offeringConfirmed < offeringTotal) currentStep = "offering_confirm";
        else if (assignTotal == 0) currentStep = "class_assign";
        else if (taskTotal == 0) currentStep = "generate_tasks";
        else if (teacherAssigned < taskTotal) currentStep = "teacher_assign";
        else if (scheduled < taskTotal) currentStep = "scheduling";
        else currentStep = "done";

        // 从 academic_weeks 的 week_type 直接读取教学周数
        int teachingWeeks = 16;
        try {
            Integer tw = jdbc.queryForObject(
                "SELECT MAX(week_number) FROM academic_weeks WHERE semester_id = ? AND week_type = 1",
                Integer.class, semesterId);
            if (tw != null && tw > 0) teachingWeeks = tw;
        } catch (Exception ignored) {}

        Map<String, Object> result = new HashMap<>();
        result.put("offerings", Map.of("total", offeringTotal, "confirmed", offeringConfirmed));
        result.put("assignments", Map.of("total", assignTotal, "confirmed", assignConfirmed));
        result.put("tasks", Map.of("total", taskTotal, "teacherAssigned", teacherAssigned, "scheduled", scheduled));
        result.put("currentStep", currentStep);
        result.put("teachingWeeks", teachingWeeks);
        return Result.success(result);
    }

    // ==================== 年级-学期映射 ====================

    /**
     * 自动生成当前学期所有年级的映射
     */
    @PostMapping("/cohort-mappings/generate")
    @CasbinAccess(resource = "teaching:workflow", action = "edit")
    public Result<Map<String, Object>> generateMappings(@RequestBody Map<String, Object> data) {
        Long semesterId = toLong(data.get("semesterId"));
        int count = workflowService.generateCohortSemesterMappings(semesterId);
        return Result.success(Map.of("generated", count));
    }

    /**
     * 查看某学期的年级映射
     */
    @GetMapping("/cohort-mappings")
    @CasbinAccess(resource = "teaching:workflow", action = "view")
    public Result<List<CohortSemesterMappingPO>> getMappings(@RequestParam Long semesterId) {
        return Result.success(mappingMapper.findBySemesterId(semesterId));
    }

    // ==================== 工作流操作 ====================

    /**
     * Step 1: 从培养方案自动导入开课计划
     */
    @PostMapping("/generate-offerings")
    @CasbinAccess(resource = "teaching:workflow", action = "edit")
    public Result<Map<String, Object>> generateOfferings(@RequestBody Map<String, Object> data) {
        Long semesterId = toLong(data.get("semesterId"));
        Long createdBy = toLong(data.get("userId"));
        int count = workflowService.generateOfferingsFromMappings(semesterId, createdBy);
        return Result.success(Map.of("generated", count));
    }

    /**
     * Step 2: 从开课计划批量生成教学任务
     */
    @PostMapping("/generate-tasks")
    @CasbinAccess(resource = "teaching:workflow", action = "edit")
    public Result<Map<String, Object>> generateTasks(@RequestBody Map<String, Object> data) {
        Long semesterId = toLong(data.get("semesterId"));
        Long createdBy = toLong(data.get("userId"));
        int count = workflowService.generateTasksFromOfferings(semesterId, createdBy);
        return Result.success(Map.of("generated", count));
    }

    /**
     * Step 3: 从教学任务批量创建考试安排
     */
    @PostMapping("/generate-exams")
    @CasbinAccess(resource = "teaching:workflow", action = "edit")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> generateExams(@RequestBody Map<String, Object> data) {
        Long batchId = toLong(data.get("batchId"));
        List<Number> taskIds = (List<Number>) data.get("taskIds");
        Long createdBy = toLong(data.get("userId"));
        List<Long> ids = taskIds.stream().map(Number::longValue).toList();
        int count = workflowService.generateExamFromTasks(batchId, ids, createdBy);
        return Result.success(Map.of("generated", count));
    }

    /**
     * Step 4: 从考试批次创建成绩批次
     */
    @PostMapping("/generate-grade-batch")
    @CasbinAccess(resource = "teaching:workflow", action = "edit")
    public Result<Map<String, Object>> generateGradeBatch(@RequestBody Map<String, Object> data) {
        Long examBatchId = toLong(data.get("examBatchId"));
        Long createdBy = toLong(data.get("userId"));
        Long gradeBatchId = workflowService.generateGradeBatchFromExam(examBatchId, createdBy);
        return Result.success(Map.of("gradeBatchId", gradeBatchId));
    }

    /**
     * 一键执行: 映射 → 开课 → 任务 (学期初始化)
     */
    @PostMapping("/initialize-semester")
    @CasbinAccess(resource = "teaching:workflow", action = "edit")
    public Result<Map<String, Object>> initializeSemester(@RequestBody Map<String, Object> data) {
        Long semesterId = toLong(data.get("semesterId"));
        Long createdBy = toLong(data.get("userId"));

        int mappings = workflowService.generateCohortSemesterMappings(semesterId);
        int offerings = workflowService.generateOfferingsFromMappings(semesterId, createdBy);
        int tasks = workflowService.generateTasksFromOfferings(semesterId, createdBy);

        return Result.success(Map.of(
            "mappings", mappings,
            "offerings", offerings,
            "tasks", tasks
        ));
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }
}
