package com.school.management.dto.rating;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评级规则视图对象
 *
 * @author system
 * @since 3.2.0
 */
@Data
public class RatingRuleVO {

    /**
     * 规则ID
     */
    private Long id;

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 检查计划名称
     */
    private String checkPlanName;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 评级类型：DAILY-单次检查评级，SUMMARY-汇总评级
     */
    private String ruleType;

    /**
     * 评级类型描述
     */
    private String ruleTypeText;

    /**
     * 评分来源：TOTAL-总分，CATEGORY-按类别
     */
    private String scoreSource;

    /**
     * 评分来源描述
     */
    private String scoreSourceText;

    /**
     * 类别ID
     */
    private Long categoryId;

    /**
     * 类别名称
     */
    private String categoryName;

    /**
     * 是否使用加权分数
     */
    private Integer useWeightedScore;

    /**
     * 划分方式
     */
    private String divisionMethod;

    /**
     * 划分方式描述
     */
    private String divisionMethodText;

    /**
     * 汇总方式
     */
    private String summaryMethod;

    /**
     * 汇总方式描述
     */
    private String summaryMethodText;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 是否启用
     */
    private Integer enabled;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建人姓名
     */
    private String creatorName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 评级等级列表
     */
    private List<RatingLevelVO> levels;

    /**
     * 获取评级类型描述
     */
    public String getRuleTypeText() {
        if (ruleType == null) return null;
        return switch (ruleType) {
            case "DAILY" -> "单次检查评级";
            case "SUMMARY" -> "汇总评级";
            default -> ruleType;
        };
    }

    /**
     * 获取评分来源描述
     */
    public String getScoreSourceText() {
        if (scoreSource == null) return null;
        return switch (scoreSource) {
            case "TOTAL" -> "总分";
            case "CATEGORY" -> "按类别";
            default -> scoreSource;
        };
    }

    /**
     * 获取划分方式描述
     */
    public String getDivisionMethodText() {
        if (divisionMethod == null) return null;
        return switch (divisionMethod) {
            case "SCORE_RANGE" -> "分数段";
            case "RANK_COUNT" -> "名次数量";
            case "PERCENTAGE" -> "百分比";
            default -> divisionMethod;
        };
    }

    /**
     * 获取汇总方式描述
     */
    public String getSummaryMethodText() {
        if (summaryMethod == null) return null;
        return switch (summaryMethod) {
            case "AVERAGE" -> "平均";
            case "SUM" -> "累加";
            default -> summaryMethod;
        };
    }
}
