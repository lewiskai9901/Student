package com.school.management.dto.rating;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 评级频次汇总视图对象
 * 用于展示某个周期内的评级频次统计概览
 */
@Data
public class RatingFrequencySummaryVO {

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 检查计划名称
     */
    private String checkPlanName;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 周期类型
     */
    private String periodType;

    /**
     * 周期开始日期
     */
    private LocalDate periodStart;

    /**
     * 周期结束日期
     */
    private LocalDate periodEnd;

    /**
     * 周期标签
     */
    private String periodLabel;

    /**
     * 参与班级总数
     */
    private Integer totalClasses;

    /**
     * 评级总次数
     */
    private Integer totalRatings;

    /**
     * 各等级分布统计
     */
    private List<LevelDistributionVO> levelDistribution;

    /**
     * 各等级的TOP班级
     */
    private List<LevelTopClassesVO> levelTopClasses;

    /**
     * 等级分布统计
     */
    @Data
    public static class LevelDistributionVO {
        private Long levelId;
        private String levelName;
        private String levelColor;
        private Integer levelOrder;
        /**
         * 获得该等级的班级数
         */
        private Integer classCount;
        /**
         * 该等级的总频次
         */
        private Integer totalFrequency;
        /**
         * 占比(%)
         */
        private Double percentage;
    }

    /**
     * 等级TOP班级
     */
    @Data
    public static class LevelTopClassesVO {
        private Long levelId;
        private String levelName;
        private String levelColor;
        /**
         * TOP班级列表
         */
        private List<TopClassVO> topClasses;
    }

    /**
     * TOP班级信息
     */
    @Data
    public static class TopClassVO {
        private Long classId;
        private String className;
        private String gradeName;
        private Integer frequency;
        private Integer ranking;
    }
}
