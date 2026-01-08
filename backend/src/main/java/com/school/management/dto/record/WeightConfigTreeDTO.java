package com.school.management.dto.record;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 加权配置树形结构DTO
 * 用于展示检查记录的完整加权配置层级结构
 *
 * @author system
 * @since 2.0.0
 */
@Data
public class WeightConfigTreeDTO {

    // ==================== 记录级配置（根节点） ====================

    /**
     * 检查记录ID
     */
    private Long recordId;

    /**
     * 检查名称
     */
    private String checkName;

    /**
     * 是否全局启用加权
     */
    private Boolean weightEnabled;

    /**
     * 全局加权配置
     */
    private WeightConfigNode globalConfig;

    /**
     * 类别配置列表（子节点）
     */
    private List<CategoryWeightNode> categories;

    /**
     * 班级加权系数汇总
     */
    private List<ClassWeightSummary> classWeightSummary;

    /**
     * 是否启用多配置
     */
    private Boolean multiConfigEnabled;

    /**
     * 多加权配置方案列表（来自daily_check_weight_configs表）
     */
    private List<MultiWeightScheme> multiWeightSchemes;

    // ==================== 加权配置节点 ====================

    @Data
    public static class WeightConfigNode {
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
         * 加权模式：STANDARD/PER_CAPITA/SEGMENT/NONE
         */
        private String weightMode;

        /**
         * 加权模式描述
         */
        private String weightModeDesc;

        /**
         * 标准人数模式：FIXED/TARGET_AVERAGE/RANGE_AVERAGE
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

        /**
         * 分段规则（仅SEGMENT模式）
         */
        private List<SegmentRule> segmentRules;

        /**
         * 计算公式说明
         */
        private String formulaDescription;
    }

    // ==================== 类别级节点 ====================

    @Data
    public static class CategoryWeightNode {
        /**
         * 类别ID
         */
        private Long categoryId;

        /**
         * 类别名称
         */
        private String categoryName;

        /**
         * 类别编码
         */
        private String categoryCode;

        /**
         * 类别类型
         */
        private String categoryType;

        /**
         * 是否启用加权
         */
        private Boolean weightEnabled;

        /**
         * 是否继承上级配置
         */
        private Boolean inherited;

        /**
         * 继承来源描述（如：继承自全局配置）
         */
        private String inheritSource;

        /**
         * 当前生效的加权配置
         */
        private WeightConfigNode effectiveConfig;

        /**
         * 该类别下的扣分项列表
         */
        private List<ItemWeightNode> items;

        /**
         * 该类别的扣分统计
         */
        private CategoryDeductionSummary deductionSummary;
    }

    // ==================== 扣分项级节点 ====================

    @Data
    public static class ItemWeightNode {
        /**
         * 扣分项ID
         */
        private Long itemId;

        /**
         * 扣分项名称
         */
        private String itemName;

        /**
         * 扣分项编码
         */
        private String itemCode;

        /**
         * 是否启用加权
         */
        private Boolean weightEnabled;

        /**
         * 是否继承上级配置
         */
        private Boolean inherited;

        /**
         * 继承来源描述（如：继承自类别配置）
         */
        private String inheritSource;

        /**
         * 当前生效的加权配置
         */
        private WeightConfigNode effectiveConfig;

        /**
         * 基础扣分值
         */
        private BigDecimal baseScore;

        /**
         * 扣分模式：FIXED_DEDUCT/PER_PERSON_DEDUCT/SCORE_RANGE
         */
        private String deductMode;

        /**
         * 扣分模式描述
         */
        private String deductModeDesc;

        /**
         * 该项的实际扣分次数
         */
        private Integer deductionCount;

        /**
         * 该项的原始扣分合计
         */
        private BigDecimal originalScore;

        /**
         * 该项的加权扣分合计
         */
        private BigDecimal weightedScore;
    }

    // ==================== 分段规则 ====================

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
         * 加权系数
         */
        private BigDecimal weight;

        /**
         * 规则说明
         */
        private String description;
    }

    // ==================== 类别扣分统计 ====================

    @Data
    public static class CategoryDeductionSummary {
        /**
         * 扣分项数量
         */
        private Integer itemCount;

        /**
         * 实际扣分次数
         */
        private Integer deductionCount;

        /**
         * 原始扣分合计
         */
        private BigDecimal originalScore;

        /**
         * 加权扣分合计
         */
        private BigDecimal weightedScore;
    }

    // ==================== 班级加权系数汇总 ====================

    @Data
    public static class ClassWeightSummary {
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
         * 全局加权系数
         */
        private BigDecimal globalWeightFactor;

        /**
         * 各类别的加权系数（可能不同）
         */
        private List<CategoryWeightFactor> categoryFactors;

        /**
         * 原始总扣分
         */
        private BigDecimal originalTotalScore;

        /**
         * 加权后总扣分
         */
        private BigDecimal weightedTotalScore;
    }

    @Data
    public static class CategoryWeightFactor {
        /**
         * 类别ID
         */
        private Long categoryId;

        /**
         * 类别名称
         */
        private String categoryName;

        /**
         * 该类别的加权系数
         */
        private BigDecimal weightFactor;

        /**
         * 原始扣分
         */
        private BigDecimal originalScore;

        /**
         * 加权扣分
         */
        private BigDecimal weightedScore;
    }

    // ==================== 多加权配置方案 ====================

    @Data
    public static class MultiWeightScheme {
        /**
         * 配置关联ID（daily_check_weight_configs.id）
         */
        private Long id;

        /**
         * 加权配置ID（class_weight_configs.id）
         */
        private Long weightConfigId;

        /**
         * 配置名称
         */
        private String configName;

        /**
         * 颜色代码
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
         * 是否默认配置
         */
        private Boolean isDefault;

        /**
         * 优先级
         */
        private Integer priority;

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
         * 最小权重
         */
        private BigDecimal minWeight;

        /**
         * 最大权重
         */
        private BigDecimal maxWeight;

        /**
         * 计算公式说明
         */
        private String formulaDescription;
    }
}
