package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.InspAppealApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.appeal.InspAppeal;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 检查申诉 REST 接口 (P1#8) — 实现 inspection:appeal:create / review / view 三个权限点.
 */
@RestController
@RequestMapping("/inspection/appeals")
@RequiredArgsConstructor
public class InspAppealController {

    private final InspAppealApplicationService appealService;

    /** 提交申诉 — inspection:appeal:create */
    @PostMapping
    @CasbinAccess(resource = "inspection_appeal", action = "create")
    public Result<InspAppeal> submit(@RequestBody SubmitAppealRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(appealService.submitAppeal(
                request.getSubmissionDetailId(), userId, request.getSubmitterName(),
                request.getReason(), request.getAttachments(), request.getExpectedAdjustment()));
    }

    /** 审核通过 — inspection:appeal:review */
    @PostMapping("/{id}/approve")
    @CasbinAccess(resource = "inspection_appeal", action = "review")
    public Result<InspAppeal> approve(@PathVariable Long id, @RequestBody ApproveAppealRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(appealService.approve(id, userId, request.getReviewerName(),
                request.getComment(), request.getFinalAdjustment()));
    }

    /** 审核驳回 — inspection:appeal:review */
    @PostMapping("/{id}/reject")
    @CasbinAccess(resource = "inspection_appeal", action = "review")
    public Result<InspAppeal> reject(@PathVariable Long id, @RequestBody RejectAppealRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(appealService.reject(id, userId, request.getReviewerName(),
                request.getComment()));
    }

    /** 撤回申诉 — 仅提交人, 不需独立权限点 (走 SELF data scope) */
    @PostMapping("/{id}/withdraw")
    @CasbinAccess(resource = "inspection_appeal", action = "create")
    public Result<InspAppeal> withdraw(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(appealService.withdraw(id, userId));
    }

    /** 单条查询 — inspection:appeal:view */
    @GetMapping("/{id}")
    @CasbinAccess(resource = "inspection_appeal", action = "view")
    public Result<InspAppeal> getById(@PathVariable Long id) {
        return Result.success(appealService.getAppeal(id)
                .orElseThrow(() -> new IllegalArgumentException("申诉不存在: " + id)));
    }

    /** 我的申诉 (提交人视角) — inspection:appeal:view */
    @GetMapping("/my")
    @CasbinAccess(resource = "inspection_appeal", action = "view")
    public Result<List<InspAppeal>> listMy() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(appealService.listMyAppeals(userId));
    }

    /** 待审核清单 (审核员视角) — inspection:appeal:review */
    @GetMapping("/pending")
    @CasbinAccess(resource = "inspection_appeal", action = "review")
    public Result<List<InspAppeal>> listPending() {
        return Result.success(appealService.listPending());
    }

    /** 按项目查询 — inspection:appeal:view */
    @GetMapping("/by-project/{projectId}")
    @CasbinAccess(resource = "inspection_appeal", action = "view")
    public Result<List<InspAppeal>> listByProject(@PathVariable Long projectId) {
        return Result.success(appealService.listByProject(projectId));
    }

    // --- DTOs ---

    @lombok.Data
    public static class SubmitAppealRequest {
        private Long submissionDetailId;
        private String submitterName;
        private String reason;
        private String attachments;
        private BigDecimal expectedAdjustment;
    }

    @lombok.Data
    public static class ApproveAppealRequest {
        private String reviewerName;
        private String comment;
        private BigDecimal finalAdjustment;
    }

    @lombok.Data
    public static class RejectAppealRequest {
        private String reviewerName;
        private String comment;
    }
}
