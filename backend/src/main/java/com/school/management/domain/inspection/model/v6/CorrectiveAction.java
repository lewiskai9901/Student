package com.school.management.domain.inspection.model.v6;

import com.school.management.domain.shared.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V6整改记录
 */
public class CorrectiveAction implements Entity<Long> {

    private Long id;
    private Long detailId;          // 关联的检查明细ID
    private Long targetId;          // 检查目标ID
    private Long taskId;            // 检查任务ID
    private Long projectId;         // 检查项目ID
    private String actionCode;      // 整改单号
    private String issueDescription;// 问题描述
    private String requiredAction;  // 整改要求
    private LocalDate deadline;     // 整改截止日期
    private Long assigneeId;        // 整改责任人ID
    private String assigneeName;    // 整改责任人姓名
    private CorrectiveActionStatus status;
    private String correctionNote;  // 整改说明
    private String evidenceIds;     // JSON - 整改证据ID列表
    private LocalDateTime correctedAt;  // 整改完成时间
    private Long verifierId;        // 验收人ID
    private String verifierName;    // 验收人姓名
    private LocalDateTime verifiedAt;   // 验收时间
    private String verificationNote;    // 验收说明
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected CorrectiveAction() {
    }

    private CorrectiveAction(Builder builder) {
        this.id = builder.id;
        this.detailId = builder.detailId;
        this.targetId = builder.targetId;
        this.taskId = builder.taskId;
        this.projectId = builder.projectId;
        this.actionCode = builder.actionCode;
        this.issueDescription = builder.issueDescription;
        this.requiredAction = builder.requiredAction;
        this.deadline = builder.deadline;
        this.assigneeId = builder.assigneeId;
        this.assigneeName = builder.assigneeName;
        this.status = builder.status != null ? builder.status : CorrectiveActionStatus.PENDING;
        this.correctionNote = builder.correctionNote;
        this.evidenceIds = builder.evidenceIds;
        this.correctedAt = builder.correctedAt;
        this.verifierId = builder.verifierId;
        this.verifierName = builder.verifierName;
        this.verifiedAt = builder.verifiedAt;
        this.verificationNote = builder.verificationNote;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    /**
     * 创建整改记录
     */
    public static CorrectiveAction create(Long detailId, Long targetId, Long taskId, Long projectId,
                                          String actionCode, String issueDescription, String requiredAction,
                                          LocalDate deadline, Long assigneeId, String assigneeName, Long createdBy) {
        return builder()
                .detailId(detailId)
                .targetId(targetId)
                .taskId(taskId)
                .projectId(projectId)
                .actionCode(actionCode)
                .issueDescription(issueDescription)
                .requiredAction(requiredAction)
                .deadline(deadline)
                .assigneeId(assigneeId)
                .assigneeName(assigneeName)
                .createdBy(createdBy)
                .build();
    }

    /**
     * 提交整改
     */
    public void submitCorrection(String correctionNote, String evidenceIds) {
        if (this.status != CorrectiveActionStatus.PENDING && this.status != CorrectiveActionStatus.REJECTED) {
            throw new IllegalStateException("当前状态不能提交整改");
        }
        this.correctionNote = correctionNote;
        this.evidenceIds = evidenceIds;
        this.correctedAt = LocalDateTime.now();
        this.status = CorrectiveActionStatus.SUBMITTED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 验收通过
     */
    public void verify(Long verifierId, String verifierName, String verificationNote) {
        if (this.status != CorrectiveActionStatus.SUBMITTED) {
            throw new IllegalStateException("当前状态不能验收");
        }
        this.verifierId = verifierId;
        this.verifierName = verifierName;
        this.verificationNote = verificationNote;
        this.verifiedAt = LocalDateTime.now();
        this.status = CorrectiveActionStatus.VERIFIED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 验收驳回
     */
    public void reject(Long verifierId, String verifierName, String verificationNote) {
        if (this.status != CorrectiveActionStatus.SUBMITTED) {
            throw new IllegalStateException("当前状态不能驳回");
        }
        this.verifierId = verifierId;
        this.verifierName = verifierName;
        this.verificationNote = verificationNote;
        this.verifiedAt = LocalDateTime.now();
        this.status = CorrectiveActionStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消整改
     */
    public void cancel() {
        if (this.status == CorrectiveActionStatus.VERIFIED) {
            throw new IllegalStateException("已验收的整改不能取消");
        }
        this.status = CorrectiveActionStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 是否已逾期
     */
    public boolean isOverdue() {
        if (this.status == CorrectiveActionStatus.VERIFIED || this.status == CorrectiveActionStatus.CANCELLED) {
            return false;
        }
        return deadline != null && LocalDate.now().isAfter(deadline);
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDetailId() {
        return detailId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public String getRequiredAction() {
        return requiredAction;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public CorrectiveActionStatus getStatus() {
        return status;
    }

    public String getCorrectionNote() {
        return correctionNote;
    }

    public String getEvidenceIds() {
        return evidenceIds;
    }

    public LocalDateTime getCorrectedAt() {
        return correctedAt;
    }

    public Long getVerifierId() {
        return verifierId;
    }

    public String getVerifierName() {
        return verifierName;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public String getVerificationNote() {
        return verificationNote;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
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
        private CorrectiveActionStatus status;
        private String correctionNote;
        private String evidenceIds;
        private LocalDateTime correctedAt;
        private Long verifierId;
        private String verifierName;
        private LocalDateTime verifiedAt;
        private String verificationNote;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder detailId(Long detailId) { this.detailId = detailId; return this; }
        public Builder targetId(Long targetId) { this.targetId = targetId; return this; }
        public Builder taskId(Long taskId) { this.taskId = taskId; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder actionCode(String actionCode) { this.actionCode = actionCode; return this; }
        public Builder issueDescription(String issueDescription) { this.issueDescription = issueDescription; return this; }
        public Builder requiredAction(String requiredAction) { this.requiredAction = requiredAction; return this; }
        public Builder deadline(LocalDate deadline) { this.deadline = deadline; return this; }
        public Builder assigneeId(Long assigneeId) { this.assigneeId = assigneeId; return this; }
        public Builder assigneeName(String assigneeName) { this.assigneeName = assigneeName; return this; }
        public Builder status(CorrectiveActionStatus status) { this.status = status; return this; }
        public Builder correctionNote(String correctionNote) { this.correctionNote = correctionNote; return this; }
        public Builder evidenceIds(String evidenceIds) { this.evidenceIds = evidenceIds; return this; }
        public Builder correctedAt(LocalDateTime correctedAt) { this.correctedAt = correctedAt; return this; }
        public Builder verifierId(Long verifierId) { this.verifierId = verifierId; return this; }
        public Builder verifierName(String verifierName) { this.verifierName = verifierName; return this; }
        public Builder verifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; return this; }
        public Builder verificationNote(String verificationNote) { this.verificationNote = verificationNote; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public CorrectiveAction build() {
            return new CorrectiveAction(this);
        }
    }
}
