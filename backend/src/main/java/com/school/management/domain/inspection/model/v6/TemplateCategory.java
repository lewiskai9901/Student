package com.school.management.domain.inspection.model.v6;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 模板类别 - V6版本
 */
public class TemplateCategory {

    private Long id;
    private Long templateId;
    private String categoryCode;
    private String categoryName;
    private String description;
    private String icon;
    private String color;
    private BigDecimal weight;
    private BigDecimal maxScore;
    private Integer sortOrder;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<TemplateScoreItem> items = new ArrayList<>();

    public TemplateCategory() {}

    public static TemplateCategory create(Long templateId, String categoryCode, String categoryName) {
        TemplateCategory category = new TemplateCategory();
        category.templateId = templateId;
        category.categoryCode = categoryCode;
        category.categoryName = categoryName;
        category.weight = BigDecimal.ONE;
        category.maxScore = new BigDecimal("100");
        category.sortOrder = 0;
        category.isEnabled = true;
        category.createdAt = LocalDateTime.now();
        category.updatedAt = LocalDateTime.now();
        return category;
    }

    public void addItem(TemplateScoreItem item) {
        this.items.add(item);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<TemplateScoreItem> getItems() { return items; }
    public void setItems(List<TemplateScoreItem> items) { this.items = items; }
}
