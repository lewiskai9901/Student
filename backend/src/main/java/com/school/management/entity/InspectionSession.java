package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 检查批次实体
 *
 * @author system
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inspection_sessions")
public class InspectionSession extends BaseEntity {

    /**
     * 关联的日常检查ID
     */
    private Long dailyCheckId;

    /**
     * 检查批次编号
     */
    private String sessionCode;

    /**
     * 检查日期
     */
    private LocalDate inspectionDate;

    /**
     * 检查时间
     */
    private LocalTime inspectionTime;

    /**
     * 检查人ID
     */
    private Long inspectorId;

    /**
     * 检查人姓名
     */
    private String inspectorName;

    /**
     * 年级ID(可选,用于筛选)
     */
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 检查目标总数
     */
    private Integer totalTargets;

    /**
     * 总扣分
     */
    private BigDecimal totalDeductions;

    /**
     * 状态:1草稿 2待审核 3已发布
     */
    private Integer status;

    /**
     * 审核人ID
     */
    private Long reviewerId;

    /**
     * 审核人姓名
     */
    private String reviewerName;

    /**
     * 审核时间
     */
    private LocalDateTime reviewedAt;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 备注
     */
    private String remarks;
}
