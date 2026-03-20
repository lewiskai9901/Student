package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 评级结果
 */
public class RatingResult extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private Long dimensionId;
    private Long targetId;
    private String targetName;
    private String targetType;          // ORG / PLACE / USER
    private LocalDate cycleDate;
    private BigDecimal score;
    private String grade;
    private Integer rankNo;
    private LocalDateTime createdAt;

    protected RatingResult() {
    }

    private RatingResult(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId != null ? builder.tenantId : 0L;
        this.dimensionId = builder.dimensionId;
        this.targetId = builder.targetId;
        this.targetName = builder.targetName;
        this.targetType = builder.targetType;
        this.cycleDate = builder.cycleDate;
        this.score = builder.score;
        this.grade = builder.grade;
        this.rankNo = builder.rankNo;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
    }

    public static RatingResult create(Long dimensionId, Long targetId, String targetName,
                                       String targetType, LocalDate cycleDate,
                                       BigDecimal score, String grade, Integer rankNo) {
        return builder()
                .dimensionId(dimensionId)
                .targetId(targetId)
                .targetName(targetName)
                .targetType(targetType)
                .cycleDate(cycleDate)
                .score(score)
                .grade(grade)
                .rankNo(rankNo)
                .build();
    }

    public static RatingResult reconstruct(Builder builder) {
        return new RatingResult(builder);
    }

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getDimensionId() { return dimensionId; }
    public Long getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
    public String getTargetType() { return targetType; }
    public LocalDate getCycleDate() { return cycleDate; }
    public BigDecimal getScore() { return score; }
    public String getGrade() { return grade; }
    public Integer getRankNo() { return rankNo; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long dimensionId;
        private Long targetId;
        private String targetName;
        private String targetType;
        private LocalDate cycleDate;
        private BigDecimal score;
        private String grade;
        private Integer rankNo;
        private LocalDateTime createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder dimensionId(Long dimensionId) { this.dimensionId = dimensionId; return this; }
        public Builder targetId(Long targetId) { this.targetId = targetId; return this; }
        public Builder targetName(String targetName) { this.targetName = targetName; return this; }
        public Builder targetType(String targetType) { this.targetType = targetType; return this; }
        public Builder cycleDate(LocalDate cycleDate) { this.cycleDate = cycleDate; return this; }
        public Builder score(BigDecimal score) { this.score = score; return this; }
        public Builder grade(String grade) { this.grade = grade; return this; }
        public Builder rankNo(Integer rankNo) { this.rankNo = rankNo; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public RatingResult build() { return new RatingResult(this); }
    }
}
