package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetApprovalApplicationService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Asset Approval REST Controller.
 * <p>M2 (2026-05-20): 10 处直 jdbc 已下沉到 AssetApprovalApplicationService.
 */
@Slf4j
@RestController
@RequestMapping("/asset-approvals")
@RequiredArgsConstructor
public class AssetApprovalController {

    private final AssetApprovalApplicationService approvalService;

    @PostMapping
    @CasbinAccess(resource = "asset:approval", action = "edit")
    public Result<Long> createApproval(@RequestBody Map<String, Object> data) {
        return Result.success(approvalService.create(data));
    }

    @PostMapping("/{id}/approve")
    @CasbinAccess(resource = "asset:approval", action = "edit")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> data) {
        String remark = data != null ? (String) data.get("remark") : null;
        approvalService.approve(id, remark);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    @CasbinAccess(resource = "asset:approval", action = "edit")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> data) {
        String remark = data != null ? (String) data.get("remark") : null;
        approvalService.reject(id, remark);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @CasbinAccess(resource = "asset:approval", action = "edit")
    public Result<Void> cancel(@PathVariable Long id) {
        approvalService.cancel(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "asset:approval", action = "view")
    public Result<Map<String, Object>> getApproval(@PathVariable Long id) {
        Map<String, Object> approval = approvalService.findById(id);
        if (approval == null) return Result.error("审批单不存在");
        enrichApproval(approval);
        return Result.success(approval);
    }

    @GetMapping("/my")
    @CasbinAccess(resource = "asset:approval", action = "view")
    public Result<List<Map<String, Object>>> getMyApprovals() {
        List<Map<String, Object>> approvals = approvalService.listAll();
        for (Map<String, Object> a : approvals) enrichApproval(a);
        return Result.success(approvals);
    }

    @GetMapping("/pending")
    @CasbinAccess(resource = "asset:approval", action = "view")
    public Result<List<Map<String, Object>>> getPendingApprovals() {
        List<Map<String, Object>> approvals = approvalService.listPending();
        for (Map<String, Object> a : approvals) enrichApproval(a);
        return Result.success(approvals);
    }

    @GetMapping
    @CasbinAccess(resource = "asset:approval", action = "view")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> queryApprovals(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer approvalType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long applicantId) {
        Map<String, Object> result = approvalService.queryPaged(pageNum, pageSize, approvalType, status, applicantId);
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.get("records");
        for (Map<String, Object> r : records) enrichApproval(r);
        return Result.success(result);
    }

    @GetMapping("/pending/count")
    @CasbinAccess(resource = "asset:approval", action = "view")
    public Result<Long> countPending() {
        return Result.success(approvalService.countPending());
    }

    // ==================== Presentation Helpers ====================

    private void enrichApproval(Map<String, Object> approval) {
        approval.put("approvalTypeDesc", getApprovalTypeDesc(approval.get("approvalType")));
        approval.put("statusDesc", getApprovalStatusDesc(approval.get("status")));
    }

    private String getApprovalTypeDesc(Object type) {
        if (type == null) return null;
        int t = ((Number) type).intValue();
        switch (t) {
            case 1: return "借用申请";
            case 2: return "采购申请";
            case 3: return "报废申请";
            case 4: return "调拨申请";
            default: return "未知";
        }
    }

    private String getApprovalStatusDesc(Object status) {
        if (status == null) return null;
        int s = ((Number) status).intValue();
        switch (s) {
            case 0: return "待审批";
            case 1: return "已通过";
            case 2: return "已拒绝";
            case 3: return "已取消";
            default: return "未知";
        }
    }
}
