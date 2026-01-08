package com.school.management.domain.task.model;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.task.event.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Task aggregate root.
 *
 * <p>Represents a task that can be assigned to one or more users.
 * Supports both individual assignment and batch assignment scenarios.
 *
 * <p>State machine:
 * <pre>
 *     PENDING_ACCEPT → IN_PROGRESS → SUBMITTED → APPROVED → COMPLETED
 *                  ↘               ↘          ↘
 *                   CANCELLED      REJECTED    REJECTED
 * </pre>
 *
 * <p>For batch tasks, the overall task status reflects the aggregate progress
 * of all assignees.
 */
public class Task extends AggregateRoot<Long> {

    private static final AtomicInteger DAILY_SEQUENCE = new AtomicInteger(0);
    private static String LAST_DATE = "";

    private String taskCode;
    private String title;
    private String description;
    private TaskPriority priority;
    private Long assignerId;
    private String assignerName;
    private AssignmentType assignmentType;
    private Long departmentId;
    private LocalDateTime dueDate;
    private TaskStatus status;
    private List<Assignee> assignees;
    private Long workflowTemplateId;
    private String processInstanceId;
    private String currentNode;
    private List<Long> currentApprovers;
    private List<Long> attachmentIds;
    private LocalDateTime acceptedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private String cancelReason;
    private LocalDateTime cancelledAt;
    private Long cancelledBy;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;

    /**
     * Assignment type enumeration.
     */
    public enum AssignmentType {
        /**
         * Assigned to a specific individual.
         */
        INDIVIDUAL(1, "指定个人"),

        /**
         * Batch assignment to multiple users.
         */
        BATCH(2, "批量分配");

        private final int value;
        private final String displayName;

