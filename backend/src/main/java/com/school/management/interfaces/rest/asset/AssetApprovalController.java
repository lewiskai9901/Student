package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetApprovalApplicationService;
import com.school.management.application.asset.command.ApproveCommand;
import com.school.management.application.asset.command.CreateApprovalCommand;
import com.school.management.application.asset.query.AssetApprovalDTO;
import com.school.management.common.result.Result;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产审批控制器
 */
@Tag(name = "资产审批", description = "资产审批管理接口")
@RestController
@RequestMapping("/v2/asset-approvals")
@RequiredArgsConstructor
public class AssetApprovalController {

    private final AssetApprovalApplicationService approvalService;

    @Operation(summary = "创建审批申请")
    @PostMapping
    public Result<Long> createApproval(
            @Valid @RequestBody CreateApprovalRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        CreateApprovalCommand command = CreateApprovalCommand.builder()
                .approvalType(request.getApprovalType())
                .businessId(request.getBusinessId())
                .assetId(request.getAssetId())
                .assetName(request.getAssetName())
                .applicantId(user.getId())
                .applicantName(user.getRealName())
                .applicantDept(user.getOrgUnitName())
                .applyReason(request.getApplyReason())
                .applyQuantity(request.getApplyQuantity())
                .applyAmount(request.getApplyAmount())
                .build();

        Long id = approvalService.createApproval(command);
        return Result.success(id);
    }

    @Operation(summary = "审批通过")
    @PostMapping("/{id}/approve")
    public Result<Void> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalActionRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        ApproveCommand command = ApproveCommand.builder()
                .approvalId(id)
                .approverId(user.getId())
                .approverName(user.getRealName())
                .remark(request != null ? request.getRemark() : null)
                .approved(true)
                .build();

        approvalService.approve(command);
        return Result.success();
    }

    @Operation(summary = "审批拒绝")
    @PostMapping("/{id}/reject")
    public Result<Void> reject(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalActionRequest request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        ApproveCommand command = ApproveCommand.builder()
                .approvalId(id)
                .approverId(user.getId())
                .approverName(user.getRealName())
                .remark(request != null ? request.getRemark() : null)
                .approved(false)
                .build();

        approvalService.approve(command);
        return Result.success();
    }

    @Operation(summary = "取消申请")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        approvalService.cancel(id, user.getId());
        return Result.success();
    }

    @Operation(summary = "获取审批详情")
    @GetMapping("/{id}")
    public Result<AssetApprovalDTO> getApproval(@PathVariable Long id) {
        return Result.success(approvalService.getApproval(id));
    }

    @Operation(summary = "获取我的申请列表")
    @GetMapping("/my")
    public Result<List<AssetApprovalDTO>> getMyApprovals(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return Result.success(approvalService.getMyApprovals(user.getId()));
    }

    @Operation(summary = "获取待审批列表")
    @GetMapping("/pending")
    public Result<List<AssetApprovalDTO>> getPendingApprovals(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return Result.success(approvalService.getPendingApprovals(user.getId()));
    }

    @Operation(summary = "分页查询审批列表")
    @GetMapping
    public Result<Map<String, Object>> queryApprovals(
            @RequestParam(required = false) Integer approvalType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long applicantId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        List<AssetApprovalDTO> list = approvalService.queryApprovals(
                approvalType, status, applicantId, null, pageNum, pageSize
        );
        int total = approvalService.countApprovals(approvalType, status, applicantId, null);

        Map<String, Object> result = new HashMap<>();
        result.put("records", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    @Operation(summary = "获取待审批数量")
    @GetMapping("/pending/count")
    public Result<Integer> countPending() {
        return Result.success(approvalService.countPending());
    }
}
