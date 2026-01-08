package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.service.DailyCheckConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 检查记录转换Controller (临时工具接口)
 *
 * @author system
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/quantification/conversion")
@RequiredArgsConstructor
@Tag(name = "检查记录转换", description = "日常检查转检查记录工具")
public class InspectionConversionController {

    private final DailyCheckConversionService conversionService;

    @PostMapping("/convert-one/{dailyCheckId}")
    @Operation(summary = "转换单条日常检查")
    @PreAuthorize("hasAuthority('quantification:check:add')")
    public Result<Long> convertOne(@PathVariable Long dailyCheckId) {
        try {
            Long sessionId = conversionService.convertDailyCheckToInspectionSession(dailyCheckId);
            return Result.success("转换成功", sessionId);
        } catch (Exception e) {
            log.error("转换失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/convert-all")
    @Operation(summary = "批量转换所有已结束的日常检查")
    @PreAuthorize("hasAuthority('quantification:check:add')")
    public Result<Integer> convertAll() {
        try {
            Integer count = conversionService.convertAllCompletedDailyChecks();
            return Result.success("成功转换 " + count + " 条记录", count);
        } catch (Exception e) {
            log.error("批量转换失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/check-converted/{dailyCheckId}")
    @Operation(summary = "检查是否已转换")
    @PreAuthorize("hasAuthority('quantification:check:list')")
    public Result<Boolean> checkConverted(@PathVariable Long dailyCheckId) {
        boolean converted = conversionService.isAlreadyConverted(dailyCheckId);
        return Result.success(converted);
    }
}
