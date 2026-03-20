package com.school.management.domain.inspection.model.v7.scoring;

import com.school.management.domain.shared.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V7 评分配置聚合根 - 定义模板的评分规则与约束
 */
public class ScoringProfile extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private Long sectionId;
    private BigDecimal maxScore;
    private BigDecimal minScore;
    private Integer precisionDigits;
    private Integer currentVersion;

    // 1.9 趋势因子
    private Boolean trendFactorEnabled;
    private Integer trendLookbackDays;
    private BigDecimal trendBonusPerPercent;
    private BigDecimal trendPenaltyPerPercent;
    private BigDecimal trendMaxAdjustment;

    // 1.10 分数衰减
    private Boolean decayEnabled;
    private String decayMode;          // LINEAR | EXPONENTIAL
    private BigDecimal decayRatePerDay;
    private BigDecimal decayFloor;

    // 1.11 多评审员聚合
    private String multiRaterMode;     // LATEST|AVERAGE|WEIGHTED_AVERAGE|MEDIAN|MAX|MIN|CONSENSUS
    private String raterWeightBy;      // EQUAL|BY_ROLE|BY_EXPERIENCE|CUSTOM
    private BigDecimal consensusThreshold;

    // 1.12 分布校准
    private Boolean calibrationEnabled;
    private String calibrationMethod;  // Z_SCORE|MIN_MAX|PERCENTILE_RANK|IRT
    private Integer calibrationPeriodDays;
    private Integer calibrationMinSamples;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    protected ScoringProfile() {
    }

    private ScoringProfile(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId != null ? builder.tenantId : 0L;
        this.sectionId = builder.sectionId;
        this.maxScore = builder.maxScore != null ? builder.maxScore : new BigDecimal("100");
        this.minScore = builder.minScore != null ? builder.minScore : BigDecimal.ZERO;
        this.precisionDigits = builder.precisionDigits != null ? builder.precisionDigits : 2;
        this.currentVersion = builder.currentVersion != null ? builder.currentVersion : 0;
        // 1.9
        this.trendFactorEnabled = builder.trendFactorEnabled != null ? builder.trendFactorEnabled : false;
        this.trendLookbackDays = builder.trendLookbackDays != null ? builder.trendLookbackDays : 7;
        this.trendBonusPerPercent = builder.trendBonusPerPercent;
        this.trendPenaltyPerPercent = builder.trendPenaltyPerPercent;
        this.trendMaxAdjustment = builder.trendMaxAdjustment;
        // 1.10
        this.decayEnabled = builder.decayEnabled != null ? builder.decayEnabled : false;
        this.decayMode = builder.decayMode;
        this.decayRatePerDay = builder.decayRatePerDay;
        this.decayFloor = builder.decayFloor;
        // 1.11
        this.multiRaterMode = builder.multiRaterMode;
        this.raterWeightBy = builder.raterWeightBy;
        this.consensusThreshold = builder.consensusThreshold;
        // 1.12
        this.calibrationEnabled = builder.calibrationEnabled != null ? builder.calibrationEnabled : false;
        this.calibrationMethod = builder.calibrationMethod;
        this.calibrationPeriodDays = builder.calibrationPeriodDays;
        this.calibrationMinSamples = builder.calibrationMinSamples;

        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    public static ScoringProfile create(Long sectionId, Long createdBy) {
        return builder()
                .sectionId(sectionId)
                .createdBy(createdBy)
                .build();
    }

    public static ScoringProfile reconstruct(Builder builder) {
        return new ScoringProfile(builder);
    }

    public void update(BigDecimal maxScore, BigDecimal minScore,
                       Integer precisionDigits, Long updatedBy) {
        if (minScore != null && maxScore != null && minScore.compareTo(maxScore) > 0) {
            throw new IllegalArgumentException("minScore 不能大于 maxScore");
        }
        if (precisionDigits != null && (precisionDigits < 0 || precisionDigits > 10)) {
            throw new IllegalArgumentException("precisionDigits 必须在 0-10 之间");
        }
        this.maxScore = maxScore;
        this.minScore = minScore;
        this.precisionDigits = precisionDigits;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateAdvancedSettings(Boolean trendFactorEnabled, Integer trendLookbackDays,
                                        BigDecimal trendBonusPerPercent, BigDecimal trendPenaltyPerPercent,
                                        BigDecimal trendMaxAdjustment,
                                        Boolean decayEnabled, String decayMode,
                                        BigDecimal decayRatePerDay, BigDecimal decayFloor,
                                        String multiRaterMode, String raterWeightBy,
                                        BigDecimal consensusThreshold,
                                        Boolean calibrationEnabled, String calibrationMethod,
                                        Integer calibrationPeriodDays, Integer calibrationMinSamples,
                                        Long updatedBy) {
        this.trendFactorEnabled = trendFactorEnabled;
        this.trendLookbackDays = trendLookbackDays;
        this.trendBonusPerPercent = trendBonusPerPercent;
        this.trendPenaltyPerPercent = trendPenaltyPerPercent;
        this.trendMaxAdjustment = trendMaxAdjustment;
        this.decayEnabled = decayEnabled;
        this.decayMode = decayMode;
        this.decayRatePerDay = decayRatePerDay;
        this.decayFloor = decayFloor;
        this.multiRaterMode = multiRaterMode;
        this.raterWeightBy = raterWeightBy;
        this.consensusThreshold = consensusThreshold;
        this.calibrationEnabled = calibrationEnabled;
        this.calibrationMethod = calibrationMethod;
        this.calibrationPeriodDays = calibrationPeriodDays;
        this.calibrationMinSamples = calibrationMinSamples;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementVersion() {
        this.currentVersion = (this.currentVersion != null ? this.currentVersion : 0) + 1;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public Long getSectionId() { return sectionId; }
    public BigDecimal getMaxScore() { return maxScore; }
    public BigDecimal getMinScore() { return minScore; }
    public Integer getPrecisionDigits() { return precisionDigits; }
    public Integer getCurrentVersion() { return currentVersion; }
    // 1.9
    public Boolean getTrendFactorEnabled() { return trendFactorEnabled; }
    public Integer getTrendLookbackDays() { return trendLookbackDays; }
    public BigDecimal getTrendBonusPerPercent() { return trendBonusPerPercent; }
    public BigDecimal getTrendPenaltyPerPercent() { return trendPenaltyPerPercent; }
    public BigDecimal getTrendMaxAdjustment() { return trendMaxAdjustment; }
    // 1.10
    public Boolean getDecayEnabled() { return decayEnabled; }
    public String getDecayMode() { return decayMode; }
    public BigDecimal getDecayRatePerDay() { return decayRatePerDay; }
    public BigDecimal getDecayFloor() { return decayFloor; }
    // 1.11
    public String getMultiRaterMode() { return multiRaterMode; }
    public String getRaterWeightBy() { return raterWeightBy; }
    public BigDecimal getConsensusThreshold() { return consensusThreshold; }
    // 1.12
    public Boolean getCalibrationEnabled() { return calibrationEnabled; }
    public String getCalibrationMethod() { return calibrationMethod; }
    public Integer getCalibrationPeriodDays() { return calibrationPeriodDays; }
    public Integer getCalibrationMinSamples() { return calibrationMinSamples; }

    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long sectionId;
        private BigDecimal maxScore;
        private BigDecimal minScore;
        private Integer precisionDigits;
        private Integer currentVersion;
        private Boolean trendFactorEnabled;
        private Integer trendLookbackDays;
        private BigDecimal trendBonusPerPercent;
        private BigDecimal trendPenaltyPerPercent;
        private BigDecimal trendMaxAdjustment;
        private Boolean decayEnabled;
        private String decayMode;
        private BigDecimal decayRatePerDay;
        private BigDecimal decayFloor;
        private String multiRaterMode;
        private String raterWeightBy;
        private BigDecimal consensusThreshold;
        private Boolean calibrationEnabled;
        private String calibrationMethod;
        private Integer calibrationPeriodDays;
        private Integer calibrationMinSamples;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder sectionId(Long sectionId) { this.sectionId = sectionId; return this; }
        public Builder maxScore(BigDecimal maxScore) { this.maxScore = maxScore; return this; }
        public Builder minScore(BigDecimal minScore) { this.minScore = minScore; return this; }
        public Builder precisionDigits(Integer precisionDigits) { this.precisionDigits = precisionDigits; return this; }
        public Builder currentVersion(Integer currentVersion) { this.currentVersion = currentVersion; return this; }
        public Builder trendFactorEnabled(Boolean v) { this.trendFactorEnabled = v; return this; }
        public Builder trendLookbackDays(Integer v) { this.trendLookbackDays = v; return this; }
        public Builder trendBonusPerPercent(BigDecimal v) { this.trendBonusPerPercent = v; return this; }
        public Builder trendPenaltyPerPercent(BigDecimal v) { this.trendPenaltyPerPercent = v; return this; }
        public Builder trendMaxAdjustment(BigDecimal v) { this.trendMaxAdjustment = v; return this; }
        public Builder decayEnabled(Boolean v) { this.decayEnabled = v; return this; }
        public Builder decayMode(String v) { this.decayMode = v; return this; }
        public Builder decayRatePerDay(BigDecimal v) { this.decayRatePerDay = v; return this; }
        public Builder decayFloor(BigDecimal v) { this.decayFloor = v; return this; }
        public Builder multiRaterMode(String v) { this.multiRaterMode = v; return this; }
        public Builder raterWeightBy(String v) { this.raterWeightBy = v; return this; }
        public Builder consensusThreshold(BigDecimal v) { this.consensusThreshold = v; return this; }
        public Builder calibrationEnabled(Boolean v) { this.calibrationEnabled = v; return this; }
        public Builder calibrationMethod(String v) { this.calibrationMethod = v; return this; }
        public Builder calibrationPeriodDays(Integer v) { this.calibrationPeriodDays = v; return this; }
        public Builder calibrationMinSamples(Integer v) { this.calibrationMinSamples = v; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ScoringProfile build() {
            return new ScoringProfile(this);
        }
    }
}
