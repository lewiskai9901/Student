package com.school.management.domain.corrective.model;

import com.school.management.domain.corrective.event.ActionCreatedEvent;
import com.school.management.domain.corrective.event.ActionEscalatedEvent;
import com.school.management.domain.corrective.event.ActionOverdueEvent;
import com.school.management.domain.corrective.event.ActionStatusChangedEvent;
import com.school.management.domain.corrective.event.ActionVerifiedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CorrectiveAction Aggregate Root.
 * Represents a corrective action with state machine workflow.
 */
public class CorrectiveAction extends AggregateRoot<Long> {

    private Long id;
    private String actionCode;
    private String title;
    private String description;
    private ActionSource source;
    private Long sourceId;
    private ActionSeverity severity;
    private ActionCategory category;
    private ActionStatus status;
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

    // For JPA/MyBatis
    protected CorrectiveAction() {
        this.resolutionAttachments = new ArrayList<>();
    }

    private CorrectiveAction(Builder builder) {
        this.id = builder.id;
        this.actionCode = builder.actionCode;
        this.title = builder.title;
        this.description = builder.description;
        this.source = builder.source;
        this.sourceId = builder.sourceId;
        this.severity = builder.severity;
        this.category = builder.category;
        this.status = ActionStatus.OPEN;
        this.classId = builder.classId;
        this.assigneeId = builder.assigneeId;
        this.deadline = builder.deadline;
        this.resolutionAttachments = new ArrayList<>();
        this.escalationLevel = 0;
        this.createdBy = builder.createdBy;
        this.createdAt = LocalDateTime.now();

        validate();
    }

    private CorrectiveAction(Reconstructor r) {
        this.id = r.id;
        this.actionCode = r.actionCode;
        this.title = r.title;
        this.description = r.description;
        this.source = r.source;
        this.sourceId = r.sourceId;
        this.severity = r.severity;
        this.category = r.category;
        this.status = r.status;
        this.classId = r.classId;
        this.assigneeId = r.assigneeId;
        this.deadline = r.deadline;
        this.resolutionNote = r.resolutionNote;
        this.resolutionAttachments = r.resolutionAttachments != null
            ? new ArrayList<>(r.resolutionAttachments) : new ArrayList<>();
        this.resolvedAt = r.resolvedAt;
        this.verifierId = r.verifierId;
        this.verificationResult = r.verificationResult;
        this.verificationComment = r.verificationComment;
        this.verifiedAt = r.verifiedAt;
        this.escalationLevel = r.escalationLevel;
        this.createdBy = r.createdBy;
        this.createdAt = r.createdAt;
    }

    /**
     * Factory method to create a new corrective action.
     */
    public static CorrectiveAction create(String actionCode, String title, String description,
                                           ActionSource source, Long sourceId,
                                           ActionSeverity severity, ActionCategory category,
                                           Long classId, Long assigneeId,
                                           LocalDateTime deadline, Long createdBy) {
        CorrectiveAction action = builder()
            .actionCode(actionCode)
            .title(title)
            .description(description)
            .source(source)
            .sourceId(sourceId)
            .severity(severity)
            .category(category)
            .classId(classId)
            .assigneeId(assigneeId)
            .deadline(deadline)
            .createdBy(createdBy)
            .build();

        action.registerEvent(new ActionCreatedEvent(action));
        return action;
    }

    /**
     * Starts progress on this action.
     * Transitions from OPEN to IN_PROGRESS.
     */
    public void startProgress() {
        assertCanTransitionTo(ActionStatus.IN_PROGRESS);
        ActionStatus oldStatus = this.status;
        this.status = ActionStatus.IN_PROGRESS;

        registerEvent(new ActionStatusChangedEvent(this, oldStatus, this.status));
    }

    /**
     * Resolves this action by submitting it for review.
     * Transitions from IN_PROGRESS to REVIEW.
     */
    public void resolve(String note, List<String> attachments) {
        assertCanTransitionTo(ActionStatus.REVIEW);
        ActionStatus oldStatus = this.status;
        this.status = ActionStatus.REVIEW;
        this.resolutionNote = note;
        this.resolutionAttachments = attachments != null
            ? new ArrayList<>(attachments)
            : new ArrayList<>();
        this.resolvedAt = LocalDateTime.now();

        registerEvent(new ActionStatusChangedEvent(this, oldStatus, this.status));
    }

    /**
     * Verifies this action after review.
     * If result is "PASS", transitions REVIEW to CLOSED.
     * If result is "FAIL", transitions REVIEW back to IN_PROGRESS.
     */
    public void verify(Long verifierId, String result, String comment) {
        if ("PASS".equals(result)) {
            assertCanTransitionTo(ActionStatus.CLOSED);
            ActionStatus oldStatus = this.status;
            this.verifierId = verifierId;
            this.verificationResult = result;
            this.verificationComment = comment;
            this.verifiedAt = LocalDateTime.now();
            this.status = ActionStatus.CLOSED;

            registerEvent(new ActionVerifiedEvent(this, oldStatus, this.status, verifierId, result));
        } else if ("FAIL".equals(result)) {
            assertCanTransitionTo(ActionStatus.IN_PROGRESS);
            ActionStatus oldStatus = this.status;
            this.verifierId = verifierId;
            this.verificationResult = result;
            this.verificationComment = comment;
            this.verifiedAt = LocalDateTime.now();
            this.status = ActionStatus.IN_PROGRESS;

            registerEvent(new ActionVerifiedEvent(this, oldStatus, this.status, verifierId, result));
        } else {
            throw new IllegalArgumentException("Verification result must be PASS or FAIL");
        }
    }

