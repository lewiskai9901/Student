package com.school.management.domain.analytics.model;

import com.school.management.domain.shared.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * AnalyticsSnapshot Entity.
 * Represents a point-in-time snapshot of analytics data such as class rankings,
 * department trends, inspector workload, or violation distributions.
 */
public class AnalyticsSnapshot extends Entity<Long> {

    private Long id;
    private SnapshotType snapshotType;
    private String snapshotScope;
    private Long scopeId;
    private LocalDate snapshotDate;
    private String dataJson;
    private LocalDateTime generatedAt;

    // For MyBatis reconstruction
    protected AnalyticsSnapshot() {
    }

    private AnalyticsSnapshot(Builder builder) {
        this.id = builder.id;
        this.snapshotType = builder.snapshotType;
        this.snapshotScope = builder.snapshotScope;
        this.scopeId = builder.scopeId;
        this.snapshotDate = builder.snapshotDate;
        this.dataJson = builder.dataJson;
        this.generatedAt = builder.generatedAt != null ? builder.generatedAt : LocalDateTime.now();
    }

    /**
     * Reconstruct from persistence.
     */
    public static AnalyticsSnapshot reconstruct(Long id, SnapshotType snapshotType,
                                                  String snapshotScope, Long scopeId,
                                                  LocalDate snapshotDate, String dataJson,
                                                  LocalDateTime generatedAt) {
        return builder()
                .id(id)
                .snapshotType(snapshotType)
                .snapshotScope(snapshotScope)
                .scopeId(scopeId)
                .snapshotDate(snapshotDate)
                .dataJson(dataJson)
                .generatedAt(generatedAt)
                .build();
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

    public SnapshotType getSnapshotType() {
        return snapshotType;
    }

    public String getSnapshotScope() {
        return snapshotScope;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public String getDataJson() {
        return dataJson;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private SnapshotType snapshotType;
        private String snapshotScope;
        private Long scopeId;
        private LocalDate snapshotDate;
        private String dataJson;
        private LocalDateTime generatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder snapshotType(SnapshotType snapshotType) { this.snapshotType = snapshotType; return this; }
        public Builder snapshotScope(String snapshotScope) { this.snapshotScope = snapshotScope; return this; }
        public Builder scopeId(Long scopeId) { this.scopeId = scopeId; return this; }
        public Builder snapshotDate(LocalDate snapshotDate) { this.snapshotDate = snapshotDate; return this; }
        public Builder dataJson(String dataJson) { this.dataJson = dataJson; return this; }
        public Builder generatedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; return this; }

        public AnalyticsSnapshot build() {
            return new AnalyticsSnapshot(this);
        }
    }
}
