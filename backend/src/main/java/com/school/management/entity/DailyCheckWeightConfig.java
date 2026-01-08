package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 日常检查加权配置关联实体（支持多加权配置）
 *
 * 一次日常检查可以关联多个加权配置，每个配置：
 * - 可以应用于特定的分类或扣分项
 * - 有独立的颜色标记用于前端展示
 * - 独立计算标准人数和权值
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_check_weight_configs")
public class DailyCheckWeightConfig extends BaseEntity {

    /**
     * 日常检查ID
     */
    private Long dailyCheckId;

    /**
     * 加权配置ID（关联class_weight_configs表）
     */
    private Long weightConfigId;

    /**
     * 颜色标记（16进制颜色码，如：#1890ff）
     * 用于前端展示区分不同的加权配置方案
     */
    private String colorCode;

    /**
     * 颜色名称（如：蓝色、绿色、橙色）
     * 便于用户识别
     */
    private String colorName;

    /**
     * 应用范围
     * CATEGORY - 分类级别（应用于整个分类下的所有扣分项）
     * ITEM - 扣分项级别（仅应用于特定扣分项）
     */
    private String applyScope;

    /**
     * 应用的分类ID列表（JSON数组格式）
     * 当applyScope=CATEGORY时有效
     * 例：[1994956485733502977, 1994956874348351490]
     */
    private String applyCategoryIds;

    /**
     * 应用的扣分项ID列表（JSON数组格式）
     * 当applyScope=ITEM时有效
     */
    private String applyItemIds;

    /**
     * 是否为默认配置
     * 当扣分项未被任何配置覆盖时，使用默认配置
     */
    private Integer isDefault;

    /**
     * 优先级（数字越大优先级越高）
     * 当扣分项被多个配置覆盖时，使用优先级最高的配置
     */
    private Integer priority;

    /**
     * 加权配置快照（JSON格式）
     * 保存创建时的配置完整信息，确保后续配置修改不影响历史记录
     */
    private String weightConfigSnapshot;

    /**
     * 计算后的标准人数
     * 根据标准人数模式计算得出，用于加权计算
     */
    private Integer calculatedStandardSize;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 备注说明
     */
    private String remarks;

    // ========== 关联字段（非数据库字段）==========

    /**
     * 加权配置名称（关联查询）
     */
    @TableField(exist = false)
    private String weightConfigName;

    /**
     * 加权模式（关联查询）
     * STANDARD - 标准人数折算
     * PER_CAPITA - 人均扣分
     * SEGMENT - 分段加权
     * NONE - 不加权
     */
    @TableField(exist = false)
    private String weightMode;

    /**
     * 加权模式描述（关联查询）
     */
    @TableField(exist = false)
    private String weightModeDesc;

    /**
     * 标准人数模式（关联查询）
     * FIXED - 固定标准人数
     * TARGET_AVERAGE - 目标班级平均人数
     * RANGE_AVERAGE - 范围内平均人数
     */
    @TableField(exist = false)
    private String standardSizeMode;

    /**
     * 标准人数（关联查询，来自class_weight_configs表）
     */
    @TableField(exist = false)
    private Integer standardSize;

    /**
     * 应用的分类名称列表（关联查询，用于展示）
     */
    @TableField(exist = false)
    private String applyCategoryNames;

    /**
     * 最小权重系数限制（关联查询）
     */
    @TableField(exist = false)
    private java.math.BigDecimal minWeight;

    /**
     * 最大权重系数限制（关联查询）
     */
    @TableField(exist = false)
    private java.math.BigDecimal maxWeight;

    /**
     * 是否启用权重限制（关联查询）
     */
    @TableField(exist = false)
    private Integer enableWeightLimit;
}