        AssignmentType(int value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public int getValue() {
            return value;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static AssignmentType fromValue(Integer value) {
            if (value == null) return INDIVIDUAL;
            for (AssignmentType type : values()) {
                if (type.value == value) return type;
            }
            return INDIVIDUAL;
        }
    }

    protected Task() {
        this.assignees = new ArrayList<>();
        this.currentApprovers = new ArrayList<>();
        this.attachmentIds = new ArrayList<>();
    }

    /**
     * Creates a new task.
     *
     * @param title         task title
     * @param description   task description
     * @param priority      task priority
     * @param assignerId    assigner user ID
     * @param assignerName  assigner name
     * @param departmentId  department ID
     * @param dueDate       due date
     * @param workflowTemplateId workflow template ID (optional)
     * @param attachmentIds initial attachments
     * @param createdBy     creator user ID
     * @return new Task instance
     */
    public static Task create(String title, String description, TaskPriority priority,
                               Long assignerId, String assignerName, Long departmentId,
                               LocalDateTime dueDate, Long workflowTemplateId,
                               List<Long> attachmentIds, Long createdBy) {
        Task task = new Task();
        task.taskCode = generateTaskCode();
        task.title = Objects.requireNonNull(title, "Title is required");
        task.description = description;
        task.priority = priority != null ? priority : TaskPriority.NORMAL;
        task.assignerId = assignerId;
        task.assignerName = assignerName;
        task.departmentId = departmentId;
        task.dueDate = dueDate;
        task.workflowTemplateId = workflowTemplateId;
        task.attachmentIds = attachmentIds != null ? new ArrayList<>(attachmentIds) : new ArrayList<>();
        task.status = TaskStatus.PENDING_ACCEPT;
        task.createdAt = LocalDateTime.now();
        task.createdBy = createdBy;
        task.updatedAt = task.createdAt;
        task.updatedBy = createdBy;

        // Register domain event
        task.registerEvent(TaskCreatedEvent.of(
            task.getId(), task.taskCode, task.title, task.assignerId, task.assignerName));

        return task;
    }

    /**
     * Assigns the task to a single user.
     *
     * @param userId         assignee user ID
     * @param userName       assignee name
     * @param departmentId   department ID
     * @param departmentName department name
     * @return the created assignee
     */
    public Assignee assignToUser(Long userId, String userName,
                                  Long departmentId, String departmentName) {
        this.assignmentType = AssignmentType.INDIVIDUAL;

        Assignee assignee = Assignee.create(getId(), userId, userName, departmentId, departmentName);
        this.assignees.add(assignee);

        // Register assignment event
        registerEvent(TaskAssignedEvent.of(getId(), taskCode, userId, userName));

        return assignee;
    }

    /**
     * Assigns the task to multiple users (batch assignment).
     *
     * @param userAssignments list of user assignment details
     * @return list of created assignees
     */
    public List<Assignee> assignToUsers(List<UserAssignment> userAssignments) {
        this.assignmentType = AssignmentType.BATCH;

        List<Assignee> newAssignees = new ArrayList<>();
        for (UserAssignment ua : userAssignments) {
            Assignee assignee = Assignee.create(
                getId(), ua.userId(), ua.userName(),
                ua.departmentId(), ua.departmentName());
            this.assignees.add(assignee);
            newAssignees.add(assignee);

            // Register assignment event for each user
            registerEvent(TaskAssignedEvent.of(getId(), taskCode, ua.userId(), ua.userName()));
        }

        return newAssignees;
    }

    /**
     * User assignment details record.
     */
    public record UserAssignment(
        Long userId,
        String userName,
        Long departmentId,
        String departmentName
    ) {}

    /**
     * Accepts the task by a specific assignee.
     *
     * @param userId   the accepting user's ID
     * @param userName the accepting user's name
     */
    public void accept(Long userId, String userName) {
        Assignee assignee = findAssigneeByUserId(userId);
        if (assignee == null) {
            throw new IllegalArgumentException("User " + userId + " is not assigned to this task");
        }

        assignee.accept();

        // Update overall task status if this is the first acceptance
        if (status == TaskStatus.PENDING_ACCEPT) {
            this.status = TaskStatus.IN_PROGRESS;
            this.acceptedAt = LocalDateTime.now();
        }

        updateTimestamp(userId);

        registerEvent(TaskAcceptedEvent.of(getId(), taskCode, userId, userName));
    }

    /**
     * Submits work for a specific assignee.
     *
     * @param userId        submitter user ID
     * @param userName      submitter name
     * @param content       submission content
     * @param attachmentIds attachment IDs
     * @return the created submission
     */
    public Submission submit(Long userId, String userName, String content,
                             List<Long> attachmentIds) {
        Assignee assignee = findAssigneeByUserId(userId);
        if (assignee == null) {
            throw new IllegalArgumentException("User " + userId + " is not assigned to this task");
        }

        Submission submission = assignee.submit(content, attachmentIds, userId, userName);

        // Update overall task status if all assignees have submitted
        updateOverallStatus();

        updateTimestamp(userId);

        registerEvent(TaskSubmittedEvent.of(getId(), taskCode, userId, userName, submission.getId()));

        return submission;
    }

    /**
     * Approves a submission for a specific assignee.
     *
     * @param assigneeUserId the assignee's user ID
     * @param reviewerId     reviewer user ID
     * @param reviewerName   reviewer name
     * @param comment        approval comment
     */
    public void approve(Long assigneeUserId, Long reviewerId, String reviewerName, String comment) {
        Assignee assignee = findAssigneeByUserId(assigneeUserId);
        if (assignee == null) {
            throw new IllegalArgumentException("User " + assigneeUserId + " is not assigned to this task");
        }

        assignee.approve(reviewerId, reviewerName, comment);

        // Update overall task status
        updateOverallStatus();

        updateTimestamp(reviewerId);

        registerEvent(TaskApprovedEvent.of(getId(), taskCode, assigneeUserId, reviewerId, reviewerName));
    }

    /**
     * Rejects a submission for a specific assignee.
     *
     * @param assigneeUserId the assignee's user ID
     * @param reviewerId     reviewer user ID
     * @param reviewerName   reviewer name
     * @param reason         rejection reason
     */
    public void reject(Long assigneeUserId, Long reviewerId, String reviewerName, String reason) {
        Assignee assignee = findAssigneeByUserId(assigneeUserId);
        if (assignee == null) {
            throw new IllegalArgumentException("User " + assigneeUserId + " is not assigned to this task");
        }

        assignee.reject(reviewerId, reviewerName, reason);

        // Update overall task status
        if (assignmentType == AssignmentType.INDIVIDUAL) {
            this.status = TaskStatus.REJECTED;
        }

        updateTimestamp(reviewerId);

        registerEvent(TaskRejectedEvent.of(getId(), taskCode, assigneeUserId, reviewerId, reviewerName, reason));
    }

    /**
     * Cancels the task.
     *
     * @param reason      cancellation reason
     * @param cancelledBy user who cancelled
     */
    public void cancel(String reason, Long cancelledBy) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot cancel task in terminal status: " + status);
        }

