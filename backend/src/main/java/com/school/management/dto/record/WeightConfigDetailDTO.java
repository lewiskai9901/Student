package com.school.management.dto.record;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 检查记录加权配置详情DTO
 * 用于向用户展示检查的加权配置信息
 *
 * @author system
 * @since 3.0.0
 */
@Data
public class WeightConfigDetailDTO {

    // ==================== 基本信息 ====================

    /**
     * 配置ID
     */
    private Long configId;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 配置编码
     */
    private String configCode;

    /**
     * 是否启用加权
     */
    private Boolean weightEnabled;

    // ==================== 加权模式 ====================

    /**
     * 加权模式: STANDARD, PER_CAPITA, SEGMENT, NONE
     */
    private String weightMode;

    /**
     * 加权模式描述
     */
    private String weightModeDesc;

    /**
     * 加权模式详细说明
     */
    private String weightModeExplanation;

    // ==================== 标准人数配置 ====================

    /**
     * 标准人数模式: FIXED, TARGET_AVERAGE, RANGE_AVERAGE
     */
    private String standardSizeMode;

    /**
     * 标准人数模式描述
     */
    private String standardSizeModeDesc;

    /**
     * 标准人数
     */
    private Integer standardSize;

    // ==================== 权重限制 ====================

    /**
     * 最小权重系数
     */
    private BigDecimal minWeight;

    /**
     * 最大权重系数
     */
    private BigDecimal maxWeight;

    /**
     * 是否启用权重限制
     */
    private Boolean enableWeightLimit;

    // ==================== 分段规则 ====================

    /**
     * 分段规则列表(当weightMode=SEGMENT时使用)
     */
    private List<SegmentRule> segmentRules;

    // ==================== 班级加权系数 ====================

    /**
     * 各班级的加权系数
     */
    private List<ClassWeightFactor> classWeightFactors;

    // ==================== 计算公式说明 ====================

    /**
     * 加权公式说明
     */
    private String formulaDescription;

    /**
     * 计算示例
     */
    private String calculationExample;

    // ==================== 配置说明 ====================

    /**
     * 配置描述
     */
    private String description;

    // ==================== 多加权配置支持 ====================

    /**
     * 是否使用多加权配置
     * true表示使用了多个加权配置，每个分类可能有不同的加权方案
     */
    private Boolean multiConfigEnabled;

    /**
     * 多加权配置列表
     * 当multiConfigEnabled=true时，包含所有使用的加权配置详情
     */
    private List<MultiWeightConfig> multiConfigs;

    // ==================== 内部类定义 ====================

    /**
     * 分段规则
     */
    @Data
    public static class SegmentRule {
        /**
         * 最小人数
         */
        private Integer minSize;

        /**
         * 最大人数
         */
        private Integer maxSize;

        /**
         * 权重系数
         */
        private BigDecimal weight;

        /**
         * 规则说明
         */
        private String description;
    }

    /**
     * 班级加权系数
     */
    @Data
    public static class ClassWeightFactor {
        /**
         * 班级ID
         */
        private Long classId;

        /**
         * 班级名称
         */
        private String className;

        /**
         * 实际人数
         */
        private Integer actualSize;

        /**
         * 标准人数
         */
        private Integer standardSize;

        /**
         * 加权系数
         */
        private BigDecimal weightFactor;

        /**
         * 原始扣分
         */
        private BigDecimal originalScore;

        /**
         * 加权后扣分
         */
        private BigDecimal weightedScore;

        /**
         * 计算说明
         */
        private String calculationNote;

        /**
         * 使用的加权配置ID（多配置模式下使用）
         */
        private Long weightConfigId;

        /**
         * 使用的加权配置颜色标记（多配置模式下使用）
         */
        private String colorCode;
    }

    /**
     * 多加权配置项
     * 用于支持一次检查使用多个加权配置方案
     */
    @Data
    public static class MultiWeightConfig {
        /**
         * 配置ID
         */
        private Long configId;

        /**
         * 关联的加权方案ID
         */
        private Long weightConfigId;

        /**
         * 配置名称
         */
        private String configName;

        /**
         * 颜色标记（16进制颜色码）
         */
        private String colorCode;

        /**
         * 颜色名称
         */
        private String colorName;

        /**
         * 应用范围：CATEGORY/ITEM
         */
        private String applyScope;

        /**
         * 应用的分类ID列表
         */
        private List<Long> applyCategoryIds;

        /**
         * 应用的分类名称列表
         */
        private List<String> applyCategoryNames;

        /**
         * 加权模式
         */
        private String weightMode;

        /**
         * 加权模式描述
         */
        private String weightModeDesc;

        /**
         * 标准人数模式
         */
        private String standardSizeMode;

        /**
         * 标准人数模式描述
         */
        private String standardSizeModeDesc;

        /**
         * 计算后的标准人数
         */
        private Integer calculatedStandardSize;

        /**
         * 最小权重系数
         */
        private BigDecimal minWeight;

        /**
         * 最大权重系数
         */
        private BigDecimal maxWeight;

        /**
         * 是否为默认配置
         */
        private Boolean isDefault;

        /**
         * 优先级
         */
        private Integer priority;

        /**
         * 加权公式说明
         */
        private String formulaDescription;
    }
}
