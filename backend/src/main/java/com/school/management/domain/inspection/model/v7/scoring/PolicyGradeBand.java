package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评分方案等级映射实体
 */
public class PolicyGradeBand implements Entity<Long> {

    private Long id;
    private Long policyId;
    private String gradeCode;
    private String gradeName;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    protected PolicyGradeBand() {
    }

    private PolicyGradeBand(Builder builder) {
        this.id = builder.id;
        this.policyId = builder.policyId;
        this.gradeCode = builder.gradeCode;
        this.gradeName = builder.gradeName;
        this.minScore = builder.minScore;
        this.maxScore = builder.maxScore;
        this.sortOrder = builder.sortOrder != null ? builder.sortOrder : 0;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    public static PolicyGradeBand reconstruct(Builder builder) {
        return new PolicyGradeBand(builder);
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }
    public Long getPolicyId() { return policyId; }
    public String getGradeCode() { return gradeCode; }
    public String getGradeName() { return gradeName; }
    public BigDecimal getMinScore() { return minScore; }
    public BigDecimal getMaxScore() { return maxScore; }
    public Integer getSortOrder() { return sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long policyId;
        private String gradeCode;
        private String gradeName;
        private BigDecimal minScore;
        private BigDecimal maxScore;
        private Integer sortOrder;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder policyId(Long policyId) { this.policyId = policyId; return this; }
        public Builder gradeCode(String gradeCode) { this.gradeCode = gradeCode; return this; }
        public Builder gradeName(String gradeName) { this.gradeName = gradeName; return this; }
        public Builder minScore(BigDecimal minScore) { this.minScore = minScore; return this; }
        public Builder maxScore(BigDecimal maxScore) { this.maxScore = maxScore; return this; }
        public Builder sortOrder(Integer sortOrder) { this.sortOrder = sortOrder; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public PolicyGradeBand build() { return new PolicyGradeBand(this); }
    }
}
