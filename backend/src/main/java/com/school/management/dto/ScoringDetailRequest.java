package com.school.management.dto;

import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 扣分明细请求
 *
 * @author system
 * @since 1.0.6
 */
@Data
public class ScoringDetailRequest {

    /**
     * 检查轮次
     */
    private Integer checkRound;

    /**
     * 类别ID
     */
    @NotNull(message = "类别ID不能为空")
    private Long categoryId;

    /**
     * 班级ID
     */
    @NotNull(message = "班级ID不能为空")
    private Long classId;

    /**
     * 扣分项ID
     */
    @NotNull(message = "扣分项ID不能为空")
    private Long deductionItemId;

    /**
     * 关联类型 (1=学生, 2=宿舍, 3=教室)
     */
    @NotNull(message = "关联类型不能为空")
    private Integer linkType;

    /**
     * 关联ID (学生ID/宿舍ID/教室ID)
     */
    @NotNull(message = "关联ID不能为空")
    private Long linkId;

    /**
     * 扣分分数
     */
    @NotNull(message = "扣分分数不能为空")
    @DecimalMin(value = "0.0", message = "扣分分数不能为负数")
    private BigDecimal deductScore;

    /**
     * 涉及人数
     */
    private Integer personCount;

    /**
     * 关联学生ID列表(逗号分隔)
     */
    private String studentIds;

    /**
     * 关联学生姓名(逗号分隔)
     */
    private String studentNames;

    /**
     * 详细描述
     */
    private String description;

    /**
     * 备注
     */
    private String remark;

    /**
     * 照片URL(多个用逗号分隔)
     */
    private String photoUrls;
}
