package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.GradeApplicationService;
import com.school.management.common.audit.Audited;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.persistence.teaching.grade.GradeBatchPO;
import com.school.management.infrastructure.persistence.teaching.grade.StudentGradePO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 成绩管理 REST Controller (DDD-lite)
 * 处理成绩批次、成绩录入、成绩查询、统计与排名
 */
@RestController("teachingGradeController")
@RequestMapping("/teaching/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeApplicationService gradeService;

    // ==================== 成绩批次管理 ====================

    @GetMapping("/batches")
    @CasbinAccess(resource = "teaching:grade", action = "view")
    public Result<Map<String, Object>> listBatches(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer gradeType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(gradeService.listBatches(semesterId, gradeType, status, pageNum, pageSize));
    }

    @GetMapping("/batches/{id}")
    @CasbinAccess(resource = "teaching:grade", action = "view")
    public Result<GradeBatchPO> getBatch(@PathVariable Long id) {
        return Result.success(gradeService.getBatch(id));
    }

    @PostMapping("/batches")
    @CasbinAccess(resource = "teaching:grade", action = "edit")
    public Result<GradeBatchPO> createBatch(@RequestBody Map<String, Object> data) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(gradeService.createBatch(data, userId));
    }

    @PutMapping("/batches/{id}")
    @CasbinAccess(resource = "teaching:grade", action = "edit")
    public Result<Void> updateBatch(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        gradeService.updateBatch(id, data);
        return Result.success();
    }

    @DeleteMapping("/batches/{id}")
    @CasbinAccess(resource = "teaching:grade", action = "edit")
    public Result<Void> deleteBatch(@PathVariable Long id) {
        gradeService.deleteBatch(id);
        return Result.success();
    }

    @PostMapping("/batches/{id}/submit")
    @CasbinAccess(resource = "teaching:grade", action = "edit")
    public Result<Void> submitBatch(@PathVariable Long id) {
        gradeService.submitBatch(id);
        return Result.success();
    }

    @PostMapping("/batches/{id}/approve")
    @CasbinAccess(resource = "teaching:grade", action = "edit")
    public Result<Void> approveBatch(@PathVariable Long id) {
        gradeService.approveBatch(id);
        return Result.success();
    }

    @PostMapping("/batches/{id}/publish")
    @CasbinAccess(resource = "teaching:grade", action = "edit")
    @Audited(module = "teaching", action = "PUBLISH", resourceType = "GradeBatch", description = "发布成绩批次")
    public Result<Void> publishBatch(@PathVariable Long id) {
        gradeService.publishBatch(id);
        return Result.success();
    }

    // ==================== 成绩录入与查询 ====================

    @GetMapping("/batches/{batchId}/grades")
    @CasbinAccess(resource = "teaching:grade", action = "view")
    public Result<List<Map<String, Object>>> listGradesByBatch(@PathVariable Long batchId) {
        return Result.success(gradeService.listGrades(batchId));
    }

    @PostMapping("/batches/{batchId}/grades")
    @CasbinAccess(resource = "teaching:grade", action = "edit")
    public Result<StudentGradePO> recordGrade(
            @PathVariable Long batchId,
            @RequestBody Map<String, Object> data) {
        return Result.success(gradeService.recordGrade(batchId, data));
    }

    @PutMapping("/{gradeId}")
    @CasbinAccess(resource = "teaching:grade", action = "edit")
    public Result<Void> updateGrade(@PathVariable Long gradeId, @RequestBody Map<String, Object> data) {
        gradeService.updateGrade(gradeId, data);
        return Result.success();
    }

    @PostMapping("/batches/{batchId}/batch-record")
    @CasbinAccess(resource = "teaching:grade", action = "edit")
    public Result<Void> batchRecordGrades(
            @PathVariable Long batchId,
            @RequestBody Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> grades = (List<Map<String, Object>>) data.get("grades");
        gradeService.batchRecordGrades(batchId, grades);
        return Result.success();
    }

    // ==================== 按学生/班级查询 ====================

    @GetMapping("/by-student/{studentId}")
    @CasbinAccess(resource = "teaching:grade", action = "view")
    public Result<List<Map<String, Object>>> getGradesByStudent(
            @PathVariable Long studentId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long courseId) {
        return Result.success(gradeService.getStudentGrades(studentId, semesterId, courseId));
    }

    @GetMapping("/by-class/{classId}")
    @CasbinAccess(resource = "teaching:grade", action = "view")
    public Result<List<Map<String, Object>>> getGradesByClass(@PathVariable Long orgUnitId) {
        return Result.success(gradeService.getClassGrades(orgUnitId));
    }

    // ==================== 统计与排名 ====================

    @GetMapping("/statistics")
    @CasbinAccess(resource = "teaching:grade", action = "view")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Long courseId) {
        return Result.success(gradeService.getStatistics(batchId, orgUnitId, courseId));
    }

    @GetMapping("/ranking")
    @CasbinAccess(resource = "teaching:grade", action = "view")
    public Result<List<Map<String, Object>>> getRanking(
            @RequestParam Long orgUnitId,
            @RequestParam(required = false) Long semesterId) {
        return Result.success(gradeService.getRanking(orgUnitId, semesterId));
    }

    // ==================== 导出 ====================

    @GetMapping("/export")
    @CasbinAccess(resource = "teaching:grade", action = "view")
    public void exportGrades(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) Long courseId,
            HttpServletResponse response) throws IOException {
        gradeService.exportGrades(semesterId, orgUnitId, courseId, response);
    }
}
