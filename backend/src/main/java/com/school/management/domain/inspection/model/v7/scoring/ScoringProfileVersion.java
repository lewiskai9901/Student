package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * V7 评分配置版本快照
 */
public class ScoringProfileVersion implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long profileId;
    private Integer version;
    private String snapshot;      // JSON: {dimensions, rules, bands, escalation}
    private LocalDateTime publishedAt;
    private Long publishedBy;
    private String changeSummary;
    private LocalDateTime createdAt;

    protected ScoringProfileVersion() {
    }

    private ScoringProfileVersion(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId != null ? builder.tenantId : 0L;
        this.profileId = builder.profileId;
        this.version = builder.version;
        this.snapshot = builder.snapshot;
        this.publishedAt = builder.publishedAt != null ? builder.publishedAt : LocalDateTime.now();
        this.publishedBy = builder.publishedBy;
        this.changeSummary = builder.changeSummary;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    public static ScoringProfileVersion reconstruct(Builder builder) {
        return new ScoringProfileVersion(builder);
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getProfileId() {
        return profileId;
    }

    public Integer getVersion() {
        return version;
    }

    public String getSnapshot() {
        return snapshot;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public Long getPublishedBy() {
        return publishedBy;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long profileId;
        private Integer version;
        private String snapshot;
        private LocalDateTime publishedAt;
        private Long publishedBy;
        private String changeSummary;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder profileId(Long profileId) { this.profileId = profileId; return this; }
        public Builder version(Integer version) { this.version = version; return this; }
        public Builder snapshot(String snapshot) { this.snapshot = snapshot; return this; }
        public Builder publishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; return this; }
        public Builder publishedBy(Long publishedBy) { this.publishedBy = publishedBy; return this; }
        public Builder changeSummary(String changeSummary) { this.changeSummary = changeSummary; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ScoringProfileVersion build() {
            return new ScoringProfileVersion(this);
        }
    }
}
