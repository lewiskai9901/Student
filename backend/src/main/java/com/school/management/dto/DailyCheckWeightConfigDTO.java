package com.school.management.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 日常检查加权配置DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class DailyCheckWeightConfigDTO {

    /**
     * 配置ID
     */
    private Long id;

    /**
     * 日常检查ID
     */
    private Long dailyCheckId;

    /**
     * 加权配置ID
     */
    private Long weightConfigId;

    /**
     * 加权配置名称
     */
    private String weightConfigName;

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
     * 应用的扣分项ID列表
     */
    private List<Long> applyItemIds;

    /**
     * 是否为默认配置
     */
    private Boolean isDefault;

    /**
     * 优先级
     */
    private Integer priority;

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
     * 最小权重限制
     */
    private BigDecimal minWeight;

    /**
     * 最大权重限制
     */
    private BigDecimal maxWeight;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 备注说明
     */
    private String remarks;

    // ========== 预定义颜色方案 ==========

    /**
     * 预定义颜色列表（供前端选择）
     */
    public static final List<ColorScheme> PREDEFINED_COLORS = List.of(
            new ColorScheme("#1890ff", "蓝色"),
            new ColorScheme("#52c41a", "绿色"),
            new ColorScheme("#faad14", "橙色"),
            new ColorScheme("#f5222d", "红色"),
            new ColorScheme("#722ed1", "紫色"),
            new ColorScheme("#13c2c2", "青色"),
            new ColorScheme("#eb2f96", "粉红"),
            new ColorScheme("#fa8c16", "橙黄")
    );

    /**
     * 颜色方案内部类
     */
    @Data
    public static class ColorScheme {
        private String code;
        private String name;

        public ColorScheme(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
