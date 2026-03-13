package com.school.management.domain.inspection.model.v7.corrective;

import com.school.management.domain.inspection.event.v7.*;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * V7 整改案例聚合根（完整 CAPA）
 * 状态机: OPEN → ASSIGNED → IN_PROGRESS → SUBMITTED → VERIFIED|REJECTED → CLOSED
 *         CLOSED → (效果验证) EFFECTIVENESS_PENDING → EFFECTIVENESS_CONFIRMED|EFFECTIVENESS_FAILED
 *         ESCALATED (任何非终态可升级)
 */
public class CorrectiveCase extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private String caseCode;
    private Long submissionId;
    private Long detailId;
    private Long projectId;
    private Long taskId;
    private String targetType;
    private Long targetId;
    private String targetName;

    // 问题描述
    private String issueDescription;
    private String requiredAction;
    private Long issueCategoryId;
    private String deficiencyCode;

    // 根因分析 (RCA)
    private RcaMethod rcaMethod;
    private String rcaData;

    // 预防措施 (CAPA 的 PA)
    private String preventiveAction;

    // 优先级与时限
    private CasePriority priority;
    private LocalDateTime deadline;
    private Long assigneeId;
    private String assigneeName;
    private Integer escalationLevel;

    // 状态
    private CaseStatus status;

    // 整改证据
    private String correctionNote;
    private List<Long> correctionEvidenceIds;
    private LocalDateTime correctedAt;

    // 验证
    private Long verifierId;
    private String verifierName;
    private LocalDateTime verifiedAt;
    private String verificationNote;

    // 效果验证
    private LocalDate effectivenessCheckDate;
    private EffectivenessStatus effectivenessStatus;
    private String effectivenessNote;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected CorrectiveCase() {
    }

    private CorrectiveCase(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.caseCode = builder.caseCode;
        this.submissionId = builder.submissionId;
        this.detailId = builder.detailId;
        this.projectId = builder.projectId;
        this.taskId = builder.taskId;
        this.targetType = builder.targetType;
        this.targetId = builder.targetId;
        this.targetName = builder.targetName;
        this.issueDescription = builder.issueDescription;
        this.requiredAction = builder.requiredAction;
        this.issueCategoryId = builder.issueCategoryId;
        this.deficiencyCode = builder.deficiencyCode;
        this.rcaMethod = builder.rcaMethod != null ? builder.rcaMethod : RcaMethod.NONE;
        this.rcaData = builder.rcaData;
        this.preventiveAction = builder.preventiveAction;
        this.priority = builder.priority != null ? builder.priority : CasePriority.MEDIUM;
        this.deadline = builder.deadline;
        this.assigneeId = builder.assigneeId;
        this.assigneeName = builder.assigneeName;
        this.escalationLevel = builder.escalationLevel != null ? builder.escalationLevel : 0;
        this.status = builder.status != null ? builder.status : CaseStatus.OPEN;
        this.correctionNote = builder.correctionNote;
        this.correctionEvidenceIds = builder.correctionEvidenceIds;
        this.correctedAt = builder.correctedAt;
        this.verifierId = builder.verifierId;
        this.verifierName = builder.verifierName;
        this.verifiedAt = builder.verifiedAt;
        this.verificationNote = builder.verificationNote;
        this.effectivenessCheckDate = builder.effectivenessCheckDate;
        this.effectivenessStatus = builder.effectivenessStatus;
        this.effectivenessNote = builder.effectivenessNote;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static CorrectiveCase create(String caseCode, String issueDescription,
                                        CasePriority priority, Long createdBy) {
        CorrectiveCase c = builder()
                .caseCode(caseCode)
                .issueDescription(issueDescription)
                .priority(priority)
                .status(CaseStatus.OPEN)
                .createdBy(createdBy)
                .build();
        c.registerEvent(new CorrectiveCaseCreatedEvent(
                null, caseCode, null, null, priority != null ? priority.name() : null));
        return c;
    }

    public static CorrectiveCase reconstruct(Builder builder) {
        return new CorrectiveCase(builder);
    }

    // ---- 状态转换方法 ----

    /**
     * 分配责任人 — OPEN → ASSIGNED
     */
    public void assign(Long assigneeId, String assigneeName) {
        if (this.status != CaseStatus.OPEN && this.status != CaseStatus.REJECTED) {
            throw new IllegalStateException("只有待分配或被驳回的案例才能分配责任人");
        }
        this.assigneeId = assigneeId;
        this.assigneeName = assigneeName;
        this.status = CaseStatus.ASSIGNED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new CaseAssignedEvent(this.id, this.caseCode, assigneeId, assigneeName));
    }

    /**
     * 开始整改 — ASSIGNED → IN_PROGRESS
     */
    public void startWork() {
        if (this.status != CaseStatus.ASSIGNED) {
            throw new IllegalStateException("只有已分配的案例才能开始整改");
        }
        this.status = CaseStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 提交整改 — IN_PROGRESS → SUBMITTED
     */
    public void submitCorrection(String correctionNote, List<Long> evidenceIds) {
        if (this.status != CaseStatus.IN_PROGRESS) {
            throw new IllegalStateException("只有进行中的案例才能提交整改");
        }
        this.correctionNote = correctionNote;
        this.correctionEvidenceIds = evidenceIds;
        this.correctedAt = LocalDateTime.now();
        this.status = CaseStatus.SUBMITTED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new CorrectionSubmittedEvent(this.id, this.caseCode, this.assigneeId));
    }

    /**
     * 验证通过 — SUBMITTED → VERIFIED
     */
    public void verify(Long verifierId, String verifierName, String note) {
        if (this.status != CaseStatus.SUBMITTED) {
            throw new IllegalStateException("只有已提交整改的案例才能验证");
        }
        this.verifierId = verifierId;
        this.verifierName = verifierName;
        this.verificationNote = note;
        this.verifiedAt = LocalDateTime.now();
        this.status = CaseStatus.VERIFIED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new CaseVerifiedEvent(this.id, this.caseCode, verifierId));
    }

    /**
     * 驳回 — SUBMITTED → REJECTED
     */
    public void reject(Long verifierId, String verifierName, String reason) {
        if (this.status != CaseStatus.SUBMITTED) {
            throw new IllegalStateException("只有已提交整改的案例才能驳回");
        }
        this.verifierId = verifierId;
        this.verifierName = verifierName;
        this.verificationNote = reason;
        this.verifiedAt = LocalDateTime.now();
        this.status = CaseStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new CaseRejectedEvent(this.id, this.caseCode, verifierId, reason));
    }

    /**
     * 关闭 — VERIFIED → CLOSED, 设置效果验证待检
     */
    public void close() {
        if (this.status != CaseStatus.VERIFIED) {
            throw new IllegalStateException("只有已验证的案例才能关闭");
        }
        this.status = CaseStatus.CLOSED;
        this.effectivenessStatus = EffectivenessStatus.PENDING;
        // 根据优先级设置效果验证日期 (CRITICAL=7天, HIGH=14天, MEDIUM=30天, LOW=60天)
        int checkDays = switch (this.priority) {
            case CRITICAL -> 7;
            case HIGH -> 14;
            case MEDIUM -> 30;
            case LOW -> 60;
        };
        this.effectivenessCheckDate = LocalDate.now().plusDays(checkDays);
        this.updatedAt = LocalDateTime.now();
        registerEvent(new CaseClosedEvent(this.id, this.caseCode, this.effectivenessCheckDate));
    }

    /**
     * 确认效果验证通过
     */
    public void confirmEffectiveness(String note) {
        if (this.status != CaseStatus.CLOSED || this.effectivenessStatus != EffectivenessStatus.PENDING) {
            throw new IllegalStateException("只有已关闭且待效果验证的案例才能确认效果");
        }
        this.effectivenessStatus = EffectivenessStatus.CONFIRMED;
        this.effectivenessNote = note;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new EffectivenessConfirmedEvent(this.id, this.caseCode));
    }

    /**
     * 效果验证不达标 — 重新打开案例
     */
    public void failEffectiveness(String note) {
        if (this.status != CaseStatus.CLOSED || this.effectivenessStatus != EffectivenessStatus.PENDING) {
            throw new IllegalStateException("只有已关闭且待效果验证的案例才能标记效果不达标");
        }
        this.effectivenessStatus = EffectivenessStatus.FAILED;
        this.effectivenessNote = note;
        // 重新打开，升级
        this.status = CaseStatus.OPEN;
        this.escalationLevel = this.escalationLevel + 1;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new EffectivenessFailedEvent(this.id, this.caseCode, this.escalationLevel));
    }

    /**
     * 设置根因分析
     */
    public void setRootCauseAnalysis(RcaMethod method, String rcaData) {
        this.rcaMethod = method;
        this.rcaData = rcaData;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置预防措施
     */
    public void setPreventiveAction(String preventiveAction) {
        this.preventiveAction = preventiveAction;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 升级 — 任何非终态都可升级
     */
    public void escalate() {
        if (this.status == CaseStatus.CLOSED || this.status == CaseStatus.ESCALATED) {
            throw new IllegalStateException("已关闭或已升级的案例不能再升级");
        }
        this.escalationLevel = this.escalationLevel + 1;
        this.status = CaseStatus.ESCALATED;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new CaseEscalatedEvent(this.id, this.caseCode, this.escalationLevel));
    }

    /**
     * SLA 超时自动升级
     */
    public void slaBreach() {
        this.escalationLevel = this.escalationLevel + 1;
        this.updatedAt = LocalDateTime.now();
        registerEvent(new SlaBreachedEvent(this.id, this.caseCode, this.escalationLevel, this.deadline));
    }

    /**
     * 判断是否逾期
     */
    public boolean isOverdue() {
        if (this.deadline == null) return false;
        if (this.status == CaseStatus.CLOSED || this.status == CaseStatus.VERIFIED) return false;
        return LocalDateTime.now().isAfter(this.deadline);
    }

    // Getters
    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public String getCaseCode() { return caseCode; }
    public Long getSubmissionId() { return submissionId; }
    public Long getDetailId() { return detailId; }
    public Long getProjectId() { return projectId; }
    public Long getTaskId() { return taskId; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
    public String getIssueDescription() { return issueDescription; }
    public String getRequiredAction() { return requiredAction; }
    public Long getIssueCategoryId() { return issueCategoryId; }
    public String getDeficiencyCode() { return deficiencyCode; }
    public RcaMethod getRcaMethod() { return rcaMethod; }
    public String getRcaData() { return rcaData; }
    public String getPreventiveAction() { return preventiveAction; }
    public CasePriority getPriority() { return priority; }
    public LocalDateTime getDeadline() { return deadline; }
    public Long getAssigneeId() { return assigneeId; }
    public String getAssigneeName() { return assigneeName; }
    public Integer getEscalationLevel() { return escalationLevel; }
    public CaseStatus getStatus() { return status; }
    public String getCorrectionNote() { return correctionNote; }
    public List<Long> getCorrectionEvidenceIds() { return correctionEvidenceIds; }
    public LocalDateTime getCorrectedAt() { return correctedAt; }
    public Long getVerifierId() { return verifierId; }
    public String getVerifierName() { return verifierName; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public String getVerificationNote() { return verificationNote; }
    public LocalDate getEffectivenessCheckDate() { return effectivenessCheckDate; }
    public EffectivenessStatus getEffectivenessStatus() { return effectivenessStatus; }
    public String getEffectivenessNote() { return effectivenessNote; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private String caseCode;
        private Long submissionId;
        private Long detailId;
        private Long projectId;
        private Long taskId;
        private String targetType;
        private Long targetId;
        private String targetName;
        private String issueDescription;
        private String requiredAction;
        private Long issueCategoryId;
        private String deficiencyCode;
        private RcaMethod rcaMethod;
        private String rcaData;
        private String preventiveAction;
        private CasePriority priority;
        private LocalDateTime deadline;
        private Long assigneeId;
        private String assigneeName;
        private Integer escalationLevel;
        private CaseStatus status;
        private String correctionNote;
        private List<Long> correctionEvidenceIds;
        private LocalDateTime correctedAt;
        private Long verifierId;
        private String verifierName;
        private LocalDateTime verifiedAt;
        private String verificationNote;
        private LocalDate effectivenessCheckDate;
        private EffectivenessStatus effectivenessStatus;
        private String effectivenessNote;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder caseCode(String caseCode) { this.caseCode = caseCode; return this; }
        public Builder submissionId(Long submissionId) { this.submissionId = submissionId; return this; }
        public Builder detailId(Long detailId) { this.detailId = detailId; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder taskId(Long taskId) { this.taskId = taskId; return this; }
        public Builder targetType(String targetType) { this.targetType = targetType; return this; }
        public Builder targetId(Long targetId) { this.targetId = targetId; return this; }
        public Builder targetName(String targetName) { this.targetName = targetName; return this; }
        public Builder issueDescription(String issueDescription) { this.issueDescription = issueDescription; return this; }
        public Builder requiredAction(String requiredAction) { this.requiredAction = requiredAction; return this; }
        public Builder issueCategoryId(Long issueCategoryId) { this.issueCategoryId = issueCategoryId; return this; }
        public Builder deficiencyCode(String deficiencyCode) { this.deficiencyCode = deficiencyCode; return this; }
        public Builder rcaMethod(RcaMethod rcaMethod) { this.rcaMethod = rcaMethod; return this; }
        public Builder rcaData(String rcaData) { this.rcaData = rcaData; return this; }
        public Builder preventiveAction(String preventiveAction) { this.preventiveAction = preventiveAction; return this; }
        public Builder priority(CasePriority priority) { this.priority = priority; return this; }
        public Builder deadline(LocalDateTime deadline) { this.deadline = deadline; return this; }
        public Builder assigneeId(Long assigneeId) { this.assigneeId = assigneeId; return this; }
        public Builder assigneeName(String assigneeName) { this.assigneeName = assigneeName; return this; }
        public Builder escalationLevel(Integer escalationLevel) { this.escalationLevel = escalationLevel; return this; }
        public Builder status(CaseStatus status) { this.status = status; return this; }
        public Builder correctionNote(String correctionNote) { this.correctionNote = correctionNote; return this; }
        public Builder correctionEvidenceIds(List<Long> correctionEvidenceIds) { this.correctionEvidenceIds = correctionEvidenceIds; return this; }
        public Builder correctedAt(LocalDateTime correctedAt) { this.correctedAt = correctedAt; return this; }
        public Builder verifierId(Long verifierId) { this.verifierId = verifierId; return this; }
        public Builder verifierName(String verifierName) { this.verifierName = verifierName; return this; }
        public Builder verifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; return this; }
        public Builder verificationNote(String verificationNote) { this.verificationNote = verificationNote; return this; }
        public Builder effectivenessCheckDate(LocalDate effectivenessCheckDate) { this.effectivenessCheckDate = effectivenessCheckDate; return this; }
        public Builder effectivenessStatus(EffectivenessStatus effectivenessStatus) { this.effectivenessStatus = effectivenessStatus; return this; }
        public Builder effectivenessNote(String effectivenessNote) { this.effectivenessNote = effectivenessNote; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public CorrectiveCase build() { return new CorrectiveCase(this); }
    }
}
