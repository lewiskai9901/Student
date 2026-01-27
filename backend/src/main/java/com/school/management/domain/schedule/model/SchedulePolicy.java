package com.school.management.domain.schedule.model;

import com.school.management.domain.schedule.event.PolicyCreatedEvent;
import com.school.management.domain.schedule.event.PolicyStatusChangedEvent;
import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SchedulePolicy Aggregate Root.
 * Represents a scheduling policy that defines how inspectors are assigned
 * to inspection sessions on a recurring basis.
 */
public class SchedulePolicy extends AggregateRoot<Long> {

    private Long id;
    private String policyCode;
    private String policyName;
    private PolicyType policyType;
    private RotationAlgorithm rotationAlgorithm;
    private Long templateId;
    private List<Long> inspectorPool;
    private String scheduleConfig;
    private List<String> excludedDates;
    private boolean isEnabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // For JPA/MyBatis
    protected SchedulePolicy() {
        this.inspectorPool = new ArrayList<>();
        this.excludedDates = new ArrayList<>();
    }

    private SchedulePolicy(Builder builder) {
        this.id = builder.id;
        this.policyCode = builder.policyCode;
        this.policyName = builder.policyName;
        this.policyType = builder.policyType;
        this.rotationAlgorithm = builder.rotationAlgorithm;
        this.templateId = builder.templateId;
        this.inspectorPool = builder.inspectorPool != null
            ? new ArrayList<>(builder.inspectorPool) : new ArrayList<>();
        this.scheduleConfig = builder.scheduleConfig;
        this.excludedDates = builder.excludedDates != null
            ? new ArrayList<>(builder.excludedDates) : new ArrayList<>();
        this.isEnabled = true;
        this.createdBy = builder.createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;

        validate();
    }

    private SchedulePolicy(Reconstructor r) {
        this.id = r.id;
        this.policyCode = r.policyCode;
        this.policyName = r.policyName;
        this.policyType = r.policyType;
        this.rotationAlgorithm = r.rotationAlgorithm;
        this.templateId = r.templateId;
        this.inspectorPool = r.inspectorPool != null
            ? new ArrayList<>(r.inspectorPool) : new ArrayList<>();
        this.scheduleConfig = r.scheduleConfig;
        this.excludedDates = r.excludedDates != null
            ? new ArrayList<>(r.excludedDates) : new ArrayList<>();
        this.isEnabled = r.isEnabled;
        this.createdBy = r.createdBy;
        this.createdAt = r.createdAt;
        this.updatedAt = r.updatedAt;
    }

    /**
     * Factory method to create a new schedule policy.
     */
    public static SchedulePolicy create(String policyCode, String policyName,
                                         PolicyType policyType, RotationAlgorithm rotationAlgorithm,
                                         Long templateId, List<Long> inspectorPool,
                                         String scheduleConfig, List<String> excludedDates,
                                         Long createdBy) {
        SchedulePolicy policy = builder()
            .policyCode(policyCode)
            .policyName(policyName)
            .policyType(policyType)
            .rotationAlgorithm(rotationAlgorithm)
            .templateId(templateId)
            .inspectorPool(inspectorPool)
            .scheduleConfig(scheduleConfig)
            .excludedDates(excludedDates)
            .createdBy(createdBy)
            .build();

        policy.registerEvent(new PolicyCreatedEvent(policy));
        return policy;
    }

