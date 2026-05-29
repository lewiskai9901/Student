package com.school.management.domain.inspection.model.scoring;

import com.school.management.domain.shared.AggregateRoot;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评分配置聚合根 - 定义模板的评分规则与约束
 */
public class ScoringProfile extends AggregateRoot<Long> {

    private Long tenantId;
    /** 历史保留: 兼容旧查询路径 (按 section 反查 profile). 主键索引是 projectId. */
    private Long sectionId;
    /** 项目-owned: 评分方案与项目同生命周期, NOT NULL. */
    private Long projectId;
    private BigDecimal maxScore;
    private BigDecimal minScore;
    private Integer precisionDigits;
    private Integer currentVersion;

    // 1.13 章节级归一化 (规模公平性) — 供装配层解析归一化分母时读取
    private NormalizeBy normalizeBy;          // NONE|PER_MEMBER|PER_PLACE|PER_SUB_ORG
    private NormalizationMode normalizationMode; // NONE|PER_CAPITA|SQRT_ADJUSTED
    private Integer baselinePopulation;       // 归一化基准人口/规模, 默认 1
    private BigDecimal normFloor;             // 归一化后下限 (NULL=不限)
    private BigDecimal normCap;               // 归一化后上限 (NULL=不限)

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
        this.projectId = builder.projectId;
        this.maxScore = builder.maxScore != null ? builder.maxScore : new BigDecimal("100");
        this.minScore = builder.minScore != null ? builder.minScore : BigDecimal.ZERO;
        this.precisionDigits = builder.precisionDigits != null ? builder.precisionDigits : 2;
        this.currentVersion = builder.currentVersion != null ? builder.currentVersion : 0;
        // 1.13 章节级归一化
        this.normalizeBy = builder.normalizeBy != null ? builder.normalizeBy : NormalizeBy.NONE;
        this.normalizationMode = builder.normalizationMode != null ? builder.normalizationMode : NormalizationMode.NONE;
        this.baselinePopulation = builder.baselinePopulation != null ? builder.baselinePopulation : 1;
        this.normFloor = builder.normFloor;
        this.normCap = builder.normCap;

        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedBy = builder.updatedBy;
        this.updatedAt = builder.updatedAt;
    }

    public static ScoringProfile create(Long sectionId, Long projectId, Long createdBy) {
        return builder()
                .sectionId(sectionId)
                .projectId(projectId)
                .createdBy(createdBy)
                .build();
    }

    /**
     * 兼容旧调用 (无 projectId), 仅供尚未迁移的测试与遗留路径使用.
     * @deprecated Use {@link #create(Long, Long, Long)} with projectId.
     */
    @Deprecated
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

    /**
     * 更新章节级归一化 (规模公平性) 配置.
     * 入参为 null 时按"未配置"语义兜底: 维度/方式回落 NONE, 基准回落 1, floor/cap 清空.
     */
    public void updateNormalization(NormalizeBy normalizeBy, NormalizationMode normalizationMode,
                                    Integer baselinePopulation, BigDecimal normFloor, BigDecimal normCap,
                                    Long updatedBy) {
        if (baselinePopulation != null && baselinePopulation < 1) {
            throw new IllegalArgumentException("baselinePopulation 必须 >= 1");
        }
        if (normFloor != null && normCap != null && normFloor.compareTo(normCap) > 0) {
            throw new IllegalArgumentException("normFloor 不能大于 normCap");
        }
        this.normalizeBy = normalizeBy != null ? normalizeBy : NormalizeBy.NONE;
        this.normalizationMode = normalizationMode != null ? normalizationMode : NormalizationMode.NONE;
        this.baselinePopulation = baselinePopulation != null ? baselinePopulation : 1;
        this.normFloor = normFloor;
        this.normCap = normCap;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementVersion() {
        this.currentVersion = (this.currentVersion != null ? this.currentVersion : 0) + 1;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getTenantId() { return tenantId; }
    public Long getSectionId() { return sectionId; }
    public Long getProjectId() { return projectId; }
    public BigDecimal getMaxScore() { return maxScore; }
    public BigDecimal getMinScore() { return minScore; }
    public Integer getPrecisionDigits() { return precisionDigits; }
    public Integer getCurrentVersion() { return currentVersion; }
    // 1.13 章节级归一化
    public NormalizeBy getNormalizeBy() { return normalizeBy; }
    public NormalizationMode getNormalizationMode() { return normalizationMode; }
    public Integer getBaselinePopulation() { return baselinePopulation; }
    public BigDecimal getNormFloor() { return normFloor; }
    public BigDecimal getNormCap() { return normCap; }

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
        private Long projectId;
        private BigDecimal maxScore;
        private BigDecimal minScore;
        private Integer precisionDigits;
        private Integer currentVersion;
        private NormalizeBy normalizeBy;
        private NormalizationMode normalizationMode;
        private Integer baselinePopulation;
        private BigDecimal normFloor;
        private BigDecimal normCap;
        private Long createdBy;
        private LocalDateTime createdAt;
        private Long updatedBy;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder sectionId(Long sectionId) { this.sectionId = sectionId; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder maxScore(BigDecimal maxScore) { this.maxScore = maxScore; return this; }
        public Builder minScore(BigDecimal minScore) { this.minScore = minScore; return this; }
        public Builder precisionDigits(Integer precisionDigits) { this.precisionDigits = precisionDigits; return this; }
        public Builder currentVersion(Integer currentVersion) { this.currentVersion = currentVersion; return this; }
        public Builder normalizeBy(NormalizeBy v) { this.normalizeBy = v; return this; }
        public Builder normalizationMode(NormalizationMode v) { this.normalizationMode = v; return this; }
        public Builder baselinePopulation(Integer v) { this.baselinePopulation = v; return this; }
        public Builder normFloor(BigDecimal v) { this.normFloor = v; return this; }
        public Builder normCap(BigDecimal v) { this.normCap = v; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedBy(Long updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ScoringProfile build() {
            return new ScoringProfile(this);
        }
    }
}
