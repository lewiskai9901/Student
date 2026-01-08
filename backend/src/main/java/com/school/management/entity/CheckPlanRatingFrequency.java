package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 评级频次统计实体
 * 记录每个班级在指定周期内获得各等级的次数
 */
@Data
@TableName("check_plan_rating_frequency")
public class CheckPlanRatingFrequency {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称(冗余)
     */
    private String className;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 年级名称(冗余)
     */
    private String gradeName;

    /**
     * 评级规则ID
     */
    private Long ruleId;

    /**
     * 规则名称(冗余)
     */
    private String ruleName;

    /**
     * 等级ID
     */
    private Long levelId;

    /**
     * 等级名称
     */
    private String levelName;

    /**
     * 等级颜色
     */
    private String levelColor;

    /**
     * 获得该等级的次数
     */
    private Integer frequency;

    /**
     * 参与评级的总次数
     */
    private Integer totalRatings;

    /**
     * 频次占比(%)
     */
    private BigDecimal frequencyRate;

    /**
     * 统计周期类型: WEEK MONTH QUARTER SEMESTER YEAR CUSTOM
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
     * 周期标签: 2024年第1周, 2024年1月等
     */
    private String periodLabel;

    /**
     * 最近一次获得该等级的日期
     */
    private LocalDate lastRatingDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 周期类型常量
    public static final String PERIOD_WEEK = "WEEK";
    public static final String PERIOD_MONTH = "MONTH";
    public static final String PERIOD_QUARTER = "QUARTER";
    public static final String PERIOD_SEMESTER = "SEMESTER";
    public static final String PERIOD_YEAR = "YEAR";
    public static final String PERIOD_CUSTOM = "CUSTOM";
}