    /**
     * Enables this schedule policy.
     */
    public void enable() {
        this.isEnabled = true;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new PolicyStatusChangedEvent(this));
    }

    /**
     * Disables this schedule policy.
     */
    public void disable() {
        this.isEnabled = false;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new PolicyStatusChangedEvent(this));
    }

    /**
     * Updates the configuration of this schedule policy.
     */
    public void updateConfig(String policyName, RotationAlgorithm rotationAlgorithm,
                             List<Long> inspectorPool, String scheduleConfig,
                             List<String> excludedDates) {
        this.policyName = policyName;
        this.rotationAlgorithm = rotationAlgorithm;
        this.inspectorPool = inspectorPool != null
            ? new ArrayList<>(inspectorPool) : new ArrayList<>();
        this.scheduleConfig = scheduleConfig;
        this.excludedDates = excludedDates != null
            ? new ArrayList<>(excludedDates) : new ArrayList<>();
        this.updatedAt = LocalDateTime.now();
    }

    private void validate() {
        if (policyCode == null || policyCode.isBlank()) {
            throw new IllegalArgumentException("Policy code is required");
        }
        if (policyName == null || policyName.isBlank()) {
            throw new IllegalArgumentException("Policy name is required");
        }
        if (policyType == null) {
            throw new IllegalArgumentException("Policy type is required");
        }
        if (rotationAlgorithm == null) {
            throw new IllegalArgumentException("Rotation algorithm is required");
        }
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public String getPolicyName() {
        return policyName;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public RotationAlgorithm getRotationAlgorithm() {
        return rotationAlgorithm;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public List<Long> getInspectorPool() {
        return Collections.unmodifiableList(inspectorPool);
    }

    public String getScheduleConfig() {
        return scheduleConfig;
    }

    public List<String> getExcludedDates() {
        return Collections.unmodifiableList(excludedDates);
    }

    public boolean isEnabled() {
        return isEnabled;
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
        private String policyCode;
        private String policyName;
        private PolicyType policyType;
        private RotationAlgorithm rotationAlgorithm;
        private Long templateId;
        private List<Long> inspectorPool;
        private String scheduleConfig;
        private List<String> excludedDates;
        private Long createdBy;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder policyCode(String policyCode) {
            this.policyCode = policyCode;
            return this;
        }

        public Builder policyName(String policyName) {
            this.policyName = policyName;
            return this;
        }

        public Builder policyType(PolicyType policyType) {
            this.policyType = policyType;
            return this;
        }

        public Builder rotationAlgorithm(RotationAlgorithm rotationAlgorithm) {
            this.rotationAlgorithm = rotationAlgorithm;
            return this;
        }

        public Builder templateId(Long templateId) {
            this.templateId = templateId;
            return this;
        }

        public Builder inspectorPool(List<Long> inspectorPool) {
            this.inspectorPool = inspectorPool;
            return this;
        }

        public Builder scheduleConfig(String scheduleConfig) {
            this.scheduleConfig = scheduleConfig;
            return this;
        }

        public Builder excludedDates(List<String> excludedDates) {
            this.excludedDates = excludedDates;
            return this;
        }

        public Builder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public SchedulePolicy build() {
            return new SchedulePolicy(this);
        }
    }

    public static class Reconstructor {
        private Long id;
        private String policyCode;
        private String policyName;
        private PolicyType policyType;
        private RotationAlgorithm rotationAlgorithm;
        private Long templateId;
        private List<Long> inspectorPool;
        private String scheduleConfig;
        private List<String> excludedDates;
        private boolean isEnabled;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Reconstructor id(Long id) { this.id = id; return this; }
        public Reconstructor policyCode(String policyCode) { this.policyCode = policyCode; return this; }
        public Reconstructor policyName(String policyName) { this.policyName = policyName; return this; }
        public Reconstructor policyType(PolicyType policyType) { this.policyType = policyType; return this; }
        public Reconstructor rotationAlgorithm(RotationAlgorithm rotationAlgorithm) { this.rotationAlgorithm = rotationAlgorithm; return this; }
        public Reconstructor templateId(Long templateId) { this.templateId = templateId; return this; }
        public Reconstructor inspectorPool(List<Long> inspectorPool) { this.inspectorPool = inspectorPool; return this; }
        public Reconstructor scheduleConfig(String scheduleConfig) { this.scheduleConfig = scheduleConfig; return this; }
        public Reconstructor excludedDates(List<String> excludedDates) { this.excludedDates = excludedDates; return this; }
        public Reconstructor isEnabled(boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Reconstructor createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Reconstructor createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Reconstructor updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public SchedulePolicy build() {
            return new SchedulePolicy(this);
        }
    }
}
