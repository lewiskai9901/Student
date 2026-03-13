package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.CorrectiveCaseApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.corrective.CasePriority;
import com.school.management.domain.inspection.model.v7.corrective.CaseStatus;
import com.school.management.domain.inspection.model.v7.corrective.CorrectiveCase;
import com.school.management.domain.inspection.model.v7.corrective.CorrectiveSubtask;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v7/insp/corrective-cases")
@RequiredArgsConstructor
public class CorrectiveCaseController {

    private final CorrectiveCaseApplicationService caseService;

    // ========== CRUD ==========

    @PostMapping
    @CasbinAccess(resource = "insp:corrective", action = "create")
    public Result<CorrectiveCase> createCase(@RequestBody CreateCaseRequest request) {
        CorrectiveCase c = caseService.createCase(
                request.getCaseCode(), request.getIssueDescription(),
                request.getPriority(), request.getSubmissionId(), request.getDetailId(),
                request.getProjectId(), request.getTaskId(),
                request.getTargetType(), request.getTargetId(), request.getTargetName(),
                request.getRequiredAction(), request.getDeadline(),
                SecurityUtils.getCurrentUserId());
        return Result.success(c);
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:corrective", action = "view")
    public Result<CorrectiveCase> getCase(@PathVariable Long id) {
        return Result.success(caseService.getCase(id)
                .orElseThrow(() -> new IllegalArgumentException("整改案例不存在: " + id)));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:corrective", action = "view")
    public Result<List<CorrectiveCase>> listCases(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long submissionId,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) CaseStatus status) {
        if (projectId != null) return Result.success(caseService.listByProject(projectId));
        if (submissionId != null) return Result.success(caseService.listBySubmission(submissionId));
        if (taskId != null) return Result.success(caseService.listByTask(taskId));
        if (status != null) return Result.success(caseService.listByStatus(status));
        return Result.success(caseService.listAll());
    }

    @GetMapping("/my-cases")
    @CasbinAccess(resource = "insp:corrective", action = "view")
    public Result<List<CorrectiveCase>> listMyCases() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(caseService.listByAssignee(userId));
    }

