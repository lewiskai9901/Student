package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.InspSubmissionApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.execution.*;
import com.school.management.infrastructure.casbin.CasbinAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/inspection/submissions")
@RequiredArgsConstructor
public class InspSubmissionController {

    private final InspSubmissionApplicationService submissionService;

    // ========== Submission CRUD ==========

    @PostMapping
    @CasbinAccess(resource = "insp:submission", action = "create")
    public Result<InspSubmission> createSubmission(@RequestBody @Valid CreateSubmissionRequest request) {
        return Result.success(submissionService.createSubmission(
                request.getTaskId(), request.getTargetType(),
                request.getTargetId(), request.getTargetName()));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:submission", action = "view")
    public Result<InspSubmission> getSubmission(@PathVariable Long id) {
        return Result.success(submissionService.getSubmission(id)
                .orElseThrow(() -> new IllegalArgumentException("提交不存在: " + id)));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:submission", action = "view")
    public Result<List<InspSubmission>> listSubmissions(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Long targetId) {
        if (taskId != null) {
            return Result.success(submissionService.listSubmissionsByTask(taskId));
        }
        if (targetId != null) {
            return Result.success(submissionService.listSubmissionsByTarget(targetId));
        }
        return Result.success(List.of());
    }

    // ========== Submission Lifecycle ==========

    @PostMapping("/{id}/lock")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    public Result<InspSubmission> lockSubmission(@PathVariable Long id) {
        return Result.success(submissionService.lockSubmission(id));
    }

    @PostMapping("/{id}/unlock")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    public Result<InspSubmission> unlockSubmission(@PathVariable Long id) {
        return Result.success(submissionService.unlockSubmission(id));
    }

    @PostMapping("/{id}/start-filling")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    public Result<InspSubmission> startFilling(@PathVariable Long id) {
        return Result.success(submissionService.startFilling(id));
    }

    @PutMapping("/{id}/form-data")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    public Result<InspSubmission> saveFormData(@PathVariable Long id,
                                                @RequestBody @Valid SaveFormDataRequest request) {
        return Result.success(submissionService.saveFormData(id, request.getFormData()));
    }

    @PostMapping("/{id}/complete")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    public Result<InspSubmission> completeSubmission(@PathVariable Long id) {
        // 后端计算分数 — 不接受任何前端传入的分数 (原 CompleteSubmissionRequest 全字段被忽略, 已删除)
        return Result.success(submissionService.completeSubmission(id));
    }

    @PostMapping("/{id}/skip")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    public Result<InspSubmission> skipSubmission(@PathVariable Long id) {
        return Result.success(submissionService.skipSubmission(id));
    }

    /**
     * 手动触发分数级联重算（管理员用）
     * 重算链：SubmissionDetail → Submission总分 → Task汇总 → ProjectScore
     */
    @PostMapping("/{id}/recalculate")
    @CasbinAccess(resource = "insp:submission", action = "admin")
    public Result<InspSubmission> recalculateScore(@PathVariable Long id) {
        return Result.success(submissionService.recalculateFromSubmission(id));
    }

    // ========== Submission Details ==========

    @PostMapping("/{submissionId}/details")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    public Result<SubmissionDetail> createDetail(@PathVariable Long submissionId,
                                                  @RequestBody @Valid CreateDetailRequest request) {
        boolean hasConfig = request.getScoringConfig() != null
                || request.getValidationRules() != null
                || request.getConditionLogic() != null;
        if (request.getSectionName() != null || request.getScoringMode() != null) {
            ScoringMode mode;
            try {
                mode = request.getScoringMode() != null ? ScoringMode.valueOf(request.getScoringMode()) : null;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("无效的评分模式: " + request.getScoringMode());
            }
            if (hasConfig) {
                return Result.success(submissionService.createDetail(submissionId,
                        request.getTemplateItemId(), request.getItemCode(),
                        request.getItemName(), request.getItemType(),
                        request.getSectionId(), request.getSectionName(), mode,
                        request.getScoringConfig(), request.getValidationRules(), request.getConditionLogic()));
            }
            return Result.success(submissionService.createDetail(submissionId,
                    request.getTemplateItemId(), request.getItemCode(),
                    request.getItemName(), request.getItemType(),
                    request.getSectionId(), request.getSectionName(), mode));
        }
        return Result.success(submissionService.createDetail(submissionId,
                request.getTemplateItemId(), request.getItemCode(),
                request.getItemName(), request.getItemType()));
    }

    @GetMapping("/{submissionId}/details")
    @CasbinAccess(resource = "insp:submission", action = "view")
    public Result<List<SubmissionDetail>> listDetails(@PathVariable Long submissionId) {
        return Result.success(submissionService.listDetailsBySubmission(submissionId));
    }

    @GetMapping("/{submissionId}/details/flagged")
    @CasbinAccess(resource = "insp:submission", action = "view")
    public Result<List<SubmissionDetail>> listFlaggedDetails(@PathVariable Long submissionId) {
        return Result.success(submissionService.listFlaggedDetails(submissionId));
    }

    @PutMapping("/details/{detailId}/response")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    public Result<SubmissionDetail> updateDetailResponse(@PathVariable Long detailId,
                                                          @RequestBody @Valid UpdateDetailResponseRequest request) {
        return Result.success(submissionService.updateDetailResponse(detailId,
                request.getResponseValue(), request.getScoringMode(),
                request.getScore(), request.getDimensions()));
    }

    @PutMapping("/details/{detailId}/remark")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    public Result<SubmissionDetail> updateDetailRemark(@PathVariable Long detailId,
                                                        @RequestBody @Valid RemarkRequest request) {
        return Result.success(submissionService.updateDetailRemark(detailId, request.getRemark()));
    }

    @PostMapping("/details/{detailId}/flag")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    public Result<SubmissionDetail> flagDetail(@PathVariable Long detailId,
                                                @RequestBody @Valid FlagDetailRequest request) {
        return Result.success(submissionService.flagDetail(detailId, request.getReason()));
    }

    @PostMapping("/details/{detailId}/unflag")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    public Result<SubmissionDetail> unflagDetail(@PathVariable Long detailId) {
        return Result.success(submissionService.unflagDetail(detailId));
    }

    @DeleteMapping("/details/{detailId}")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    public Result<Void> deleteDetail(@PathVariable Long detailId) {
        submissionService.deleteDetail(detailId);
        return Result.success();
    }

    // ========== Evidence ==========

    @PostMapping("/{submissionId}/evidences")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    public Result<InspEvidence> addEvidence(@PathVariable Long submissionId,
                                             @RequestBody @Valid AddEvidenceRequest request) {
        return Result.success(submissionService.addEvidence(submissionId,
                request.getDetailId(), request.getEvidenceType(),
                request.getFileName(), request.getFileUrl()));
    }

    @GetMapping("/{submissionId}/evidences")
    @CasbinAccess(resource = "insp:submission", action = "view")
    public Result<List<InspEvidence>> listEvidence(@PathVariable Long submissionId) {
        return Result.success(submissionService.listEvidenceBySubmission(submissionId));
    }

    @DeleteMapping("/evidences/{evidenceId}")
    @CasbinAccess(resource = "insp:submission", action = "execute")
    public Result<Void> deleteEvidence(@PathVariable Long evidenceId) {
        submissionService.deleteEvidence(evidenceId);
        return Result.success();
    }

    // --- Request DTOs ---

    @lombok.Data
    public static class CreateSubmissionRequest {
        @NotNull
        private Long taskId;
        @NotNull
        private TargetType targetType;
        @NotNull
        private Long targetId;
        private String targetName;
    }

    @lombok.Data
    public static class SaveFormDataRequest {
        private String formData;
    }

    @lombok.Data
    public static class CreateDetailRequest {
        private Long templateItemId;
        private String itemCode;
        private String itemName;
        private String itemType;
        private Long sectionId;
        private String sectionName;
        private String scoringMode;
        private String scoringConfig;
        private String validationRules;
        private String conditionLogic;
    }

    @lombok.Data
    public static class UpdateDetailResponseRequest {
        private String responseValue;
        private ScoringMode scoringMode;
        private BigDecimal score;
        private String dimensions;
    }

    @lombok.Data
    public static class RemarkRequest {
        private String remark;
    }

    @lombok.Data
    public static class FlagDetailRequest {
        private String reason;
    }

    @lombok.Data
    public static class AddEvidenceRequest {
        private Long detailId;
        @NotNull
        private EvidenceType evidenceType;
        private String fileName;
        @NotNull
        private String fileUrl;
    }
}
