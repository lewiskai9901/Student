package com.school.management.dto.rating;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建评级规则DTO
 *
 * @author system
 * @since 3.2.0
 */
@Data
public class RatingRuleCreateDTO {

    /**
     * 检查计划ID
     */
    @NotNull(message = "检查计划ID不能为空")
    private Long checkPlanId;

    /**
     * 规则名称
     */
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    /**
     * 评级类型：DAILY-单次检查评级，SUMMARY-汇总评级
     */
    @NotBlank(message = "评级类型不能为空")
    private String ruleType;

    /**
     * 评分来源：TOTAL-总分，CATEGORY-按类别
     */
    @NotBlank(message = "评分来源不能为空")
    private String scoreSource;

    /**
     * 类别ID（当scoreSource=CATEGORY时使用）
     */
    private Long categoryId;

    /**
     * 是否使用加权分数：0-原始扣分，1-加权扣分
     */
    private Integer useWeightedScore = 0;

    /**
     * 划分方式：SCORE_RANGE-分数段，RANK_COUNT-名次数量，PERCENTAGE-百分比
     */
    @NotBlank(message = "划分方式不能为空")
    private String divisionMethod;

    /**
     * 汇总方式（仅SUMMARY类型）：AVERAGE-平均，SUM-累加
     */
    private String summaryMethod = "AVERAGE";

    /**
     * 排序序号
     */
    private Integer sortOrder = 0;

    /**
     * 是否启用
     */
    private Integer enabled = 1;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 评级等级列表
     */
    @NotEmpty(message = "评级等级不能为空")
    @Valid
    private List<RatingLevelDTO> levels;
}
