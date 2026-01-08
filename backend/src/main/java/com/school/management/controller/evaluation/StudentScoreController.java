package com.school.management.controller.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.entity.evaluation.StudentScore;
import com.school.management.service.evaluation.StudentScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生成绩管理控制器
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/scores")
@RequiredArgsConstructor
@Tag(name = "学生成绩管理", description = "学生成绩管理相关接口")
public class StudentScoreController {

    private final StudentScoreService scoreService;

    /**
     * 分页查询成绩
     */
    @GetMapping
    @Operation(summary = "分页查询成绩")
    @PreAuthorize("hasAuthority('evaluation:score:list')")
    public Result<PageResult<Map<String, Object>>> pageScores(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "学生姓名") @RequestParam(required = false) String studentName,
            @Parameter(description = "学号") @RequestParam(required = false) String studentNo,
            @Parameter(description = "课程ID") @RequestParam(required = false) Long courseId,
            @Parameter(description = "学期ID") @RequestParam(required = false) Long semesterId,
            @Parameter(description = "班级ID") @RequestParam(required = false) Long classId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {

        log.info("分页查询成绩: pageNum={}, pageSize={}", pageNum, pageSize);

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        Map<String, Object> query = new HashMap<>();
        query.put("studentName", studentName);
        query.put("studentNo", studentNo);
        query.put("courseId", courseId);
        query.put("semesterId", semesterId);
        query.put("classId", classId);
        query.put("status", status);

        Page<Map<String, Object>> result = scoreService.pageScores(page, query);
        return Result.success(PageResult.from(result));
    }

    /**
     * 获取成绩详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取成绩详情")
    @PreAuthorize("hasAuthority('evaluation:score:list')")
    public Result<Map<String, Object>> getScoreDetail(
            @Parameter(description = "成绩ID") @PathVariable Long id) {
        log.info("获取成绩详情: id={}", id);
        Map<String, Object> detail = scoreService.getScoreDetail(id);
        return Result.success(detail);
    }

    /**
     * 录入成绩
     */
    @PostMapping
    @Operation(summary = "录入成绩")
    @PreAuthorize("hasAuthority('evaluation:score:input')")
    public Result<Long> inputScore(@RequestBody StudentScore score) {
        log.info("录入成绩: studentId={}, courseId={}", score.getStudentId(), score.getCourseId());
        Long id = scoreService.inputScore(score);
        return Result.success(id);
    }

    /**
     * 批量录入成绩
     */
    @PostMapping("/batch")
    @Operation(summary = "批量录入成绩")
    @PreAuthorize("hasAuthority('evaluation:score:input')")
    public Result<Integer> batchInputScores(@RequestBody List<StudentScore> scores) {
        log.info("批量录入成绩: count={}", scores.size());
        int count = scoreService.batchInputScores(scores);
        return Result.success(count);
    }

    /**
     * 更新成绩
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新成绩")
    @PreAuthorize("hasAuthority('evaluation:score:update')")
    public Result<Void> updateScore(
            @Parameter(description = "成绩ID") @PathVariable Long id,
            @RequestBody StudentScore score) {
        log.info("更新成绩: id={}", id);
        score.setId(id);
        scoreService.updateScore(score);
        return Result.success();
    }

    /**
     * 删除成绩
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除成绩")
    @PreAuthorize("hasAuthority('evaluation:score:delete')")
    public Result<Void> deleteScore(
            @Parameter(description = "成绩ID") @PathVariable Long id) {
        log.info("删除成绩: id={}", id);
        scoreService.deleteScore(id);
        return Result.success();
    }

    /**
     * 获取学生学期成绩
     */
    @GetMapping("/student/{studentId}")
    @Operation(summary = "获取学生学期成绩")
    @PreAuthorize("hasAuthority('evaluation:score:list')")
    public Result<List<Map<String, Object>>> getStudentScores(
            @Parameter(description = "学生ID") @PathVariable Long studentId,
            @Parameter(description = "学期ID") @RequestParam Long semesterId) {
        log.info("获取学生学期成绩: studentId={}, semesterId={}", studentId, semesterId);
        List<Map<String, Object>> list = scoreService.getStudentScores(studentId, semesterId);
        return Result.success(list);
    }

    /**
     * 获取我的成绩
     */
    @GetMapping("/my")
    @Operation(summary = "获取我的成绩")
    @PreAuthorize("hasAuthority('evaluation:score:my')")
    public Result<List<Map<String, Object>>> getMyScores(
            @Parameter(description = "学期ID") @RequestParam(required = false) Long semesterId) {
        // 从SecurityContext获取当前认证用户ID
        Long studentId = com.school.management.util.SecurityUtils.getCurrentUserId();
        if (studentId == null) {
            log.warn("未获取到当前用户ID，请确保已登录");
            return Result.error("未认证用户");
        }
        log.info("获取我的成绩: studentId={}", studentId);
        List<Map<String, Object>> list = scoreService.getStudentScores(studentId, semesterId);
        return Result.success(list);
    }

