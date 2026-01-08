package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 扣分项明细实体
 *
 * @author system
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_deduction_items")
public class InspectionDeductionItem extends BaseEntity {

    /**
     * 检查类别ID
     */
    private Long categoryId;

    /**
     * 检查目标ID
     */
    private Long targetId;

    /**
     * 检查批次ID
     */
    private Long sessionId;

    /**
     * 扣分项名称
     */
    private String itemName;

    /**
     * 扣分分值
     */
    private BigDecimal deductScore;

    /**
     * 涉及人数(适用于按人头扣分)
     */
    private Integer personCount;

    /**
     * 扣分原因详情
     */
    private String deductReason;

    /**
     * 证据图片
     */
    private String evidenceImages;

    /**
     * 逻辑删除标志 - inspection_deduction_items表中不存在此字段
     */
    @TableField(exist = false)
    private Integer deleted;
}
