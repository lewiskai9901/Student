package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V7 指标得分记录 — 实体
 * <p>
 * 记录某个指标在某个周期内针对某个评价对象的得分（含等级映射结果）。
 */
public class IndicatorScore implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long indicatorId;
    private Long targetId;
    private String targetName;
    private String targetType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal score;
    private String gradeCode;
    private String gradeName;
    private String gradeColor;
    private Integer sourceCount;
    private String detail; // JSON
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected IndicatorScore() {
    }

    private IndicatorScore(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.indicatorId = builder.indicatorId;
        this.targetId = builder.targetId;
        this.targetName = builder.targetName;
        this.targetType = builder.targetType;
        this.periodStart = builder.periodStart;
        this.periodEnd = builder.periodEnd;
        this.score = builder.score;
        this.gradeCode = builder.gradeCode;
        this.gradeName = builder.gradeName;
        this.gradeColor = builder.gradeColor;
        this.sourceCount = builder.sourceCount != null ? builder.sourceCount : 0;
        this.detail = builder.detail;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    // ── Factory ──────────────────────────────────────────────

    public static IndicatorScore create(Long indicatorId, Long targetId,
                                        String targetName, String targetType,
                                        LocalDate periodStart, LocalDate periodEnd) {
        IndicatorScore s = new IndicatorScore();
        s.indicatorId = indicatorId;
        s.targetId = targetId;
        s.targetName = targetName;
        s.targetType = targetType;
        s.periodStart = periodStart;
        s.periodEnd = periodEnd;
        s.sourceCount = 0;
        s.createdAt = LocalDateTime.now();
        return s;
    }

    public static IndicatorScore reconstruct(Builder builder) {
        return new IndicatorScore(builder);
    }

    // ── Commands ─────────────────────────────────────────────

    public void updateScore(BigDecimal score, String gradeCode, String gradeName,
                            String gradeColor, Integer sourceCount, String detail) {
        this.score = score;
        this.gradeCode = gradeCode;
        this.gradeName = gradeName;
        this.gradeColor = gradeColor;
        this.sourceCount = sourceCount;
        this.detail = detail;
        this.updatedAt = LocalDateTime.now();
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
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

    public Long getIndicatorId() {
        return indicatorId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getTargetType() {
        return targetType;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getGradeCode() {
        return gradeCode;
    }

    public String getGradeName() {
        return gradeName;
    }

    public String getGradeColor() {
        return gradeColor;
    }

    public Integer getSourceCount() {
        return sourceCount;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ── Builder ──────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long indicatorId;
        private Long targetId;
        private String targetName;
        private String targetType;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private BigDecimal score;
        private String gradeCode;
        private String gradeName;
        private String gradeColor;
        private Integer sourceCount;
        private String detail;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder indicatorId(Long indicatorId) { this.indicatorId = indicatorId; return this; }
        public Builder targetId(Long targetId) { this.targetId = targetId; return this; }
        public Builder targetName(String targetName) { this.targetName = targetName; return this; }
        public Builder targetType(String targetType) { this.targetType = targetType; return this; }
        public Builder periodStart(LocalDate periodStart) { this.periodStart = periodStart; return this; }
        public Builder periodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; return this; }
        public Builder score(BigDecimal score) { this.score = score; return this; }
        public Builder gradeCode(String gradeCode) { this.gradeCode = gradeCode; return this; }
        public Builder gradeName(String gradeName) { this.gradeName = gradeName; return this; }
        public Builder gradeColor(String gradeColor) { this.gradeColor = gradeColor; return this; }
        public Builder sourceCount(Integer sourceCount) { this.sourceCount = sourceCount; return this; }
        public Builder detail(String detail) { this.detail = detail; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public IndicatorScore build() {
            return new IndicatorScore(this);
        }
    }
}