        this.status = TaskStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
        this.cancelledBy = cancelledBy;

        updateTimestamp(cancelledBy);

        registerEvent(TaskCancelledEvent.of(getId(), taskCode, reason, cancelledBy));
    }

    /**
     * Updates the overall task status based on assignee statuses.
     */
    private void updateOverallStatus() {
        if (assignees.isEmpty()) return;

        boolean allCompleted = assignees.stream().allMatch(Assignee::isCompleted);
        boolean anySubmitted = assignees.stream()
            .anyMatch(a -> a.getStatus() == AssigneeStatus.PENDING_REVIEW);
        boolean anyInProgress = assignees.stream()
            .anyMatch(a -> a.getStatus() == AssigneeStatus.IN_PROGRESS);

        if (allCompleted) {
            this.status = TaskStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        } else if (anySubmitted) {
            this.status = TaskStatus.SUBMITTED;
            if (this.submittedAt == null) {
                this.submittedAt = LocalDateTime.now();
            }
        } else if (anyInProgress) {
            this.status = TaskStatus.IN_PROGRESS;
        }
    }

    /**
     * Finds an assignee by user ID.
     *
     * @param userId the user ID
     * @return the assignee or null
     */
    private Assignee findAssigneeByUserId(Long userId) {
        return assignees.stream()
            .filter(a -> Objects.equals(a.getUserId(), userId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Updates the modification timestamp.
     */
    private void updateTimestamp(Long userId) {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = userId;
    }

    /**
     * Generates a unique task code.
     */
    private static synchronized String generateTaskCode() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if (!today.equals(LAST_DATE)) {
            LAST_DATE = today;
            DAILY_SEQUENCE.set(0);
        }
        int seq = DAILY_SEQUENCE.incrementAndGet();
        return String.format("TASK-%s-%04d", today, seq);
    }

    // Query methods

    /**
     * Checks if the task is overdue.
     *
     * @return true if overdue
     */
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && !status.isTerminal();
    }

    /**
     * Gets the completion percentage.
     *
     * @return completion percentage (0-100)
     */
    public int getCompletionPercentage() {
        if (assignees.isEmpty()) return 0;
        long completed = assignees.stream().filter(Assignee::isCompleted).count();
        return (int) (completed * 100 / assignees.size());
    }

    /**
     * Gets the number of assignees.
     *
     * @return assignee count
     */
    public int getAssigneeCount() {
        return assignees.size();
    }

    /**
     * Gets the number of completed assignees.
     *
     * @return completed count
     */
    public int getCompletedAssigneeCount() {
        return (int) assignees.stream().filter(Assignee::isCompleted).count();
    }

    /**
     * Checks if a user is an assignee of this task.
     *
     * @param userId the user ID
     * @return true if user is an assignee
     */
    public boolean hasAssignee(Long userId) {
        return findAssigneeByUserId(userId) != null;
    }

    /**
     * Gets the assignee for a specific user.
     *
     * @param userId the user ID
     * @return the assignee or empty optional
     */
    public Optional<Assignee> getAssignee(Long userId) {
        return Optional.ofNullable(findAssigneeByUserId(userId));
    }

    // Getters

    public String getTaskCode() {
        return taskCode;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public Long getAssignerId() {
        return assignerId;
    }

    public String getAssignerName() {
        return assignerName;
    }

    public AssignmentType getAssignmentType() {
        return assignmentType;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public List<Assignee> getAssignees() {
        return Collections.unmodifiableList(assignees);
    }

    public Long getWorkflowTemplateId() {
        return workflowTemplateId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public List<Long> getCurrentApprovers() {
        return Collections.unmodifiableList(currentApprovers);
    }

    public List<Long> getAttachmentIds() {
        return Collections.unmodifiableList(attachmentIds);
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public Long getCancelledBy() {
        return cancelledBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    // Builder pattern for reconstruction from persistence

    public static class Builder {
        private final Task task = new Task();

        public Builder id(Long id) {
            task.setId(id);
            return this;
        }

        public Builder taskCode(String taskCode) {
            task.taskCode = taskCode;
            return this;
        }

        public Builder title(String title) {
            task.title = title;
            return this;
        }

        public Builder description(String description) {
            task.description = description;
            return this;
        }

        public Builder priority(TaskPriority priority) {
            task.priority = priority;
            return this;
        }

        public Builder assignerId(Long assignerId) {
            task.assignerId = assignerId;
            return this;
        }

        public Builder assignerName(String assignerName) {
            task.assignerName = assignerName;
            return this;
        }

        public Builder assignmentType(AssignmentType assignmentType) {
            task.assignmentType = assignmentType;
            return this;
        }

        public Builder departmentId(Long departmentId) {
            task.departmentId = departmentId;
            return this;
        }

        public Builder dueDate(LocalDateTime dueDate) {
            task.dueDate = dueDate;
            return this;
        }

        public Builder status(TaskStatus status) {
            task.status = status;
            return this;
        }

        public Builder assignees(List<Assignee> assignees) {
            task.assignees = new ArrayList<>(assignees);
            return this;
        }

        public Builder workflowTemplateId(Long workflowTemplateId) {
            task.workflowTemplateId = workflowTemplateId;
            return this;
        }

        public Builder processInstanceId(String processInstanceId) {
            task.processInstanceId = processInstanceId;
            return this;
        }

        public Builder currentNode(String currentNode) {
            task.currentNode = currentNode;
            return this;
        }

        public Builder currentApprovers(List<Long> currentApprovers) {
            task.currentApprovers = currentApprovers != null ? new ArrayList<>(currentApprovers) : new ArrayList<>();
            return this;
        }

        public Builder attachmentIds(List<Long> attachmentIds) {
            task.attachmentIds = attachmentIds != null ? new ArrayList<>(attachmentIds) : new ArrayList<>();
            return this;
        }

        public Builder acceptedAt(LocalDateTime acceptedAt) {
            task.acceptedAt = acceptedAt;
            return this;
        }

        public Builder submittedAt(LocalDateTime submittedAt) {
            task.submittedAt = submittedAt;
            return this;
        }

        public Builder completedAt(LocalDateTime completedAt) {
            task.completedAt = completedAt;
            return this;
        }

        public Builder cancelReason(String cancelReason) {
            task.cancelReason = cancelReason;
            return this;
        }

        public Builder cancelledAt(LocalDateTime cancelledAt) {
            task.cancelledAt = cancelledAt;
            return this;
        }

        public Builder cancelledBy(Long cancelledBy) {
            task.cancelledBy = cancelledBy;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            task.createdAt = createdAt;
            return this;
        }

        public Builder createdBy(Long createdBy) {
            task.createdBy = createdBy;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            task.updatedAt = updatedAt;
            return this;
        }

        public Builder updatedBy(Long updatedBy) {
            task.updatedBy = updatedBy;
            return this;
        }

        public Builder version(Long version) {
            task.setVersion(version);
            return this;
        }

        public Task build() {
            return task;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
