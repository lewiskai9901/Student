package com.school.management.domain.behavior.model;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * BehaviorAlert Entity.
 * Represents an alert triggered by student behavior patterns (frequency, severity, trends).
 */
public class BehaviorAlert extends Entity<Long> {

    private Long id;
    private Long studentId;
    private Long classId;
    private AlertType alertType;
    private String alertLevel;
    private String title;
    private String description;
    private String triggerData;
    private boolean isRead;
    private boolean isHandled;
    private Long handledBy;
    private LocalDateTime handledAt;
    private String handleNote;
    private LocalDateTime createdAt;

    // For JPA/MyBatis
    protected BehaviorAlert() {
    }

    private BehaviorAlert(Builder builder) {
        this.id = builder.id;
        this.studentId = builder.studentId;
        this.classId = builder.classId;
        this.alertType = builder.alertType;
        this.alertLevel = builder.alertLevel;
        this.title = builder.title;
        this.description = builder.description;
        this.triggerData = builder.triggerData;
        this.isRead = builder.isRead;
        this.isHandled = builder.isHandled;
        this.handledBy = builder.handledBy;
        this.handledAt = builder.handledAt;
        this.handleNote = builder.handleNote;
        this.createdAt = builder.createdAt;
    }

    /**
     * Marks the alert as read.
     */
    public void markRead() {
        this.isRead = true;
    }

    /**
     * Handles the alert with the given handler and note.
     */
    public void handle(Long handledBy, String note) {
        this.isHandled = true;
        this.handledBy = handledBy;
        this.handledAt = LocalDateTime.now();
        this.handleNote = note;
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getClassId() {
        return classId;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTriggerData() {
        return triggerData;
    }

    public boolean isRead() {
        return isRead;
    }

    public boolean isHandled() {
        return isHandled;
    }

    public Long getHandledBy() {
        return handledBy;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Alias for builder(), used when reconstructing from persistence.
     */
    public static Builder reconstruct() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long studentId;
        private Long classId;
        private AlertType alertType;
        private String alertLevel;
        private String title;
        private String description;
        private String triggerData;
        private boolean isRead;
        private boolean isHandled;
        private Long handledBy;
        private LocalDateTime handledAt;
        private String handleNote;
        private LocalDateTime createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder studentId(Long studentId) {
            this.studentId = studentId;
            return this;
        }

        public Builder classId(Long classId) {
            this.classId = classId;
            return this;
        }

        public Builder alertType(AlertType alertType) {
            this.alertType = alertType;
            return this;
        }

        public Builder alertLevel(String alertLevel) {
            this.alertLevel = alertLevel;
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

        public Builder triggerData(String triggerData) {
            this.triggerData = triggerData;
            return this;
        }

        public Builder isRead(boolean isRead) {
            this.isRead = isRead;
            return this;
        }

        public Builder isHandled(boolean isHandled) {
            this.isHandled = isHandled;
            return this;
        }

        public Builder handledBy(Long handledBy) {
            this.handledBy = handledBy;
            return this;
        }

        public Builder handledAt(LocalDateTime handledAt) {
            this.handledAt = handledAt;
            return this;
        }

        public Builder handleNote(String handleNote) {
            this.handleNote = handleNote;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public BehaviorAlert build() {
            return new BehaviorAlert(this);
        }
    }
}
