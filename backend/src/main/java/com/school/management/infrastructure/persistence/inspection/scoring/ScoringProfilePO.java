package com.school.management.infrastructure.persistence.inspection.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_scoring_profiles")
public class ScoringProfilePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    /** 历史保留: 兼容旧查询路径 (按 section 反查 profile). 主键索引仍是 projectId. */
    private Long sectionId;
    /** 项目-owned: 评分方案与项目同生命周期, NOT NULL. */
    private Long projectId;
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
    private String decayMode;
    private BigDecimal decayRatePerDay;
    private BigDecimal decayFloor;

    // 1.11 多评审员聚合
    private String multiRaterMode;
    private String raterWeightBy;
    private BigDecimal consensusThreshold;

    // 1.12 分布校准
    private Boolean calibrationEnabled;
    private String calibrationMethod;
    private Integer calibrationPeriodDays;
    private Integer calibrationMinSamples;

    // 1.13 章节级归一化 (规模公平性) — 枚举存 String, 与既有 decayMode/multiRaterMode 风格一致
    private String normalizeBy;            // NONE|PER_MEMBER|PER_PLACE|PER_SUB_ORG
    private String normalizationMode;      // NONE|PER_CAPITA|SQRT_ADJUSTED
    private Integer baselinePopulation;
    private BigDecimal normFloor;
    private BigDecimal normCap;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
