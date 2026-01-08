package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 检查项字典实体类
 * 对应表: check_items
 *
 * @author Claude
 * @since 2025-11-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("check_items")
public class CheckItem extends BaseEntity {

    /**
     * 类别ID
     */
    private Long categoryId;

    /**
     * 检查项编码: "FLOOR_CLEAN"
     */
    private String itemCode;

    /**
     * 检查项名称: "地面整洁"
     */
    private String itemName;

    /**
     * 检查说明
     */
    private String itemDescription;

    /**
     * 扣分模式: 1=按次, 2=按人
     */
    private Integer deductMode;

    /**
     * 默认扣分: -5.00
     */
    private BigDecimal defaultDeductScore;

    /**
     * 最小扣分: -10.00
     */
    private BigDecimal minDeductScore;

    /**
     * 最大扣分: -1.00
     */
    private BigDecimal maxDeductScore;

    /**
     * 检查要点 (JSON数组字符串): ["地面无垃圾", "无污渍"]
     */
    private String checkPoints;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态: 1=启用, 0=禁用
     */
    private Integer status;
}
