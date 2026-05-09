package com.school.management.interfaces.rest.access;

import com.school.management.application.access.AccessRelationService;
import com.school.management.application.access.RelationApprovalService;
import com.school.management.common.result.Result;
import com.school.management.domain.access.model.entity.PendingRelationApproval;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 关系审批 REST 接口.
 *
 * <p>审批通过后(approve), 自动调 {@link AccessRelationService#applyApprovedRequest}
 * 把 pending 记录落到 access_relations, 返回新关系 id.
 */
@RestController
@RequestMapping("/access-relations/approvals")
@RequiredArgsConstructor
public class RelationApprovalController {

    private final RelationApprovalService approvalService;
    private final AccessRelationService accessRelationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<List<PendingRelationApproval>> listPending() {
        return Result.success(approvalService.listPending());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('access:approval:review')")
    public Result<Long> approve(@PathVariable Long id, @RequestBody ApproveRequest req) {
        Optional<PendingRelationApproval> p = approvalService.approve(id, req.getApproverId());
        if (p.isEmpty()) {
            return Result.error("审批失败 — 状态非 PENDING 或 ID 不存在");
        }
        Long newRelationId = accessRelationService.applyApprovedRequest(id, req.getApproverId());
        return Result.success(newRelationId);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('access:approval:review')")
    public Result<Void> reject(@PathVariable Long id, @RequestBody RejectRequest req) {
        approvalService.reject(id, req.getApproverId(), req.getReason());
        return Result.success(null);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody CancelRequest req) {
        approvalService.cancel(id, req.getRequesterId());
        return Result.success(null);
    }

    @lombok.Data public static class ApproveRequest { private Long approverId; }
    @lombok.Data public static class RejectRequest { private Long approverId; private String reason; }
    @lombok.Data public static class CancelRequest { private Long requesterId; }
}
