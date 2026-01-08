package com.school.management.controller.evaluation;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.entity.evaluation.EvaluationPeriod;
import com.school.management.service.evaluation.EvaluationPeriodService;
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
 * 综测评定周期管理控制器
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@RestController
@RequestMapping("/evaluation/periods")
@RequiredArgsConstructor
@Tag(name = "综测评定周期管理", description = "综测评定周期相关接口")
public class EvaluationPeriodController {

    private final EvaluationPeriodService periodService;

    /**
     * 分页查询综测周期
     */
    @GetMapping
    @Operation(summary = "分页查询综测周期")
    @PreAuthorize("hasAuthority('evaluation:period:list')")
    public Result<PageResult<Map<String, Object>>> pagePeriods(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "周期编码") @RequestParam(required = false) String periodCode,
            @Parameter(description = "周期名称") @RequestParam(required = false) String periodName,
            @Parameter(description = "学年") @RequestParam(required = false) String academicYear,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {

        log.info("分页查询综测周期: pageNum={}, pageSize={}", pageNum, pageSize);

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        Map<String, Object> query = new HashMap<>();
        query.put("periodCode", periodCode);
        query.put("periodName", periodName);
        query.put("academicYear", academicYear);
        query.put("status", status);

        Page<Map<String, Object>> result = periodService.pagePeriods(page, query);
        return Result.success(PageResult.from(result));
    }

    /**
     * 获取当前进行中的周期
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前进行中的周期")
    @PreAuthorize("hasAuthority('evaluation:period:list')")
    public Result<EvaluationPeriod> getCurrentPeriod() {
        log.info("获取当前进行中的周期");
        EvaluationPeriod period = periodService.getCurrentPeriod();
        return Result.success(period);
    }

    /**
     * 获取周期详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取周期详情")
    @PreAuthorize("hasAuthority('evaluation:period:list')")
    public Result<Map<String, Object>> getPeriodDetail(
            @Parameter(description = "周期ID") @PathVariable Long id) {
        log.info("获取周期详情: id={}", id);
        Map<String, Object> detail = periodService.getPeriodDetail(id);
        return Result.success(detail);
    }

    /**
     * 创建综测周期
     */
    @PostMapping
    @Operation(summary = "创建综测周期")
    @PreAuthorize("hasAuthority('evaluation:period:create')")
    public Result<Long> createPeriod(@RequestBody EvaluationPeriod period) {
        log.info("创建综测周期: code={}, name={}", period.getPeriodCode(), period.getPeriodName());
        Long id = periodService.createPeriod(period);
        return Result.success(id);
    }

    /**
     * 更新综测周期
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新综测周期")
    @PreAuthorize("hasAuthority('evaluation:period:update')")
    public Result<Void> updatePeriod(
            @Parameter(description = "周期ID") @PathVariable Long id,
            @RequestBody EvaluationPeriod period) {
        log.info("更新综测周期: id={}", id);
        period.setId(id);
        periodService.updatePeriod(period);
        return Result.success();
    }

    /**
     * 删除综测周期
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除综测周期")
    @PreAuthorize("hasAuthority('evaluation:period:delete')")
    public Result<Void> deletePeriod(
            @Parameter(description = "周期ID") @PathVariable Long id) {
        log.info("删除综测周期: id={}", id);
        periodService.deletePeriod(id);
        return Result.success();
    }

    /**
     * 开始数据采集
     */
    @PostMapping("/{id}/start-data-collection")
    @Operation(summary = "开始数据采集")
    @PreAuthorize("hasAuthority('evaluation:period:manage')")
    public Result<Void> startDataCollection(
            @Parameter(description = "周期ID") @PathVariable Long id) {
        log.info("开始数据采集: id={}", id);
        periodService.startDataCollection(id);
        return Result.success();
    }

    /**
     * 开始荣誉申报
     */
    @PostMapping("/{id}/start-application")
    @Operation(summary = "开始荣誉申报")
    @PreAuthorize("hasAuthority('evaluation:period:manage')")
    public Result<Void> startApplication(
            @Parameter(description = "周期ID") @PathVariable Long id) {
        log.info("开始荣誉申报: id={}", id);
        periodService.startApplication(id);
        return Result.success();
    }

    /**
     * 开始审核
     */
    @PostMapping("/{id}/start-review")
    @Operation(summary = "开始审核")
    @PreAuthorize("hasAuthority('evaluation:period:manage')")
    public Result<Void> startReview(
            @Parameter(description = "周期ID") @PathVariable Long id) {
        log.info("开始审核: id={}", id);
        periodService.startReview(id);
        return Result.success();
    }

    /**
     * 开始公示
     */
    @PostMapping("/{id}/start-publicity")
    @Operation(summary = "开始公示")
    @PreAuthorize("hasAuthority('evaluation:period:manage')")
    public Result<Void> startPublicity(
            @Parameter(description = "周期ID") @PathVariable Long id) {
        log.info("开始公示: id={}", id);
        periodService.startPublicity(id);
        return Result.success();
    }

    /**
     * 结束周期
     */
    @PostMapping("/{id}/finish")
    @Operation(summary = "结束周期")
    @PreAuthorize("hasAuthority('evaluation:period:manage')")
    public Result<Void> finishPeriod(
            @Parameter(description = "周期ID") @PathVariable Long id) {
        log.info("结束周期: id={}", id);
        periodService.finishPeriod(id);
        return Result.success();
    }

    /**
     * 锁定周期
     */
    @PostMapping("/{id}/lock")
    @Operation(summary = "锁定周期")
    @PreAuthorize("hasAuthority('evaluation:period:lock')")
    public Result<Void> lockPeriod(
            @Parameter(description = "周期ID") @PathVariable Long id) {
        log.info("锁定周期: id={}", id);
        periodService.lockPeriod(id);
        return Result.success();
    }

    /**
     * 解锁周期
     */
    @PostMapping("/{id}/unlock")
    @Operation(summary = "解锁周期")
    @PreAuthorize("hasAuthority('evaluation:period:lock')")
    public Result<Void> unlockPeriod(
            @Parameter(description = "周期ID") @PathVariable Long id) {
        log.info("解锁周期: id={}", id);
        periodService.unlockPeriod(id);
        return Result.success();
    }

    /**
     * 根据学年获取周期列表
     */
    @GetMapping("/academic-year/{academicYear}")
    @Operation(summary = "根据学年获取周期列表")
    @PreAuthorize("hasAuthority('evaluation:period:list')")
    public Result<List<EvaluationPeriod>> getByAcademicYear(
            @Parameter(description = "学年") @PathVariable String academicYear) {
        log.info("根据学年获取周期列表: academicYear={}", academicYear);
        List<EvaluationPeriod> list = periodService.getByAcademicYear(academicYear);
        return Result.success(list);
    }
}
