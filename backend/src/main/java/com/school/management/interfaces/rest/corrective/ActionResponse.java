package com.school.management.interfaces.rest.corrective;

import com.school.management.domain.corrective.model.CorrectiveAction;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActionResponse {
    private Long id;
    private String actionCode;
    private String title;
    private String description;
    private String source;
    private Long sourceId;
    private String severity;
    private String category;
    private String status;
    private Long classId;
    private Long assigneeId;
    private LocalDateTime deadline;
    private String resolutionNote;
    private List<String> resolutionAttachments;
    private LocalDateTime resolvedAt;
    private Long verifierId;
    private String verificationResult;
    private String verificationComment;
    private LocalDateTime verifiedAt;
    private int escalationLevel;
    private Long createdBy;
    private LocalDateTime createdAt;

    public static ActionResponse fromDomain(CorrectiveAction action) {
        ActionResponse resp = new ActionResponse();
        resp.setId(action.getId());
        resp.setActionCode(action.getActionCode());
        resp.setTitle(action.getTitle());
        resp.setDescription(action.getDescription());
        resp.setSource(action.getSource().name());
        resp.setSourceId(action.getSourceId());
        resp.setSeverity(action.getSeverity().name());
        resp.setCategory(action.getCategory().name());
        resp.setStatus(action.getStatus().name());
        resp.setClassId(action.getClassId());
        resp.setAssigneeId(action.getAssigneeId());
        resp.setDeadline(action.getDeadline());
        resp.setResolutionNote(action.getResolutionNote());
        resp.setResolutionAttachments(action.getResolutionAttachments());
        resp.setResolvedAt(action.getResolvedAt());
        resp.setVerifierId(action.getVerifierId());
        resp.setVerificationResult(action.getVerificationResult());
        resp.setVerificationComment(action.getVerificationComment());
        resp.setVerifiedAt(action.getVerifiedAt());
        resp.setEscalationLevel(action.getEscalationLevel());
        resp.setCreatedBy(action.getCreatedBy());
        resp.setCreatedAt(action.getCreatedAt());
        return resp;
    }
}
