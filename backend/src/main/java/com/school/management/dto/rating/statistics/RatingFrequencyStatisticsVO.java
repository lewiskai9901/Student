package com.school.management.dto.rating.statistics;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 评级频次统计视图对象（核心统计VO）
 * 用于评级次数统计中心页面的主要数据展示
 *
 * @author Claude Code
 * @since 2025-12-22
 */
@Data
public class RatingFrequencyStatisticsVO {

    // 基本信息
    private Long checkPlanId;
    private String planName;
    private Long ruleId;
    private String ruleName;

    // 统计周期
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String periodLabel;        // 格式化标签，如"2025年12月"
    private String periodType;          // WEEK/MONTH/SEMESTER/CUSTOM

    // 概览数据
    private Integer totalRatings;       // 总评级次数
    private Integer awardedClasses;     // 获奖班级数
    private Integer totalClasses;       // 参评班级总数
    private Integer statisticsDays;     // 统计天数

    // 按等级分组的统计
    private List<LevelFrequencyVO> levelStatistics;

    // 院系对比
    private List<DepartmentComparisonVO> departmentComparison;

    // 计算时间
    private String calculatedAt;        // 统计计算时间

    /**
     * 等级频次统计VO
     */
    @Data
    public static class LevelFrequencyVO {
        private Long levelId;
        private String levelName;
        private String levelColor;
        private String levelIcon;
        private Integer levelOrder;

        private Integer totalFrequency;    // 该等级总次数
        private BigDecimal percentage;     // 占比（相对于总评级次数）
        private Integer classCount;        // 获得该等级的班级数

        // TOP3班级
        private List<ClassFrequencySimpleVO> topClasses;
    }

    /**
     * 院系对比VO
     */
    @Data
    public static class DepartmentComparisonVO {
        private Long departmentId;
        private String departmentName;
        private Integer classCount;        // 该院系班级数
        private Integer excellentCount;    // 获得优秀的班级数
        private BigDecimal excellentRate;  // 优秀率
        private Integer ranking;           // 院系排名
    }

    /**
     * 班级频次简化VO（用于TOP展示）
     */
    @Data
    public static class ClassFrequencySimpleVO {
        private Long classId;
        private String className;
        private String departmentName;
        private Integer frequency;         // 获得次数
        private BigDecimal rate;           // 获得率
        private Integer ranking;           // 排名
    }
}
