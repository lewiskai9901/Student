package com.school.management.domain.inspection.model.v7.execution;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V7 提交明细（每个评分项）
 */
public class SubmissionDetail implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long submissionId;
    private Long templateItemId;
    private String itemCode;
    private String itemName;
    private Long sectionId;
    private String sectionName;
    private String itemType;
    private String responseValue;
    private ScoringMode scoringMode;
    private BigDecimal score;
    private String dimensions;
    private String scoringConfig;
    private String validationRules;
    private String conditionLogic;
    private BigDecimal itemWeight;    // 快照:提交时的项目权重
    private Integer timeSpentSeconds;
    private Boolean isFlagged;
    private String flagReason;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected SubmissionDetail() {
    }

    private SubmissionDetail(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.submissionId = builder.submissionId;
        this.templateItemId = builder.templateItemId;
        this.itemCode = builder.itemCode;
        this.itemName = builder.itemName;
        this.sectionId = builder.sectionId;
        this.sectionName = builder.sectionName;
        this.itemType = builder.itemType;
        this.responseValue = builder.responseValue;
        this.scoringMode = builder.scoringMode;
        this.score = builder.score;
        this.dimensions = builder.dimensions;
        this.scoringConfig = builder.scoringConfig;
        this.validationRules = builder.validationRules;
        this.conditionLogic = builder.conditionLogic;
        this.itemWeight = builder.itemWeight;
        this.timeSpentSeconds = builder.timeSpentSeconds;
        this.isFlagged = builder.isFlagged != null ? builder.isFlagged : false;
        this.flagReason = builder.flagReason;
        this.remark = builder.remark;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static SubmissionDetail create(Long submissionId, Long templateItemId,
                                          String itemCode, String itemName, String itemType) {
        return builder()
                .submissionId(submissionId)
                .templateItemId(templateItemId)
                .itemCode(itemCode)
                .itemName(itemName)
                .itemType(itemType)
                .build();
    }

    public static SubmissionDetail create(Long submissionId, Long templateItemId,
                                          String itemCode, String itemName, String itemType,
                                          Long sectionId, String sectionName, ScoringMode scoringMode) {
        return builder()
                .submissionId(submissionId)
                .templateItemId(templateItemId)
                .itemCode(itemCode)
                .itemName(itemName)
                .itemType(itemType)
                .sectionId(sectionId)
                .sectionName(sectionName)
                .scoringMode(scoringMode)
                .build();
    }

    public static SubmissionDetail create(Long submissionId, Long templateItemId,
                                          String itemCode, String itemName, String itemType,
                                          Long sectionId, String sectionName, ScoringMode scoringMode,
                                          String scoringConfig, String validationRules, String conditionLogic) {
        return builder()
                .submissionId(submissionId)
                .templateItemId(templateItemId)
                .itemCode(itemCode)
                .itemName(itemName)
                .itemType(itemType)
                .sectionId(sectionId)
                .sectionName(sectionName)
                .scoringMode(scoringMode)
                .scoringConfig(scoringConfig)
                .validationRules(validationRules)
                .conditionLogic(conditionLogic)
                .build();
    }

    public static SubmissionDetail reconstruct(Builder builder) {
        return new SubmissionDetail(builder);
    }

    public void updateResponse(String responseValue, ScoringMode scoringMode,
                               BigDecimal score, String dimensions) {
        this.responseValue = responseValue;
        this.scoringMode = scoringMode;
        this.score = score;
        this.dimensions = dimensions;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateRemark(String remark) {
        this.remark = remark;
        this.updatedAt = LocalDateTime.now();
    }

    public void flag(String reason) {
        this.isFlagged = true;
        this.flagReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void unflag() {
        this.isFlagged = false;
        this.flagReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getSubmissionId() { return submissionId; }
    public Long getTemplateItemId() { return templateItemId; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public Long getSectionId() { return sectionId; }
    public String getSectionName() { return sectionName; }
    public String getItemType() { return itemType; }
    public String getResponseValue() { return responseValue; }
    public ScoringMode getScoringMode() { return scoringMode; }
    public BigDecimal getScore() { return score; }
    public String getDimensions() { return dimensions; }
    public String getScoringConfig() { return scoringConfig; }
    public String getValidationRules() { return validationRules; }
    public String getConditionLogic() { return conditionLogic; }
    public BigDecimal getItemWeight() { return itemWeight; }
    public Integer getTimeSpentSeconds() { return timeSpentSeconds; }
    public Boolean getIsFlagged() { return isFlagged; }
    public String getFlagReason() { return flagReason; }
    public String getRemark() { return remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long submissionId;
        private Long templateItemId;
        private String itemCode;
        private String itemName;
        private Long sectionId;
        private String sectionName;
        private String itemType;
        private String responseValue;
        private ScoringMode scoringMode;
        private BigDecimal score;
        private String dimensions;
        private String scoringConfig;
        private String validationRules;
        private String conditionLogic;
        private BigDecimal itemWeight;
        private Integer timeSpentSeconds;
        private Boolean isFlagged;
        private String flagReason;
        private String remark;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder submissionId(Long submissionId) { this.submissionId = submissionId; return this; }
        public Builder templateItemId(Long templateItemId) { this.templateItemId = templateItemId; return this; }
        public Builder itemCode(String itemCode) { this.itemCode = itemCode; return this; }
        public Builder itemName(String itemName) { this.itemName = itemName; return this; }
        public Builder sectionId(Long sectionId) { this.sectionId = sectionId; return this; }
        public Builder sectionName(String sectionName) { this.sectionName = sectionName; return this; }
        public Builder itemType(String itemType) { this.itemType = itemType; return this; }
        public Builder responseValue(String responseValue) { this.responseValue = responseValue; return this; }
        public Builder scoringMode(ScoringMode scoringMode) { this.scoringMode = scoringMode; return this; }
        public Builder score(BigDecimal score) { this.score = score; return this; }
        public Builder dimensions(String dimensions) { this.dimensions = dimensions; return this; }
        public Builder scoringConfig(String scoringConfig) { this.scoringConfig = scoringConfig; return this; }
        public Builder validationRules(String validationRules) { this.validationRules = validationRules; return this; }
        public Builder conditionLogic(String conditionLogic) { this.conditionLogic = conditionLogic; return this; }
        public Builder itemWeight(BigDecimal itemWeight) { this.itemWeight = itemWeight; return this; }
        public Builder timeSpentSeconds(Integer timeSpentSeconds) { this.timeSpentSeconds = timeSpentSeconds; return this; }
        public Builder isFlagged(Boolean isFlagged) { this.isFlagged = isFlagged; return this; }
        public Builder flagReason(String flagReason) { this.flagReason = flagReason; return this; }
        public Builder remark(String remark) { this.remark = remark; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public SubmissionDetail build() { return new SubmissionDetail(this); }
    }
}
