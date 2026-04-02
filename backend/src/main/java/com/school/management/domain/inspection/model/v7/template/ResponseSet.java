package com.school.management.domain.inspection.model.v7.template;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * V7 可复用选项集聚合根
 */
public class ResponseSet extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private String setCode;
    private String setName;
    private Boolean isGlobal;
    private Boolean isEnabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    protected ResponseSet() {
    }

    private ResponseSet(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.setCode = builder.setCode;
        this.setName = builder.setName;
        this.isGlobal = builder.isGlobal != null ? builder.isGlobal : false;
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    public static ResponseSet create(String setCode, String setName, Long createdBy) {
        return builder()
                .setCode(setCode)
                .setName(setName)
                .createdBy(createdBy)
                .build();
    }

    public static ResponseSet reconstruct(Builder builder) {
        return new ResponseSet(builder);
    }

    public void update(String setName, Boolean isGlobal, Boolean isEnabled, Long updatedBy) {
        this.setName = setName;
        this.isGlobal = isGlobal;
        this.isEnabled = isEnabled;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
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

    public Long getTenantId() {
        return tenantId;
    }

    public String getSetCode() {
        return setCode;
    }

    public String getSetName() {
        return setName;
    }

    public Boolean getIsGlobal() {
        return isGlobal;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private String setCode;
        private String setName;
        private Boolean isGlobal;
        private Boolean isEnabled;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder setCode(String setCode) { this.setCode = setCode; return this; }
        public Builder setName(String setName) { this.setName = setName; return this; }
        public Builder isGlobal(Boolean isGlobal) { this.isGlobal = isGlobal; return this; }
        public Builder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ResponseSet build() {
            return new ResponseSet(this);
        }
    }
}
