package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 检查类别字典实体类
 * 对应表: check_categories
 *
 * @author Claude
 * @since 2025-11-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("check_categories")
public class CheckCategory extends BaseEntity {

    /**
     * 类别编码: "HYGIENE", "DISCIPLINE"
     */
    private String categoryCode;

    /**
     * 类别名称: "卫生检查", "纪律检查"
     */
    private String categoryName;

    /**
     * 类别类型: "HYGIENE", "DISCIPLINE", "OTHER"
     */
    private String categoryType;

    /**
     * 默认满分: 100.00
     */
    private BigDecimal defaultMaxScore;

    /**
     * 描述
     */
    private String description;

    /**
     * 图标: "🏠", "📚"
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 默认检查轮次
     */
    private Integer defaultRounds;

    /**
     * 状态: 1=启用, 0=禁用
     */
    private Integer status;
}
