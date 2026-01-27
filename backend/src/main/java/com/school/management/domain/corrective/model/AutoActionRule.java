package com.school.management.domain.corrective.model;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * AutoActionRule Entity.
 * Represents a rule for automatically creating corrective actions
 * based on certain trigger conditions.
 */
public class AutoActionRule extends Entity<Long> {

    private Long id;
    private String ruleCode;
    private String ruleName;
    private String triggerType;
    private String triggerCondition; // JSON
    private ActionSeverity severity;
    private ActionCategory category;
    private int deadlineHours;
    private boolean autoAssign;
    private boolean enabled;
    private LocalDateTime createdAt;

    // For JPA/MyBatis
    protected AutoActionRule() {
    }

    private AutoActionRule(Builder builder) {
        this.id = builder.id;
        this.ruleCode = builder.ruleCode;
        this.ruleName = builder.ruleName;
        this.triggerType = builder.triggerType;
        this.triggerCondition = builder.triggerCondition;
        this.severity = builder.severity;
        this.category = builder.category;
        this.deadlineHours = builder.deadlineHours;
        this.autoAssign = builder.autoAssign;
        this.enabled = builder.enabled;
        this.createdAt = LocalDateTime.now();
    }

    private AutoActionRule(Reconstructor r) {
        this.id = r.id;
        this.ruleCode = r.ruleCode;
        this.ruleName = r.ruleName;
        this.triggerType = r.triggerType;
        this.triggerCondition = r.triggerCondition;
        this.severity = r.severity;
        this.category = r.category;
        this.deadlineHours = r.deadlineHours;
        this.autoAssign = r.autoAssign;
        this.enabled = r.enabled;
        this.createdAt = r.createdAt;
    }

    /**
     * Simplified evaluation that checks if triggerType matches eventType.
     *
     * @param eventType the type of the event to evaluate
     * @param eventData the data of the event (reserved for future use)
     * @return true if this rule should trigger for the given event
     */
    public boolean evaluate(String eventType, String eventData) {
        if (!enabled) {
            return false;
        }
        return triggerType != null && triggerType.equals(eventType);
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public String getTriggerCondition() {
        return triggerCondition;
    }

    public ActionSeverity getSeverity() {
        return severity;
    }

    public ActionCategory getCategory() {
        return category;
    }

    public int getDeadlineHours() {
        return deadlineHours;
    }

    public boolean isAutoAssign() {
        return autoAssign;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Reconstructor alias for restoring from persistence (skips createdAt = now()).
     */
    public static Reconstructor reconstruct() {
        return new Reconstructor();
    }

    public static class Builder {
        private Long id;
        private String ruleCode;
        private String ruleName;
        private String triggerType;
        private String triggerCondition;
        private ActionSeverity severity;
        private ActionCategory category;
        private int deadlineHours;
        private boolean autoAssign;
        private boolean enabled;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder ruleCode(String ruleCode) {
            this.ruleCode = ruleCode;
            return this;
        }

        public Builder ruleName(String ruleName) {
            this.ruleName = ruleName;
            return this;
        }

        public Builder triggerType(String triggerType) {
            this.triggerType = triggerType;
            return this;
        }

        public Builder triggerCondition(String triggerCondition) {
            this.triggerCondition = triggerCondition;
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

        public Builder deadlineHours(int deadlineHours) {
            this.deadlineHours = deadlineHours;
            return this;
        }

        public Builder autoAssign(boolean autoAssign) {
            this.autoAssign = autoAssign;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public AutoActionRule build() {
            return new AutoActionRule(this);
        }
    }

    public static class Reconstructor {
        private Long id;
        private String ruleCode;
        private String ruleName;
        private String triggerType;
        private String triggerCondition;
        private ActionSeverity severity;
        private ActionCategory category;
        private int deadlineHours;
        private boolean autoAssign;
        private boolean enabled;
        private LocalDateTime createdAt;

        public Reconstructor id(Long id) { this.id = id; return this; }
        public Reconstructor ruleCode(String ruleCode) { this.ruleCode = ruleCode; return this; }
        public Reconstructor ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public Reconstructor triggerType(String triggerType) { this.triggerType = triggerType; return this; }
        public Reconstructor triggerCondition(String triggerCondition) { this.triggerCondition = triggerCondition; return this; }
        public Reconstructor severity(ActionSeverity severity) { this.severity = severity; return this; }
        public Reconstructor category(ActionCategory category) { this.category = category; return this; }
        public Reconstructor deadlineHours(int deadlineHours) { this.deadlineHours = deadlineHours; return this; }
        public Reconstructor autoAssign(boolean autoAssign) { this.autoAssign = autoAssign; return this; }
        public Reconstructor enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Reconstructor createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public AutoActionRule build() {
            return new AutoActionRule(this);
        }
    }
}
