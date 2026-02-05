package com.school.management.interfaces.rest.inspection.v6;

import com.school.management.application.inspection.v6.CorrectiveActionApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v6.CorrectiveAction;
import com.school.management.domain.inspection.model.v6.CorrectiveActionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * V6整改记录控制器
 */
@RestController
@RequestMapping("/v6/corrective-actions")
@RequiredArgsConstructor
@Tag(name = "V6整改管理", description = "整改记录管理接口")
public class CorrectiveActionController {

    private final CorrectiveActionApplicationService correctiveActionService;

    @Operation(summary = "创建整改记录")
    @PostMapping
    public Result<CorrectiveActionResponse> createAction(@RequestBody CreateActionRequest request) {
        CorrectiveAction action = correctiveActionService.createAction(
                request.getDetailId(),
                request.getTargetId(),
                request.getTaskId(),
                request.getProjectId(),
                request.getIssueDescription(),
                request.getRequiredAction(),
                request.getDeadline(),
                request.getAssigneeId(),
                request.getAssigneeName(),
                request.getCreatedBy()
        );
        return Result.success(toResponse(action));
    }

    @Operation(summary = "提交整改")
    @PostMapping("/{id}/submit")
    public Result<CorrectiveActionResponse> submitCorrection(
            @PathVariable Long id,
            @RequestBody SubmitCorrectionRequest request) {
        CorrectiveAction action = correctiveActionService.submitCorrection(
                id, request.getCorrectionNote(), request.getEvidenceIds());
        return Result.success(toResponse(action));
    }

    @Operation(summary = "验收通过")
    @PostMapping("/{id}/verify")
    public Result<CorrectiveActionResponse> verify(
            @PathVariable Long id,
            @RequestBody VerifyRequest request) {
        CorrectiveAction action = correctiveActionService.verify(
                id, request.getVerifierId(), request.getVerifierName(), request.getVerificationNote());
        return Result.success(toResponse(action));
    }

    @Operation(summary = "验收驳回")
    @PostMapping("/{id}/reject")
    public Result<CorrectiveActionResponse> reject(
            @PathVariable Long id,
            @RequestBody VerifyRequest request) {
        CorrectiveAction action = correctiveActionService.reject(
                id, request.getVerifierId(), request.getVerifierName(), request.getVerificationNote());
        return Result.success(toResponse(action));
    }

    @Operation(summary = "取消整改")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelAction(@PathVariable Long id) {
        correctiveActionService.cancelAction(id);
        return Result.success();
    }

    @Operation(summary = "删除整改记录")
    @DeleteMapping("/{id}")
    public Result<Void> deleteAction(@PathVariable Long id) {
        correctiveActionService.deleteAction(id);
        return Result.success();
    }

    @Operation(summary = "根据ID查询")
    @GetMapping("/{id}")
    public Result<CorrectiveActionResponse> getById(@PathVariable Long id) {
        CorrectiveAction action = correctiveActionService.getById(id);
        return Result.success(toResponse(action));
    }

