package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 扣分项实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("deduction_items")
public class DeductionItem extends BaseEntity {

    /**
     * 所属量化类型ID
     */
    private Long typeId;

    /**
     * 扣分项名称
     */
    private String itemName;

    /**
     * 扣分模式: 1固定扣分 2按人数扣分 3区间扣分
     */
    private Integer deductMode;

    /**
     * 固定扣分分数(模式1使用)
     */
    private BigDecimal fixedScore;

    /**
     * 基础扣分分数(模式2使用)
     */
    private BigDecimal baseScore;

    /**
     * 每人扣分分数(模式2使用)
     */
    private BigDecimal perPersonScore;

    /**
     * 区间配置JSON(模式3使用)
     * 格式: [{"min": 0, "max": 10}]
     * 说明: 定义可扣分的数值区间,实际扣分时在此区间内任意输入
     */
    private String rangeConfig;

    /**
     * 描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态: 1启用 0禁用
     */
    private Integer status;

    /**
     * 是否允许上传照片: 1是 0否
     */
    private Integer allowPhoto;

    /**
     * 是否允许添加备注: 1是 0否
     */
    private Integer allowRemark;

    /**
     * 是否允许添加学生: 1是 0否 (仅按人次模式有效)
     */
    private Integer allowStudents;

    /**
     * 是否启用加权: 0否 1是
     */
    private Integer enableWeight;

    /**
     * 加权配置ID
     */
    private Long weightConfigId;
}