    /**
     * 计算学生GPA
     */
    @GetMapping("/student/{studentId}/gpa")
    @Operation(summary = "计算学生GPA")
    @PreAuthorize("hasAuthority('evaluation:score:list')")
    public Result<Map<String, Object>> calculateGPA(
            @Parameter(description = "学生ID") @PathVariable Long studentId,
            @Parameter(description = "学期ID") @RequestParam Long semesterId) {
        log.info("计算学生GPA: studentId={}, semesterId={}", studentId, semesterId);

        BigDecimal gpa = scoreService.calculateSemesterGPA(studentId, semesterId);
        BigDecimal average = scoreService.calculateSemesterAverage(studentId, semesterId);
        BigDecimal weightedAverage = scoreService.calculateWeightedAverage(studentId, semesterId);

        Map<String, Object> result = new HashMap<>();
        result.put("gpa", gpa);
        result.put("average", average);
        result.put("weightedAverage", weightedAverage);

        return Result.success(result);
    }

    /**
     * 获取班级成绩统计
     */
    @GetMapping("/statistics/class/{classId}")
    @Operation(summary = "获取班级成绩统计")
    @PreAuthorize("hasAuthority('evaluation:score:list')")
    public Result<Map<String, Object>> getClassStatistics(
            @Parameter(description = "班级ID") @PathVariable Long classId,
            @Parameter(description = "课程ID") @RequestParam(required = false) Long courseId,
            @Parameter(description = "学期ID") @RequestParam Long semesterId) {
        log.info("获取班级成绩统计: classId={}, semesterId={}", classId, semesterId);
        Map<String, Object> statistics = scoreService.getClassScoreStatistics(classId, courseId, semesterId);
        return Result.success(statistics);
    }

    /**
     * 获取课程成绩排名
     */
    @GetMapping("/ranking/course/{courseId}")
    @Operation(summary = "获取课程成绩排名")
    @PreAuthorize("hasAuthority('evaluation:score:list')")
    public Result<List<Map<String, Object>>> getCourseRanking(
            @Parameter(description = "课程ID") @PathVariable Long courseId,
            @Parameter(description = "学期ID") @RequestParam Long semesterId,
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取课程成绩排名: courseId={}, semesterId={}", courseId, semesterId);
        List<Map<String, Object>> ranking = scoreService.getCourseScoreRanking(courseId, semesterId, limit);
        return Result.success(ranking);
    }

    /**
     * 导入成绩数据
     */
    @PostMapping("/import")
    @Operation(summary = "导入成绩数据")
    @PreAuthorize("hasAuthority('evaluation:score:import')")
    public Result<Integer> importScores(@RequestBody List<StudentScore> scores) {
        log.info("导入成绩数据: count={}", scores.size());
        int count = scoreService.importScores(scores);
        return Result.success(count);
    }

    /**
     * 导出班级成绩
     */
    @GetMapping("/export")
    @Operation(summary = "导出班级成绩")
    @PreAuthorize("hasAuthority('evaluation:score:export')")
    public Result<List<Map<String, Object>>> exportClassScores(
            @Parameter(description = "班级ID") @RequestParam Long classId,
            @Parameter(description = "学期ID") @RequestParam Long semesterId) {
        log.info("导出班级成绩: classId={}, semesterId={}", classId, semesterId);
        List<Map<String, Object>> data = scoreService.exportClassScores(classId, semesterId);
        return Result.success(data);
    }

    /**
     * 锁定成绩
     */
    @PostMapping("/lock")
    @Operation(summary = "锁定成绩")
    @PreAuthorize("hasAuthority('evaluation:score:lock')")
    public Result<Void> lockScores(
            @Parameter(description = "学期ID") @RequestParam Long semesterId,
            @Parameter(description = "课程ID") @RequestParam(required = false) Long courseId) {
        log.info("锁定成绩: semesterId={}, courseId={}", semesterId, courseId);
        scoreService.lockScores(semesterId, courseId);
        return Result.success();
    }

    /**
     * 解锁成绩
     */
    @PostMapping("/unlock")
    @Operation(summary = "解锁成绩")
    @PreAuthorize("hasAuthority('evaluation:score:lock')")
    public Result<Void> unlockScores(
            @Parameter(description = "学期ID") @RequestParam Long semesterId,
            @Parameter(description = "课程ID") @RequestParam(required = false) Long courseId) {
        log.info("解锁成绩: semesterId={}, courseId={}", semesterId, courseId);
        scoreService.unlockScores(semesterId, courseId);
        return Result.success();
    }
}