    /**
     * Marks this action as overdue.
     * Transitions from IN_PROGRESS to OVERDUE.
     */
    public void markOverdue() {
        assertCanTransitionTo(ActionStatus.OVERDUE);
        ActionStatus oldStatus = this.status;
        this.status = ActionStatus.OVERDUE;

        registerEvent(new ActionOverdueEvent(this));
    }

    /**
     * Escalates this overdue action.
     * Transitions from OVERDUE to ESCALATED, increments escalation level.
     */
    public void escalate() {
        assertCanTransitionTo(ActionStatus.ESCALATED);
        ActionStatus oldStatus = this.status;
        this.status = ActionStatus.ESCALATED;
        this.escalationLevel++;

        registerEvent(new ActionEscalatedEvent(this, this.escalationLevel));
    }

    private void assertCanTransitionTo(ActionStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", status, target));
        }
    }

    private void validate() {
        if (actionCode == null || actionCode.isBlank()) {
            throw new IllegalArgumentException("Action code is required");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (source == null) {
            throw new IllegalArgumentException("Action source is required");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Severity is required");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category is required");
        }
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActionCode() {
        return actionCode;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ActionSource getSource() {
        return source;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public ActionSeverity getSeverity() {
        return severity;
    }

    public ActionCategory getCategory() {
        return category;
    }

    public ActionStatus getStatus() {
        return status;
    }

    public Long getClassId() {
        return classId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public List<String> getResolutionAttachments() {
        return Collections.unmodifiableList(resolutionAttachments);
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public Long getVerifierId() {
        return verifierId;
    }

    public String getVerificationResult() {
        return verificationResult;
    }

    public String getVerificationComment() {
        return verificationComment;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public int getEscalationLevel() {
        return escalationLevel;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Builder (for creation)
    public static Builder builder() {
        return new Builder();
    }

    // Reconstructor (for restoring from persistence)
    public static Reconstructor reconstruct() {
        return new Reconstructor();
    }

    public static class Builder {
        private Long id;
        private String actionCode;
        private String title;
        private String description;
        private ActionSource source;
        private Long sourceId;
        private ActionSeverity severity;
        private ActionCategory category;
        private Long classId;
        private Long assigneeId;
        private LocalDateTime deadline;
        private Long createdBy;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder actionCode(String actionCode) {
            this.actionCode = actionCode;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder source(ActionSource source) {
            this.source = source;
            return this;
        }

        public Builder sourceId(Long sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder severity(ActionSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder category(ActionCategory category) {
            this.category = category;
            return this;
        }

        public Builder classId(Long classId) {
            this.classId = classId;
            return this;
        }

        public Builder assigneeId(Long assigneeId) {
            this.assigneeId = assigneeId;
            return this;
        }

        public Builder deadline(LocalDateTime deadline) {
            this.deadline = deadline;
            return this;
        }

        public Builder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public CorrectiveAction build() {
            return new CorrectiveAction(this);
        }
    }

    public static class Reconstructor {
        private Long id;
        private String actionCode;
        private String title;
        private String description;
        private ActionSource source;
        private Long sourceId;
        private ActionSeverity severity;
        private ActionCategory category;
        private ActionStatus status;
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

        public Reconstructor id(Long id) { this.id = id; return this; }
        public Reconstructor actionCode(String actionCode) { this.actionCode = actionCode; return this; }
        public Reconstructor title(String title) { this.title = title; return this; }
        public Reconstructor description(String description) { this.description = description; return this; }
        public Reconstructor source(ActionSource source) { this.source = source; return this; }
        public Reconstructor sourceId(Long sourceId) { this.sourceId = sourceId; return this; }
        public Reconstructor severity(ActionSeverity severity) { this.severity = severity; return this; }
        public Reconstructor category(ActionCategory category) { this.category = category; return this; }
        public Reconstructor status(ActionStatus status) { this.status = status; return this; }
        public Reconstructor classId(Long classId) { this.classId = classId; return this; }
        public Reconstructor assigneeId(Long assigneeId) { this.assigneeId = assigneeId; return this; }
        public Reconstructor deadline(LocalDateTime deadline) { this.deadline = deadline; return this; }
        public Reconstructor resolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; return this; }
        public Reconstructor resolutionAttachments(List<String> resolutionAttachments) { this.resolutionAttachments = resolutionAttachments; return this; }
        public Reconstructor resolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; return this; }
        public Reconstructor verifierId(Long verifierId) { this.verifierId = verifierId; return this; }
        public Reconstructor verificationResult(String verificationResult) { this.verificationResult = verificationResult; return this; }
        public Reconstructor verificationComment(String verificationComment) { this.verificationComment = verificationComment; return this; }
        public Reconstructor verifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; return this; }
        public Reconstructor escalationLevel(int escalationLevel) { this.escalationLevel = escalationLevel; return this; }
        public Reconstructor createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Reconstructor createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public CorrectiveAction build() {
            return new CorrectiveAction(this);
        }
    }
}
