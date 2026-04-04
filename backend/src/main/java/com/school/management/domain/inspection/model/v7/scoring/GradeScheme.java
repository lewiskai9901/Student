package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * V7 等级方案 — 聚合根
 */
public class GradeScheme implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private String displayName;
    private String description;
    private String schemeType;
    private Boolean isSystem;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Transient — loaded separately, not part of builder/constructor */
    private List<GradeDefinition> grades = new ArrayList<>();

    protected GradeScheme() {
    }

    private GradeScheme(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.schemeType = builder.schemeType;
        this.isSystem = builder.isSystem != null ? builder.isSystem : false;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    // ── Factory ──────────────────────────────────────────────

    public static GradeScheme create(Long tenantId, String displayName, String description,
                                     String schemeType, Long createdBy) {
        GradeScheme scheme = new GradeScheme();
        scheme.tenantId = tenantId;
        scheme.displayName = displayName;
        scheme.description = description;
        scheme.schemeType = schemeType;
        scheme.isSystem = false;
        scheme.createdBy = createdBy;
        scheme.createdAt = LocalDateTime.now();
        return scheme;
    }

    public static GradeScheme reconstruct(Builder builder) {
        return new GradeScheme(builder);
    }

    // ── Commands ─────────────────────────────────────────────

    public void updateInfo(String displayName, String description) {
        guardSystemPreset();
        this.displayName = displayName;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void setGrades(List<GradeDefinition> grades) {
        this.grades = grades;
    }

    // ── Guards ────────────────────────────────────────────────

    private void guardSystemPreset() {
        if (Boolean.TRUE.equals(isSystem)) {
            throw new IllegalStateException("Cannot modify a system-preset grade scheme");
        }
    }

    // ── Getters ──────────────────────────────────────────────

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

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getSchemeType() {
        return schemeType;
    }

    public Boolean getIsSystem() {
        return isSystem;
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

    public List<GradeDefinition> getGrades() {
        return grades;
    }

    // ── Builder ──────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private String displayName;
        private String description;
        private String schemeType;
        private Boolean isSystem;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder schemeType(String schemeType) { this.schemeType = schemeType; return this; }
        public Builder isSystem(Boolean isSystem) { this.isSystem = isSystem; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public GradeScheme build() {
            return new GradeScheme(this);
        }
    }
}
