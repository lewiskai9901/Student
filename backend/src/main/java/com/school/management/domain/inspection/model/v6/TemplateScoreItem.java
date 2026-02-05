package com.school.management.domain.inspection.model.v6;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模板扣分项 - V6版本
 */
public class TemplateScoreItem {

    private Long id;
    private Long categoryId;
    private String itemCode;
    private String itemName;
    private String description;
    private ScoringMode scoringMode;
    private BigDecimal score;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private BigDecimal perPersonScore;
    private Boolean canLinkIndividual;
    private Boolean requiresPhoto;
    private Boolean requiresRemark;
    private String checkPoints; // JSON
    private Integer sortOrder;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ScoringMode {
        DEDUCTION,  // 扣分
        ADDITION,   // 加分
        FIXED,      // 固定分值
        PER_PERSON, // 按人数
        RANGE       // 区间
    }

    public TemplateScoreItem() {}

    public static TemplateScoreItem create(Long categoryId, String itemCode, String itemName, BigDecimal score) {
        TemplateScoreItem item = new TemplateScoreItem();
        item.categoryId = categoryId;
        item.itemCode = itemCode;
        item.itemName = itemName;
        item.score = score;
        item.scoringMode = ScoringMode.DEDUCTION;
        item.minScore = BigDecimal.ZERO;
        item.canLinkIndividual = false;
        item.requiresPhoto = false;
        item.requiresRemark = false;
        item.sortOrder = 0;
        item.isEnabled = true;
        item.createdAt = LocalDateTime.now();
        item.updatedAt = LocalDateTime.now();
        return item;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ScoringMode getScoringMode() { return scoringMode; }
    public void setScoringMode(ScoringMode scoringMode) { this.scoringMode = scoringMode; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public BigDecimal getMinScore() { return minScore; }
    public void setMinScore(BigDecimal minScore) { this.minScore = minScore; }

    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }

    public BigDecimal getPerPersonScore() { return perPersonScore; }
    public void setPerPersonScore(BigDecimal perPersonScore) { this.perPersonScore = perPersonScore; }

    public Boolean getCanLinkIndividual() { return canLinkIndividual; }
    public void setCanLinkIndividual(Boolean canLinkIndividual) { this.canLinkIndividual = canLinkIndividual; }

    public Boolean getRequiresPhoto() { return requiresPhoto; }
    public void setRequiresPhoto(Boolean requiresPhoto) { this.requiresPhoto = requiresPhoto; }

    public Boolean getRequiresRemark() { return requiresRemark; }
    public void setRequiresRemark(Boolean requiresRemark) { this.requiresRemark = requiresRemark; }

    public String getCheckPoints() { return checkPoints; }
    public void setCheckPoints(String checkPoints) { this.checkPoints = checkPoints; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
