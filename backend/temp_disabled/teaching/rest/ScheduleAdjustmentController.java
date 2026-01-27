package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.ScheduleAdjustmentApplicationService;
import com.school.management.application.teaching.command.CreateAdjustmentCommand;
import com.school.management.application.teaching.query.ScheduleAdjustmentDTO;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 调课管理控制器
 */
@Tag(name = "调课管理", description = "调课申请的创建、审批和查询")
@RestController
@RequestMapping("/api/v2/teaching/adjustments")
@RequiredArgsConstructor
public class ScheduleAdjustmentController {

    private final ScheduleAdjustmentApplicationService adjustmentService;

    @Operation(summary = "创建调课申请")
    @PostMapping
    @PreAuthorize("hasAuthority('teaching:adjustment:create')")
    public Result<Long> createAdjustment(
            @Valid @RequestBody CreateAdjustmentRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        CreateAdjustmentCommand command = CreateAdjustmentCommand.builder()
                .entryId(request.getEntryId())
                .adjustType(request.getAdjustType())
                .originalDate(request.getOriginalDate())
                .originalSlot(request.getOriginalSlot())
                .newDate(request.getNewDate())
                .newSlot(request.getNewSlot())
                .newClassroomId(request.getNewClassroomId())
                .substituteTeacherId(request.getSubstituteTeacherId())
                .reason(request.getReason())
                .operatorId(user.getId())
                .build();

        Long id = adjustmentService.createAdjustment(command);
        return Result.success(id);
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('teaching:adjustment:approve')")
    public Result<Void> approve(
            @PathVariable Long id,
            @RequestParam(required = false) String remark,
            @AuthenticationPrincipal CustomUserDetails user) {
        adjustmentService.approve(id, user.getId(), remark);
        return Result.success();
    }

    @Operation(summary = "审批拒绝")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('teaching:adjustment:approve')")
    public Result<Void> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String remark,
            @AuthenticationPrincipal CustomUserDetails user) {
        adjustmentService.reject(id, user.getId(), remark);
        return Result.success();
    }

    @Operation(summary = "取消申请")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('teaching:adjustment:create')")
    public Result<Void> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        adjustmentService.cancel(id, user.getId());
        return Result.success();
    }

    @Operation(summary = "获取调课申请详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:adjustment:view')")
    public Result<ScheduleAdjustmentDTO> getAdjustment(@PathVariable Long id) {
        return Result.success(adjustmentService.getAdjustment(id));
    }

    @Operation(summary = "获取待审批列表")
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('teaching:adjustment:approve')")
    public Result<List<ScheduleAdjustmentDTO>> getPendingList(@RequestParam Long semesterId) {
        return Result.success(adjustmentService.getPendingList(semesterId));
    }

    @Operation(summary = "根据课表条目获取调课记录")
    @GetMapping("/entry/{entryId}")
    @PreAuthorize("hasAuthority('teaching:adjustment:view')")
    public Result<List<ScheduleAdjustmentDTO>> getByEntryId(@PathVariable Long entryId) {
        return Result.success(adjustmentService.getByEntryId(entryId));
    }

    @Operation(summary = "获取我的调课申请")
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('teaching:adjustment:view')")
    public Result<List<ScheduleAdjustmentDTO>> getMyAdjustments(
            @AuthenticationPrincipal CustomUserDetails user) {
        return Result.success(adjustmentService.getByApplicant(user.getId()));
    }

    @Operation(summary = "分页查询调课记录")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('teaching:adjustment:view')")
    public Result<PageResult<ScheduleAdjustmentDTO>> getPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer adjustType,
            @RequestParam(required = false) Integer status) {

        List<ScheduleAdjustmentDTO> list = adjustmentService.getPage(page, size, semesterId, adjustType, status);
        long total = adjustmentService.count(semesterId, adjustType, status);

        return Result.success(new PageResult<>(list, total, page, size));
    }
}
