package com.school.management.dto.rating;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 评级结果 VO
 *
 * @author System
 * @since 4.4.0
 */
@Data
public class RatingResultVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 评级配置ID
     */
    private Long ratingConfigId;

    /**
     * 评级名称
     */
    private String ratingName;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 检查计划名称
     */
    private String checkPlanName;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 评级周期类型
     */
    private String periodType;

    /**
     * 评级周期类型显示文本
     */
    private String periodTypeText;

    /**
     * 周期开始日期
     */
    private LocalDate periodStart;

    /**
     * 周期结束日期
     */
    private LocalDate periodEnd;

    /**
     * 周期显示文本（如：2024年第1周、2024年1月）
     */
    private String periodText;

    /**
     * 排名（1=第一名）
     */
    private Integer ranking;

    /**
     * 最终得分
     */
    private BigDecimal finalScore;

    /**
     * 是否获得该评级：0否 1是
     */
    private Integer awarded;

    /**
     * 状态：DRAFT/PENDING_APPROVAL/PUBLISHED
     */
    private String status;

    /**
     * 状态显示文本
     */
    private String statusText;

    /**
     * 计算时间
     */
    private LocalDateTime calculatedAt;

    /**
     * 审核人ID
     */
    private Long approvedBy;

    /**
     * 审核人姓名
     */
    private String approvedByName;

    /**
     * 审核时间
     */
    private LocalDateTime approvedAt;

    /**
     * 发布人ID
     */
    private Long publishedBy;

    /**
     * 发布人姓名
     */
    private String publishedByName;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
