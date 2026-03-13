package com.school.management.domain.inspection.model.v7.template;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V7 选项集选项
 */
public class ResponseSetOption implements Entity<Long> {

    private Long id;
    private Long responseSetId;
    private String optionValue;
    private String optionLabel;
    private String optionColor;
    private BigDecimal score;
    private Boolean isFlagged;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ResponseSetOption() {
    }

    private ResponseSetOption(Builder builder) {
        this.id = builder.id;
        this.responseSetId = builder.responseSetId;
        this.optionValue = builder.optionValue;
        this.optionLabel = builder.optionLabel;
        this.optionColor = builder.optionColor;
        this.score = builder.score;
        this.isFlagged = builder.isFlagged != null ? builder.isFlagged : false;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static ResponseSetOption create(Long responseSetId, String optionValue,
                                           String optionLabel, BigDecimal score) {
        return builder()
                .responseSetId(responseSetId)
                .optionValue(optionValue)
                .optionLabel(optionLabel)
                .score(score)
                .build();
    }

    public static ResponseSetOption reconstruct(Builder builder) {
        return new ResponseSetOption(builder);
    }

    public void update(String optionLabel, String optionColor, BigDecimal score,
                       Boolean isFlagged, Integer sortOrder) {
        this.optionLabel = optionLabel;
        this.optionColor = optionColor;
        this.score = score;
        this.isFlagged = isFlagged;
        this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResponseSetId() {
        return responseSetId;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public String getOptionLabel() {
        return optionLabel;
    }

    public String getOptionColor() {
        return optionColor;
    }

    public BigDecimal getScore() {
        return score;
    }

    public Boolean getIsFlagged() {
        return isFlagged;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long responseSetId;
        private String optionValue;
        private String optionLabel;
        private String optionColor;
        private BigDecimal score;
        private Boolean isFlagged;
        private Integer sortOrder;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder responseSetId(Long responseSetId) { this.responseSetId = responseSetId; return this; }
        public Builder optionValue(String optionValue) { this.optionValue = optionValue; return this; }
        public Builder optionLabel(String optionLabel) { this.optionLabel = optionLabel; return this; }
        public Builder optionColor(String optionColor) { this.optionColor = optionColor; return this; }
        public Builder score(BigDecimal score) { this.score = score; return this; }
        public Builder isFlagged(Boolean isFlagged) { this.isFlagged = isFlagged; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ResponseSetOption build() {
            return new ResponseSetOption(this);
        }
    }
}
