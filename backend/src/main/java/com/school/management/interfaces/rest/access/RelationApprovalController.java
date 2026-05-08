package com.school.management.interfaces.rest.access;

import com.school.management.application.access.RelationApprovalService;
import com.school.management.common.result.Result;
import com.school.management.domain.access.model.entity.PendingRelationApproval;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 关系审批 REST 接口.
 */
@RestController
@RequestMapping("/access-relations/approvals")
@RequiredArgsConstructor
public class RelationApprovalController {

    private final RelationApprovalService approvalService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<List<PendingRelationApproval>> listPending() {
        return Result.success(approvalService.listPending());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('access:approval:review')")
    public Result<Void> approve(@PathVariable Long id, @RequestBody ApproveRequest req) {
        approvalService.approve(id, req.getApproverId());
        return Result.success(null);
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
