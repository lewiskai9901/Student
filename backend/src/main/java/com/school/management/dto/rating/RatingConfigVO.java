package com.school.management.dto.rating;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评级配置 VO
 *
 * @author System
 * @since 4.4.0
 */
@Data
public class RatingConfigVO {

    /**
     * 主键ID
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
     * 评级名称
     */
    private String ratingName;

    /**
     * 评级周期类型：DAILY/WEEKLY/MONTHLY
     */
    private String ratingType;

    /**
     * 评级周期类型显示文本
     */
    private String ratingTypeText;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色（十六进制）
     */
    private String color;

    /**
     * 显示优先级
     */
    private Integer priority;

    /**
     * 划分方式
     */
    private String divisionMethod;

    /**
     * 划分方式显示文本
     */
    private String divisionMethodText;

    /**
     * 划分值
     */
    private BigDecimal divisionValue;

    /**
     * 是否需要审核：0否 1是
     */
    private Integer requireApproval;

    /**
     * 审核通过后自动发布：0否 1是
     */
    private Integer autoPublish;

    /**
     * 是否启用：0否 1是
     */
    private Integer enabled;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 规则说明
     */
    private String description;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建人姓名
     */
    private String createdByName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 排名数据源列表
     */
    private List<RatingRankingSourceVO> rankingSources;

    /**
     * 当前版本号
     */
    private Integer currentVersion;
}
