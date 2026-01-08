package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.dto.DailyCheckCreateRequest;
import com.school.management.dto.DailyCheckResponse;
import com.school.management.dto.DailyScoringInitResponse;
import com.school.management.dto.DailyScoringRequest;
import com.school.management.service.DailyCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 日常检查控制器
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/quantification/daily-checks")
@RequiredArgsConstructor
public class DailyCheckController {

    private final DailyCheckService dailyCheckService;

    /**
     * 创建日常检查
     */
    @PostMapping
    @PreAuthorize("hasAuthority('quantification:check:add')")
    public Result<Long> createDailyCheck(@Valid @RequestBody DailyCheckCreateRequest request) {
        Long id = dailyCheckService.createDailyCheck(request);
        return Result.success(id);
    }

    /**
     * 更新日常检查
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('quantification:check:edit')")
    public Result<Void> updateDailyCheck(@PathVariable Long id, @Valid @RequestBody DailyCheckCreateRequest request) {
        dailyCheckService.updateDailyCheck(id, request);
        return Result.success();
    }

    /**
     * 删除日常检查
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('quantification:check:delete')")
    public Result<Void> deleteDailyCheck(@PathVariable Long id) {
        dailyCheckService.deleteDailyCheck(id);
        return Result.success();
    }

    /**
     * 获取检查详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('quantification:check:view')")
    public Result<DailyCheckResponse> getDailyCheck(@PathVariable Long id) {
        DailyCheckResponse check = dailyCheckService.getDailyCheckById(id);
        return Result.success(check);
    }

    /**
     * 分页查询检查
     */
    @GetMapping
    @PreAuthorize("hasAuthority('quantification:check:view')")
    public Result<IPage<DailyCheckResponse>> getDailyCheckPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkDate,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String checkName,
            @RequestParam(required = false) Long planId) {
        IPage<DailyCheckResponse> page = dailyCheckService.getDailyCheckPage(
                pageNum, pageSize, checkDate, status, checkName, planId);
        return Result.success(page);
    }

    /**
     * 更新检查状态
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('quantification:check:edit')")
    public Result<Void> updateCheckStatus(@PathVariable Long id, @RequestParam Integer status) {
        dailyCheckService.updateCheckStatus(id, status);
        return Result.success();
    }

    /**
     * 获取打分初始化数据
     */
    @GetMapping("/{checkId}/scoring/init")
    @PreAuthorize("hasAuthority('quantification:check:score')")
    public Result<DailyScoringInitResponse> getScoringInitData(@PathVariable Long checkId) {
        DailyScoringInitResponse response = dailyCheckService.getScoringInitData(checkId);
        return Result.success(response);
    }

    /**
     * 保存打分数据
     */
    @PostMapping("/{checkId}/scoring")
    @PreAuthorize("hasAuthority('quantification:check:score')")
    public Result<Void> saveScoring(@PathVariable Long checkId, @Valid @RequestBody DailyScoringRequest request) {
        dailyCheckService.saveScoring(checkId, request);
        return Result.success();
    }
}
