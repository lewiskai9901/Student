package com.school.management.domain.task.event;

/**
 * Event published when a task submission is made.
 */
public class TaskSubmittedEvent extends TaskDomainEvent {

    private final Long submittedBy;
    private final String submittedByName;
    private final Long submissionId;

    private TaskSubmittedEvent(Long taskId, String taskCode, Long submittedBy, 
                               String submittedByName, Long submissionId) {
        super(taskId, taskCode);
        this.submittedBy = submittedBy;
        this.submittedByName = submittedByName;
        this.submissionId = submissionId;
    }

    public static TaskSubmittedEvent of(Long taskId, String taskCode, Long submittedBy, 
                                         String submittedByName, Long submissionId) {
        return new TaskSubmittedEvent(taskId, taskCode, submittedBy, submittedByName, submissionId);
    }

    @Override
    public String getEventType() {
        return "task.submitted";
    }

    public Long getSubmittedBy() {
        return submittedBy;
    }

    public String getSubmittedByName() {
        return submittedByName;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    /**
     * Alias for getSubmittedBy for compatibility.
     */
    public Long getSubmitterId() {
        return submittedBy;
    }

    /**
     * Alias for getSubmittedByName for compatibility.
     */
    public String getSubmitterName() {
        return submittedByName;
    }
}
