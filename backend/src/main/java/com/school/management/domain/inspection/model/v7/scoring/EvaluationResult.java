package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 评选结果实体 — 记录某周期内某目标的评选结果
 */
public class EvaluationResult implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long ruleId;
    private String targetType;
    private Long targetId;
    private String targetName;
    private LocalDate cycleDate;
    private Integer levelNum;
    private String levelName;
    private BigDecimal score;
    private Integer rankNo;
    private String details;  // JSON: 各条件判定详情
    private LocalDateTime createdAt;

    protected EvaluationResult() {
    }

    private EvaluationResult(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId != null ? builder.tenantId : 0L;
        this.ruleId = builder.ruleId;
        this.targetType = builder.targetType;
        this.targetId = builder.targetId;
        this.targetName = builder.targetName;
        this.cycleDate = builder.cycleDate;
        this.levelNum = builder.levelNum;
        this.levelName = builder.levelName;
        this.score = builder.score;
        this.rankNo = builder.rankNo;
        this.details = builder.details;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    public static EvaluationResult reconstruct(Builder builder) {
        return new EvaluationResult(builder);
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public Long getRuleId() { return ruleId; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
    public LocalDate getCycleDate() { return cycleDate; }
    public Integer getLevelNum() { return levelNum; }
    public String getLevelName() { return levelName; }
    public BigDecimal getScore() { return score; }
    public Integer getRankNo() { return rankNo; }
    public String getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long ruleId;
        private String targetType;
        private Long targetId;
        private String targetName;
        private LocalDate cycleDate;
        private Integer levelNum;
        private String levelName;
        private BigDecimal score;
        private Integer rankNo;
        private String details;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder ruleId(Long ruleId) { this.ruleId = ruleId; return this; }
        public Builder targetType(String targetType) { this.targetType = targetType; return this; }
        public Builder targetId(Long targetId) { this.targetId = targetId; return this; }
        public Builder targetName(String targetName) { this.targetName = targetName; return this; }
        public Builder cycleDate(LocalDate cycleDate) { this.cycleDate = cycleDate; return this; }
        public Builder levelNum(Integer levelNum) { this.levelNum = levelNum; return this; }
        public Builder levelName(String levelName) { this.levelName = levelName; return this; }
        public Builder score(BigDecimal score) { this.score = score; return this; }
        public Builder rankNo(Integer rankNo) { this.rankNo = rankNo; return this; }
        public Builder details(String details) { this.details = details; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public EvaluationResult build() { return new EvaluationResult(this); }
    }
}
