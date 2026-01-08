package com.school.management.controller.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.entity.evaluation.EvaluationSemester;
import com.school.management.service.evaluation.SemesterService;
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
 * 学期管理控制器
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@RestController("evaluationSemesterController")
@RequestMapping("/evaluation/semesters")
@RequiredArgsConstructor
@Tag(name = "综测-学期管理", description = "综测学期管理相关接口")
public class SemesterController {

    private final SemesterService semesterService;

    /**
     * 分页查询学期
     */
    @GetMapping
    @Operation(summary = "分页查询学期")
    @PreAuthorize("hasAuthority('evaluation:period:list')")
    public Result<PageResult<Map<String, Object>>> pageSemesters(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "学期编码") @RequestParam(required = false) String semesterCode,
            @Parameter(description = "学期名称") @RequestParam(required = false) String semesterName,
            @Parameter(description = "学年") @RequestParam(required = false) String academicYear,
            @Parameter(description = "学期类型") @RequestParam(required = false) Integer semesterType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {

        log.info("分页查询学期: pageNum={}, pageSize={}", pageNum, pageSize);

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        Map<String, Object> query = new HashMap<>();
        query.put("semesterCode", semesterCode);
        query.put("semesterName", semesterName);
        query.put("academicYear", academicYear);
        query.put("semesterType", semesterType);
        query.put("status", status);

        Page<Map<String, Object>> result = semesterService.pageSemesters(page, query);
        return Result.success(PageResult.from(result));
    }

    /**
     * 获取当前学期
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前学期")
    public Result<EvaluationSemester> getCurrentSemester() {
        log.info("获取当前学期");
        EvaluationSemester semester = semesterService.getCurrentSemester();
        return Result.success(semester);
    }

    /**
     * 获取学期详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取学期详情")
    @PreAuthorize("hasAuthority('evaluation:period:list')")
    public Result<Map<String, Object>> getSemesterDetail(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("获取学期详情: id={}", id);
        Map<String, Object> detail = semesterService.getSemesterDetail(id);
        return Result.success(detail);
    }

    /**
     * 创建学期
     */
    @PostMapping
    @Operation(summary = "创建学期")
    @PreAuthorize("hasAuthority('evaluation:period:create')")
    public Result<Long> createSemester(@RequestBody EvaluationSemester semester) {
        log.info("创建学期: code={}, name={}", semester.getSemesterCode(), semester.getSemesterName());
        Long id = semesterService.createSemester(semester);
        return Result.success(id);
    }

    /**
     * 更新学期
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新学期")
    @PreAuthorize("hasAuthority('evaluation:period:update')")
    public Result<Void> updateSemester(
            @Parameter(description = "学期ID") @PathVariable Long id,
            @RequestBody EvaluationSemester semester) {
        log.info("更新学期: id={}", id);
        semester.setId(id);
        semesterService.updateSemester(semester);
        return Result.success();
    }

    /**
     * 删除学期
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除学期")
    @PreAuthorize("hasAuthority('evaluation:period:delete')")
    public Result<Void> deleteSemester(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("删除学期: id={}", id);
        semesterService.deleteSemester(id);
        return Result.success();
    }

    /**
     * 根据学年获取学期列表
     */
    @GetMapping("/academic-year/{academicYear}")
    @Operation(summary = "根据学年获取学期列表")
    public Result<List<EvaluationSemester>> getByAcademicYear(
            @Parameter(description = "学年") @PathVariable String academicYear) {
        log.info("根据学年获取学期列表: academicYear={}", academicYear);
        List<EvaluationSemester> list = semesterService.getByAcademicYear(academicYear);
        return Result.success(list);
    }

    /**
     * 获取所有学年列表
     */
    @GetMapping("/academic-years")
    @Operation(summary = "获取所有学年列表")
    public Result<List<String>> getAllAcademicYears() {
        log.info("获取所有学年列表");
        List<String> years = semesterService.getAllAcademicYears();
        return Result.success(years);
    }

    /**
     * 设置为当前学期
     */
    @PostMapping("/{id}/set-current")
    @Operation(summary = "设置为当前学期")
    @PreAuthorize("hasAuthority('evaluation:period:manage')")
    public Result<Void> setAsCurrent(
            @Parameter(description = "学期ID") @PathVariable Long id) {
        log.info("设置为当前学期: id={}", id);
        semesterService.setAsCurrent(id);
        return Result.success();
    }

    /**
     * 获取所有启用的学期（用于下拉选择）
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有启用的学期列表")
    public Result<List<EvaluationSemester>> listAllSemesters() {
        List<EvaluationSemester> list = semesterService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EvaluationSemester>()
                        .eq(EvaluationSemester::getStatus, 1)
                        .orderByDesc(EvaluationSemester::getAcademicYear)
                        .orderByAsc(EvaluationSemester::getSemesterType)
        );
        return Result.success(list);
    }
}
