package com.school.management.domain.inspection.model;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a bonus item configuration.
 * Defines how bonus scores are calculated for specific achievements.
 */
public class BonusItem extends Entity<Long> {

    private Long id;
    private Long categoryId;
    private String itemName;
    private BonusMode bonusMode;
    private BigDecimal fixedBonus;
    private String progressiveConfig;
    private BigDecimal improvementCoefficient;
    private String description;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected BonusItem() {
    }

    private BonusItem(Builder builder) {
        this.id = builder.id;
        this.categoryId = builder.categoryId;
        this.itemName = builder.itemName;
        this.bonusMode = builder.bonusMode != null ? builder.bonusMode : BonusMode.FIXED;
        this.fixedBonus = builder.fixedBonus;
        this.progressiveConfig = builder.progressiveConfig;
        this.improvementCoefficient = builder.improvementCoefficient;
        this.description = builder.description;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.status = builder.status != null ? builder.status : 1;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static BonusItem create(Long categoryId, String itemName, BonusMode bonusMode,
                                    BigDecimal fixedBonus, String progressiveConfig,
                                    BigDecimal improvementCoefficient, String description) {
        return builder()
            .categoryId(categoryId)
            .itemName(itemName)
            .bonusMode(bonusMode)
            .fixedBonus(fixedBonus)
            .progressiveConfig(progressiveConfig)
            .improvementCoefficient(improvementCoefficient)
            .description(description)
            .build();
    }

    public void update(String itemName, BonusMode bonusMode, BigDecimal fixedBonus,
                       String progressiveConfig, BigDecimal improvementCoefficient,
                       String description, Integer sortOrder) {
        this.itemName = itemName;
        this.bonusMode = bonusMode;
        this.fixedBonus = fixedBonus;
        this.progressiveConfig = progressiveConfig;
        this.improvementCoefficient = improvementCoefficient;
        this.description = description;
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void enable() {
        this.status = 1;
    }

    public void disable() {
        this.status = 0;
    }

    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }

    // Getters
    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCategoryId() { return categoryId; }
    public String getItemName() { return itemName; }
    public BonusMode getBonusMode() { return bonusMode; }
    public BigDecimal getFixedBonus() { return fixedBonus; }
    public String getProgressiveConfig() { return progressiveConfig; }
    public BigDecimal getImprovementCoefficient() { return improvementCoefficient; }
    public String getDescription() { return description; }
    public Integer getSortOrder() { return sortOrder; }
    public Integer getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long categoryId;
        private String itemName;
        private BonusMode bonusMode;
        private BigDecimal fixedBonus;
        private String progressiveConfig;
        private BigDecimal improvementCoefficient;
        private String description;
        private Integer sortOrder;
        private Integer status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder categoryId(Long categoryId) { this.categoryId = categoryId; return this; }
        public Builder itemName(String itemName) { this.itemName = itemName; return this; }
        public Builder bonusMode(BonusMode bonusMode) { this.bonusMode = bonusMode; return this; }
        public Builder fixedBonus(BigDecimal fixedBonus) { this.fixedBonus = fixedBonus; return this; }
        public Builder progressiveConfig(String progressiveConfig) { this.progressiveConfig = progressiveConfig; return this; }
        public Builder improvementCoefficient(BigDecimal improvementCoefficient) { this.improvementCoefficient = improvementCoefficient; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder status(Integer status) { this.status = status; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public BonusItem build() {
            return new BonusItem(this);
        }
    }
}
