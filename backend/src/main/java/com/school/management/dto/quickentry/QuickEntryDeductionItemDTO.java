package com.school.management.dto.quickentry;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 快捷录入 - 扣分项DTO
 * 只返回"按人次扣分"模式的扣分项
 *
 * @author system
 * @since 1.0.7
 */
@Data
public class QuickEntryDeductionItemDTO {

    /**
     * 扣分项ID
     */
    private Long id;

    /**
     * 扣分项名称
     */
    private String itemName;

    /**
     * 所属类别ID
     */
    private Long categoryId;

    /**
     * 所属类别名称
     */
    private String categoryName;

    /**
     * 基础扣分分数
     */
    private BigDecimal baseScore;

    /**
     * 每人扣分分数
     */
    private BigDecimal perPersonScore;

    /**
     * 是否允许上传照片
     */
    private Integer allowPhoto;

    /**
     * 是否允许添加备注
     */
    private Integer allowRemark;

    /**
     * 描述
     */
    private String description;

    /**
     * 参与的轮次，如"1,3"
     */
    private String participatedRounds;

    /**
     * 参与的轮次列表
     */
    private List<Integer> participatedRoundsList;
}
