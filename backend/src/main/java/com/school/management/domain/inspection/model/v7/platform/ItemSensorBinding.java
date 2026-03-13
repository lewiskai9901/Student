package com.school.management.domain.inspection.model.v7.platform;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * V7 检查项-传感器绑定实体
 * 将IoT传感器绑定到模板检查项，支持自动填充和自动评分。
 */
public class ItemSensorBinding implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long templateItemId;
    private Long sensorId;
    private Boolean autoFill;
    private Boolean autoScore;
    /** JSON格式的自动评分阈值配置 */
    private String scoringThresholds;
    private LocalDateTime createdAt;

    protected ItemSensorBinding() {
    }

    private ItemSensorBinding(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.templateItemId = builder.templateItemId;
        this.sensorId = builder.sensorId;
        this.autoFill = builder.autoFill != null ? builder.autoFill : true;
        this.autoScore = builder.autoScore != null ? builder.autoScore : false;
        this.scoringThresholds = builder.scoringThresholds;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    public static ItemSensorBinding create(Long templateItemId, Long sensorId,
                                            Boolean autoFill, Boolean autoScore,
                                            String scoringThresholds) {
        return builder()
                .templateItemId(templateItemId)
                .sensorId(sensorId)
                .autoFill(autoFill)
                .autoScore(autoScore)
                .scoringThresholds(scoringThresholds)
                .build();
    }

    public static ItemSensorBinding reconstruct(Builder builder) {
        return new ItemSensorBinding(builder);
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getTemplateItemId() { return templateItemId; }
    public Long getSensorId() { return sensorId; }
    public Boolean getAutoFill() { return autoFill; }
    public Boolean getAutoScore() { return autoScore; }
    public String getScoringThresholds() { return scoringThresholds; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long templateItemId;
        private Long sensorId;
        private Boolean autoFill;
        private Boolean autoScore;
        private String scoringThresholds;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder templateItemId(Long templateItemId) { this.templateItemId = templateItemId; return this; }
        public Builder sensorId(Long sensorId) { this.sensorId = sensorId; return this; }
        public Builder autoFill(Boolean autoFill) { this.autoFill = autoFill; return this; }
        public Builder autoScore(Boolean autoScore) { this.autoScore = autoScore; return this; }
        public Builder scoringThresholds(String scoringThresholds) { this.scoringThresholds = scoringThresholds; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public ItemSensorBinding build() { return new ItemSensorBinding(this); }
    }
}
