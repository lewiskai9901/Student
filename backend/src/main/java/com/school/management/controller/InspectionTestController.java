package com.school.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.Result;
import com.school.management.dto.InspectionSessionQueryRequest;
import com.school.management.dto.InspectionSessionResponse;
import com.school.management.service.DailyCheckConversionService;
import com.school.management.service.InspectionSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * 检查记录测试Controller (仅开发环境可用)
 *
 * @author system
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/test/inspections")
@RequiredArgsConstructor
@Tag(name = "检查记录测试", description = "开发环境测试接口(仅dev环境可用)")
@Profile("dev")
public class InspectionTestController {

    private final InspectionSessionService inspectionSessionService;
    private final DailyCheckConversionService conversionService;

    @GetMapping("/list")
    @Operation(summary = "查询检查批次列表(无权限)")
    public Result<Page<InspectionSessionResponse>> listInspectionSessions(InspectionSessionQueryRequest request) {
        log.info("测试接口:查询检查批次列表, pageNum={}, pageSize={}", request.getPageNum(), request.getPageSize());
        Page<InspectionSessionResponse> page = inspectionSessionService.listInspectionSessions(request);
        log.info("查询结果: total={}, records={}", page.getTotal(), page.getRecords().size());
        return Result.success(page);
    }

    @GetMapping("/detail/{sessionId}")
    @Operation(summary = "查询检查批次详情(无权限)")
    public Result<InspectionSessionResponse> getDetail(@PathVariable Long sessionId) {
        log.info("测试接口:查询检查批次详情, sessionId={}", sessionId);
        InspectionSessionResponse response = inspectionSessionService.getInspectionSessionDetail(sessionId);
        return Result.success(response);
    }

    @PostMapping("/convert-all")
    @Operation(summary = "批量转换(无权限)")
    public Result<Integer> convertAll() {
        log.info("测试接口:批量转换所有已结束的检查");
        try {
            Integer count = conversionService.convertAllCompletedDailyChecks();
            log.info("转换完成,成功转换 {} 条记录", count);
            return Result.success("成功转换 " + count + " 条记录", count);
        } catch (Exception e) {
            log.error("转换失败", e);
            return Result.error("转换失败: " + e.getMessage());
        }
    }

    @GetMapping("/count")
    @Operation(summary = "统计检查记录数量")
    public Result<Long> countInspections() {
        InspectionSessionQueryRequest request = new InspectionSessionQueryRequest();
        request.setPageNum(1);
        request.setPageSize(1);
        Page<InspectionSessionResponse> page = inspectionSessionService.listInspectionSessions(request);
        return Result.success("当前共有 " + page.getTotal() + " 条检查记录", page.getTotal());
    }
}
