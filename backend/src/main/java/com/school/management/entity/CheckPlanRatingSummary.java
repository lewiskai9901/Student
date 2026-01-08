package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 评级汇总统计实体
 */
@Data
@TableName(value = "check_plan_rating_summary", autoResultMap = true)
public class CheckPlanRatingSummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 评级规则ID
     */
    private Long ruleId;

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
     * 等级分布JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> levelDistribution;

    /**
     * 各等级TOP班级JSON
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> topClasses;

    /**
     * 计算时间
     */
    private LocalDateTime calculatedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