    @Operation(summary = "根据检查明细查询")
    @GetMapping("/detail/{detailId}")
    public Result<List<CorrectiveActionResponse>> getByDetailId(@PathVariable Long detailId) {
        List<CorrectiveAction> actions = correctiveActionService.getByDetailId(detailId);
        return Result.success(actions.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "根据检查目标查询")
    @GetMapping("/target/{targetId}")
    public Result<List<CorrectiveActionResponse>> getByTargetId(@PathVariable Long targetId) {
        List<CorrectiveAction> actions = correctiveActionService.getByTargetId(targetId);
        return Result.success(actions.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "根据检查任务查询")
    @GetMapping("/task/{taskId}")
    public Result<List<CorrectiveActionResponse>> getByTaskId(@PathVariable Long taskId) {
        List<CorrectiveAction> actions = correctiveActionService.getByTaskId(taskId);
        return Result.success(actions.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "根据检查项目查询")
    @GetMapping("/project/{projectId}")
    public Result<List<CorrectiveActionResponse>> getByProjectId(@PathVariable Long projectId) {
        List<CorrectiveAction> actions = correctiveActionService.getByProjectId(projectId);
        return Result.success(actions.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "查询我的待整改")
    @GetMapping("/my-pending")
    public Result<List<CorrectiveActionResponse>> getMyPendingActions(@RequestParam Long assigneeId) {
        List<CorrectiveAction> actions = correctiveActionService.getMyPendingActions(assigneeId);
        return Result.success(actions.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "查询逾期整改")
    @GetMapping("/overdue")
    public Result<List<CorrectiveActionResponse>> getOverdueActions() {
        List<CorrectiveAction> actions = correctiveActionService.getOverdueActions();
        return Result.success(actions.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "根据项目和状态查询")
    @GetMapping("/project/{projectId}/status/{status}")
    public Result<List<CorrectiveActionResponse>> getByProjectIdAndStatus(
            @PathVariable Long projectId,
            @PathVariable CorrectiveActionStatus status) {
        List<CorrectiveAction> actions = correctiveActionService.getByProjectIdAndStatus(projectId, status);
        return Result.success(actions.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @Operation(summary = "项目整改统计")
    @GetMapping("/project/{projectId}/stats")
    public Result<StatsResponse> getProjectStats(@PathVariable Long projectId) {
        var stats = correctiveActionService.getProjectStats(projectId);
        return Result.success(new StatsResponse(stats.pending(), stats.submitted(), stats.verified(), stats.rejected(), stats.total()));
    }

    @Operation(summary = "获取所有状态")
    @GetMapping("/statuses")
    public Result<List<StatusInfo>> getStatuses() {
        List<StatusInfo> statuses = List.of(
                new StatusInfo(CorrectiveActionStatus.PENDING.name(), CorrectiveActionStatus.PENDING.getDescription()),
                new StatusInfo(CorrectiveActionStatus.SUBMITTED.name(), CorrectiveActionStatus.SUBMITTED.getDescription()),
                new StatusInfo(CorrectiveActionStatus.VERIFIED.name(), CorrectiveActionStatus.VERIFIED.getDescription()),
                new StatusInfo(CorrectiveActionStatus.REJECTED.name(), CorrectiveActionStatus.REJECTED.getDescription()),
                new StatusInfo(CorrectiveActionStatus.CANCELLED.name(), CorrectiveActionStatus.CANCELLED.getDescription())
        );
        return Result.success(statuses);
    }

    private CorrectiveActionResponse toResponse(CorrectiveAction action) {
        CorrectiveActionResponse response = new CorrectiveActionResponse();
        response.setId(action.getId());
        response.setDetailId(action.getDetailId());
        response.setTargetId(action.getTargetId());
        response.setTaskId(action.getTaskId());
        response.setProjectId(action.getProjectId());
        response.setActionCode(action.getActionCode());
        response.setIssueDescription(action.getIssueDescription());
        response.setRequiredAction(action.getRequiredAction());
        response.setDeadline(action.getDeadline());
        response.setAssigneeId(action.getAssigneeId());
        response.setAssigneeName(action.getAssigneeName());
        response.setStatus(action.getStatus() != null ? action.getStatus().name() : null);
        response.setStatusName(action.getStatus() != null ? action.getStatus().getDescription() : null);
        response.setCorrectionNote(action.getCorrectionNote());
        response.setEvidenceIds(action.getEvidenceIds());
        response.setCorrectedAt(action.getCorrectedAt());
        response.setVerifierId(action.getVerifierId());
        response.setVerifierName(action.getVerifierName());
        response.setVerifiedAt(action.getVerifiedAt());
        response.setVerificationNote(action.getVerificationNote());
        response.setOverdue(action.isOverdue());
        response.setCreatedAt(action.getCreatedAt());
        return response;
    }

    @Data
    public static class CreateActionRequest {
        private Long detailId;
        private Long targetId;
        private Long taskId;
        private Long projectId;
        private String issueDescription;
        private String requiredAction;
        private LocalDate deadline;
        private Long assigneeId;
        private String assigneeName;
        private Long createdBy;
    }

    @Data
    public static class SubmitCorrectionRequest {
        private String correctionNote;
        private String evidenceIds;
    }

    @Data
    public static class VerifyRequest {
        private Long verifierId;
        private String verifierName;
        private String verificationNote;
    }

    @Data
    public static class CorrectiveActionResponse {
        private Long id;
        private Long detailId;
        private Long targetId;
        private Long taskId;
        private Long projectId;
        private String actionCode;
        private String issueDescription;
        private String requiredAction;
        private LocalDate deadline;
        private Long assigneeId;
        private String assigneeName;
        private String status;
        private String statusName;
        private String correctionNote;
        private String evidenceIds;
        private LocalDateTime correctedAt;
        private Long verifierId;
        private String verifierName;
        private LocalDateTime verifiedAt;
        private String verificationNote;
        private boolean overdue;
        private LocalDateTime createdAt;
    }

    @Data
    public static class StatsResponse {
        private long pending;
        private long submitted;
        private long verified;
        private long rejected;
        private long total;

        public StatsResponse(long pending, long submitted, long verified, long rejected, long total) {
            this.pending = pending;
            this.submitted = submitted;
            this.verified = verified;
            this.rejected = rejected;
            this.total = total;
        }
    }

    @Data
    public static class StatusInfo {
        private String code;
        private String name;

        public StatusInfo(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