    @GetMapping("/overdue")
    @CasbinAccess(resource = "insp:corrective", action = "view")
    public Result<List<CorrectiveCase>> listOverdueCases() {
        return Result.success(caseService.listOverdue());
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:corrective", action = "delete")
    public Result<Void> deleteCase(@PathVariable Long id) {
        caseService.deleteCase(id);
        return Result.success(null);
    }

    // ========== Lifecycle ==========

    @PostMapping("/{id}/assign")
    @CasbinAccess(resource = "insp:corrective", action = "manage")
    public Result<CorrectiveCase> assignCase(@PathVariable Long id,
                                              @RequestBody AssignCaseRequest request) {
        return Result.success(caseService.assignCase(id, request.getAssigneeId(), request.getAssigneeName()));
    }

    @PostMapping("/{id}/start-work")
    @CasbinAccess(resource = "insp:corrective", action = "execute")
    public Result<CorrectiveCase> startWork(@PathVariable Long id) {
        return Result.success(caseService.startWork(id));
    }

    @PostMapping("/{id}/submit-correction")
    @CasbinAccess(resource = "insp:corrective", action = "execute")
    public Result<CorrectiveCase> submitCorrection(@PathVariable Long id,
                                                     @RequestBody SubmitCorrectionRequest request) {
        return Result.success(caseService.submitCorrection(id,
                request.getCorrectionNote(), request.getEvidenceIds()));
    }

    @PostMapping("/{id}/verify")
    @CasbinAccess(resource = "insp:corrective", action = "manage")
    public Result<CorrectiveCase> verifyCase(@PathVariable Long id,
                                              @RequestBody VerifyCaseRequest request) {
        Long verifierId = SecurityUtils.getCurrentUserId();
        return Result.success(caseService.verifyCase(id, verifierId,
                request.getVerifierName(), request.getNote()));
    }

    @PostMapping("/{id}/reject")
    @CasbinAccess(resource = "insp:corrective", action = "manage")
    public Result<CorrectiveCase> rejectCase(@PathVariable Long id,
                                              @RequestBody RejectCaseRequest request) {
        Long verifierId = SecurityUtils.getCurrentUserId();
        return Result.success(caseService.rejectCase(id, verifierId,
                request.getVerifierName(), request.getReason()));
    }

    @PostMapping("/{id}/close")
    @CasbinAccess(resource = "insp:corrective", action = "manage")
    public Result<CorrectiveCase> closeCase(@PathVariable Long id) {
        return Result.success(caseService.closeCase(id));
    }

    @PostMapping("/{id}/escalate")
    @CasbinAccess(resource = "insp:corrective", action = "manage")
    public Result<CorrectiveCase> escalateCase(@PathVariable Long id) {
        return Result.success(caseService.escalateCase(id));
    }

    // ========== Subtasks ==========

    @GetMapping("/{caseId}/subtasks")
    @CasbinAccess(resource = "insp:corrective", action = "view")
    public Result<List<CorrectiveSubtask>> getSubtasks(@PathVariable Long caseId) {
        return Result.success(caseService.getSubtasks(caseId));
    }

    @PostMapping("/{caseId}/subtasks")
    @CasbinAccess(resource = "insp:corrective", action = "manage")
    public Result<CorrectiveSubtask> createSubtask(@PathVariable Long caseId,
                                                     @RequestBody CreateSubtaskRequest request) {
        return Result.success(caseService.createSubtask(
                caseId, request.getSubtaskName(), request.getDescription(),
                request.getAssigneeId(), request.getPriority(), request.getDueDate(),
                SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/{caseId}/subtasks/{subtaskId}")
    @CasbinAccess(resource = "insp:corrective", action = "manage")
    public Result<CorrectiveSubtask> updateSubtask(@PathVariable Long caseId,
                                                     @PathVariable Long subtaskId,
                                                     @RequestBody CreateSubtaskRequest request) {
        return Result.success(caseService.updateSubtask(
                subtaskId, request.getSubtaskName(), request.getDescription(),
                request.getAssigneeId(), request.getPriority(), request.getDueDate()));
    }

    @PutMapping("/{caseId}/subtasks/{subtaskId}/start")
    @CasbinAccess(resource = "insp:corrective", action = "execute")
    public Result<CorrectiveSubtask> startSubtask(@PathVariable Long caseId,
                                                    @PathVariable Long subtaskId) {
        return Result.success(caseService.startSubtask(subtaskId));
    }

    @PutMapping("/{caseId}/subtasks/{subtaskId}/complete")
    @CasbinAccess(resource = "insp:corrective", action = "execute")
    public Result<CorrectiveSubtask> completeSubtask(@PathVariable Long caseId,
                                                       @PathVariable Long subtaskId) {
        return Result.success(caseService.completeSubtask(subtaskId));
    }

    @PutMapping("/{caseId}/subtasks/{subtaskId}/block")
    @CasbinAccess(resource = "insp:corrective", action = "execute")
    public Result<CorrectiveSubtask> blockSubtask(@PathVariable Long caseId,
                                                    @PathVariable Long subtaskId) {
        return Result.success(caseService.blockSubtask(subtaskId));
    }

    @DeleteMapping("/{caseId}/subtasks/{subtaskId}")
    @CasbinAccess(resource = "insp:corrective", action = "manage")
    public Result<Void> deleteSubtask(@PathVariable Long caseId,
                                       @PathVariable Long subtaskId) {
        caseService.deleteSubtask(subtaskId);
        return Result.success(null);
    }

    // ========== Request DTOs ==========

    @lombok.Data
    public static class CreateCaseRequest {
        private String caseCode;
        private String issueDescription;
        private CasePriority priority;
        private Long submissionId;
        private Long detailId;
        private Long projectId;
        private Long taskId;
        private String targetType;
        private Long targetId;
        private String targetName;
        private String requiredAction;
        private LocalDateTime deadline;
    }

    @lombok.Data
    public static class AssignCaseRequest {
        private Long assigneeId;
        private String assigneeName;
    }

    @lombok.Data
    public static class SubmitCorrectionRequest {
        private String correctionNote;
        private List<Long> evidenceIds;
    }

    @lombok.Data
    public static class VerifyCaseRequest {
        private String verifierName;
        private String note;
    }

    @lombok.Data
    public static class RejectCaseRequest {
        private String verifierName;
        private String reason;
    }

    @lombok.Data
    public static class CreateSubtaskRequest {
        private String subtaskName;
        private String description;
        private Long assigneeId;
        private Integer priority;
        private LocalDate dueDate;
    }
}
