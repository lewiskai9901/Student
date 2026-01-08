package com.school.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.Result;
import com.school.management.dto.InspectionSessionQueryRequest;
import com.school.management.dto.InspectionSessionRequest;
import com.school.management.dto.InspectionSessionResponse;
// import com.school.management.security.util.UserContext;
import com.school.management.service.InspectionSessionService;
import com.school.management.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 检查批次Controller
 *
 * @author system
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/quantification/inspections")
@RequiredArgsConstructor
@Tag(name = "量化检查管理V2", description = "新版量化检查批次管理")
public class InspectionSessionController {

    private final InspectionSessionService inspectionSessionService;

    @PostMapping
    @Operation(summary = "创建检查批次")
    @PreAuthorize("hasAuthority('quantification:check:add')")
    public Result<Long> createInspectionSession(@Validated @RequestBody InspectionSessionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentUsername();
        Long sessionId = inspectionSessionService.createInspectionSession(request, userId, userName);
        return Result.success("创建成功", sessionId);
    }

    @GetMapping
    @Operation(summary = "查询检查批次列表")
    @PreAuthorize("hasAuthority('quantification:check:list')")
    public Result<Page<InspectionSessionResponse>> listInspectionSessions(InspectionSessionQueryRequest request) {
        Page<InspectionSessionResponse> page = inspectionSessionService.listInspectionSessions(request);
        return Result.success(page);
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "查询检查批次详情")
    @PreAuthorize("hasAuthority('quantification:check:detail')")
    public Result<InspectionSessionResponse> getInspectionSessionDetail(@PathVariable Long sessionId) {
        InspectionSessionResponse response = inspectionSessionService.getInspectionSessionDetail(sessionId);
        return Result.success(response);
    }

    @PutMapping("/{sessionId}")
    @Operation(summary = "更新检查批次")
    @PreAuthorize("hasAuthority('quantification:check:edit')")
    public Result<Void> updateInspectionSession(@PathVariable Long sessionId,
                                                 @Validated @RequestBody InspectionSessionRequest request) {
        inspectionSessionService.updateInspectionSession(sessionId, request);
        return Result.success();
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "删除检查批次")
    @PreAuthorize("hasAuthority('quantification:check:delete')")
    public Result<Void> deleteInspectionSession(@PathVariable Long sessionId) {
        inspectionSessionService.deleteInspectionSession(sessionId);
        return Result.success();
    }

    @PostMapping("/{sessionId}/submit")
    @Operation(summary = "提交审核")
    @PreAuthorize("hasAuthority('quantification:check:submit')")
    public Result<Void> submitForReview(@PathVariable Long sessionId) {
        inspectionSessionService.submitForReview(sessionId);
        return Result.success();
    }

    @PostMapping("/{sessionId}/approve")
    @Operation(summary = "审核通过")
    @PreAuthorize("hasAuthority('quantification:check:review')")
    public Result<Void> approveInspectionSession(@PathVariable Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentUsername();
        inspectionSessionService.approveInspectionSession(sessionId, userId, userName);
        return Result.success();
    }

    @PostMapping("/{sessionId}/publish")
    @Operation(summary = "发布检查结果")
    @PreAuthorize("hasAuthority('quantification:check:publish')")
    public Result<Void> publishInspectionSession(@PathVariable Long sessionId) {
        inspectionSessionService.publishInspectionSession(sessionId);
        return Result.success();
    }
}
