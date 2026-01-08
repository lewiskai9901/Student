package com.school.management.controller.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.entity.evaluation.StudentEvaluationResult;
import com.school.management.dto.StudentResponse;
import com.school.management.exception.BusinessException;
import com.school.management.service.StudentService;
import com.school.management.service.evaluation.EvaluationCalculationService;
import com.school.management.service.evaluation.EvaluationResultService;
import com.school.management.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 综测结果管理控制器
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/results")
@RequiredArgsConstructor
@Tag(name = "综测结果管理", description = "综测结果查询与计算相关接口")
public class EvaluationResultController {

    private final EvaluationResultService resultService;
    private final EvaluationCalculationService calculationService;
    private final StudentService studentService;

    /**
     * 分页查询综测结果
     */
    @GetMapping
    @Operation(summary = "分页查询综测结果")
    @PreAuthorize("hasAuthority('evaluation:result:list')")
    public Result<PageResult<Map<String, Object>>> pageResults(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "周期ID") @RequestParam(required = false) Long periodId,
            @Parameter(description = "学生姓名") @RequestParam(required = false) String studentName,
            @Parameter(description = "学号") @RequestParam(required = false) String studentNo,
            @Parameter(description = "班级ID") @RequestParam(required = false) Long classId,
            @Parameter(description = "院系ID") @RequestParam(required = false) Long departmentId) {

        log.info("分页查询综测结果: pageNum={}, pageSize={}, periodId={}", pageNum, pageSize, periodId);

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        Map<String, Object> query = new HashMap<>();
        query.put("periodId", periodId);
        query.put("studentName", studentName);
        query.put("studentNo", studentNo);
        query.put("classId", classId);
        query.put("departmentId", departmentId);

        Page<Map<String, Object>> result = resultService.pageResults(page, query);
        return Result.success(PageResult.from(result));
    }

    /**
     * 获取综测结果详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取综测结果详情")
    @PreAuthorize("hasAuthority('evaluation:result:list')")
    public Result<Map<String, Object>> getResultDetail(
            @Parameter(description = "结果ID") @PathVariable Long id) {
        log.info("获取综测结果详情: id={}", id);
        Map<String, Object> detail = resultService.getResultDetail(id);
        return Result.success(detail);
    }

    /**
     * 获取学生综测结果
     */
    @GetMapping("/student")
    @Operation(summary = "获取学生综测结果")
    @PreAuthorize("hasAuthority('evaluation:result:list')")
    public Result<Map<String, Object>> getStudentResult(
            @Parameter(description = "学生ID") @RequestParam Long studentId,
            @Parameter(description = "周期ID") @RequestParam Long periodId) {
        log.info("获取学生综测结果: studentId={}, periodId={}", studentId, periodId);
        Map<String, Object> result = resultService.getStudentResult(studentId, periodId);
        return Result.success(result);
    }

    /**
     * 获取我的综测结果
     */
    @GetMapping("/my")
    @Operation(summary = "获取我的综测结果")
    @PreAuthorize("hasAuthority('evaluation:result:my')")
    public Result<Map<String, Object>> getMyResult(
            @Parameter(description = "周期ID") @RequestParam Long periodId) {
        // 从当前登录用户获取关联的学生ID
        Long userId = SecurityUtils.getCurrentUserId();
        StudentResponse student = studentService.getStudentByUserId(userId);
        if (student == null) {
            throw new BusinessException("当前用户未关联学生信息");
        }
        Long studentId = student.getId();
        log.info("获取我的综测结果: userId={}, studentId={}, periodId={}", userId, studentId, periodId);
        Map<String, Object> result = resultService.getStudentResult(studentId, periodId);
        return Result.success(result);
    }

    /**
     * 计算单个学生综测
     */
    @PostMapping("/calculate/student")
    @Operation(summary = "计算单个学生综测")
    @PreAuthorize("hasAuthority('evaluation:result:calculate')")
    public Result<StudentEvaluationResult> calculateStudent(
            @Parameter(description = "周期ID") @RequestParam Long periodId,
            @Parameter(description = "学生ID") @RequestParam Long studentId) {
        log.info("计算学生综测: periodId={}, studentId={}", periodId, studentId);
        StudentEvaluationResult result = calculationService.calculateStudent(periodId, studentId);
        return Result.success(result);
    }

    /**
     * 计算班级综测
     */
    @PostMapping("/calculate/class")
    @Operation(summary = "计算班级综测")
    @PreAuthorize("hasAuthority('evaluation:result:calculate')")
    public Result<List<StudentEvaluationResult>> calculateClass(
            @Parameter(description = "周期ID") @RequestParam Long periodId,
            @Parameter(description = "班级ID") @RequestParam Long classId) {
        log.info("计算班级综测: periodId={}, classId={}", periodId, classId);
        List<StudentEvaluationResult> results = calculationService.calculateClass(periodId, classId);
        return Result.success(results);
    }

    /**
     * 计算年级综测
     */
    @PostMapping("/calculate/grade")
    @Operation(summary = "计算年级综测")
    @PreAuthorize("hasAuthority('evaluation:result:calculate')")
    public Result<Integer> calculateGrade(
            @Parameter(description = "周期ID") @RequestParam Long periodId,
            @Parameter(description = "年级ID") @RequestParam Long gradeId) {
        log.info("计算年级综测: periodId={}, gradeId={}", periodId, gradeId);
        int count = calculationService.calculateGrade(periodId, gradeId);
        return Result.success(count);
    }

    /**
     * 重新计算
     */
    @PostMapping("/{id}/recalculate")
    @Operation(summary = "重新计算")
    @PreAuthorize("hasAuthority('evaluation:result:calculate')")
    public Result<StudentEvaluationResult> recalculate(
            @Parameter(description = "结果ID") @PathVariable Long id) {
        log.info("重新计算综测: id={}", id);
        StudentEvaluationResult result = calculationService.recalculate(id);
        return Result.success(result);
    }

    /**
     * 计算排名
     */
    @PostMapping("/calculate-rankings")
    @Operation(summary = "计算排名")
    @PreAuthorize("hasAuthority('evaluation:result:calculate')")
    public Result<Void> calculateRankings(
            @Parameter(description = "周期ID") @RequestParam Long periodId) {
        log.info("计算排名: periodId={}", periodId);
        calculationService.calculateRankings(periodId);
        return Result.success();
    }

    /**
     * 获取班级综测排名
     */
    @GetMapping("/ranking/class")
    @Operation(summary = "获取班级综测排名")
    @PreAuthorize("hasAuthority('evaluation:result:list')")
    public Result<List<Map<String, Object>>> getClassRanking(
            @Parameter(description = "周期ID") @RequestParam Long periodId,
            @Parameter(description = "班级ID") @RequestParam Long classId) {
        log.info("获取班级综测排名: periodId={}, classId={}", periodId, classId);
        List<Map<String, Object>> ranking = resultService.getClassRanking(periodId, classId);
        return Result.success(ranking);
    }

    /**
     * 获取年级综测排名
     */
    @GetMapping("/ranking/grade")
    @Operation(summary = "获取年级综测排名")
    @PreAuthorize("hasAuthority('evaluation:result:list')")
    public Result<List<Map<String, Object>>> getGradeRanking(
            @Parameter(description = "周期ID") @RequestParam Long periodId,
            @Parameter(description = "年级ID") @RequestParam Long gradeId,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "100") Integer limit) {
        log.info("获取年级综测排名: periodId={}, gradeId={}", periodId, gradeId);
        List<Map<String, Object>> ranking = resultService.getGradeRanking(periodId, gradeId, limit);
        return Result.success(ranking);
    }

    /**
     * 获取综测统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取综测统计")
    @PreAuthorize("hasAuthority('evaluation:result:list')")
    public Result<Map<String, Object>> getStatistics(
            @Parameter(description = "周期ID") @RequestParam Long periodId,
            @Parameter(description = "班级ID") @RequestParam(required = false) Long classId,
            @Parameter(description = "年级ID") @RequestParam(required = false) Long gradeId) {
        log.info("获取综测统计: periodId={}", periodId);
        Map<String, Object> statistics = resultService.getStatistics(periodId, classId, gradeId);
        return Result.success(statistics);
    }

    /**
     * 导出综测结果
     */
    @GetMapping("/export")
    @Operation(summary = "导出综测结果")
    @PreAuthorize("hasAuthority('evaluation:result:export')")
    public Result<List<Map<String, Object>>> exportResults(
            @Parameter(description = "周期ID") @RequestParam Long periodId,
            @Parameter(description = "班级ID") @RequestParam(required = false) Long classId) {
        log.info("导出综测结果: periodId={}, classId={}", periodId, classId);
        List<Map<String, Object>> data = resultService.exportResults(periodId, classId);
        return Result.success(data);
    }

    /**
     * 获取维度分数明细
     */
    @GetMapping("/{id}/dimension/{dimension}")
    @Operation(summary = "获取维度分数明细")
    @PreAuthorize("hasAuthority('evaluation:result:list')")
    public Result<Map<String, Object>> getDimensionDetail(
            @Parameter(description = "结果ID") @PathVariable Long id,
            @Parameter(description = "维度") @PathVariable String dimension) {
        log.info("获取维度分数明细: id={}, dimension={}", id, dimension);
        Map<String, Object> detail = resultService.getDimensionDetail(id, dimension);
        return Result.success(detail);
    }

    /**
     * 同步量化数据
     */
    @PostMapping("/sync-quantification")
    @Operation(summary = "同步量化数据到综测")
    @PreAuthorize("hasAuthority('evaluation:result:sync')")
    public Result<Integer> syncQuantificationData(
            @Parameter(description = "周期ID") @RequestParam Long periodId) {
        log.info("同步量化数据: periodId={}", periodId);
        int count = calculationService.syncQuantificationData(periodId);
        return Result.success(count);
    }

    /**
     * 同步荣誉数据
     */
    @PostMapping("/sync-honor")
    @Operation(summary = "同步荣誉数据到综测")
    @PreAuthorize("hasAuthority('evaluation:result:sync')")
    public Result<Integer> syncHonorData(
            @Parameter(description = "周期ID") @RequestParam Long periodId) {
        log.info("同步荣誉数据: periodId={}", periodId);
        int count = calculationService.syncHonorData(periodId);
        return Result.success(count);
    }
}
