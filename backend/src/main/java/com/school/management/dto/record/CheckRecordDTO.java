package com.school.management.dto.record;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 检查记录响应DTO
 *
 * @author system
 * @since 2.0.0
 */
@Data
public class CheckRecordDTO {

    private Long id;

    /**
     * 记录编号
     */
    private String recordCode;

    /**
     * 关联的检查计划ID
     */
    private Long planId;

    /**
     * 关联的日常检查ID
     */
    private Long dailyCheckId;

    /**
     * 检查名称
     */
    private String checkName;

    /**
     * 检查日期
     */
    private LocalDate checkDate;

    /**
     * 检查类型：1=日常检查 2=专项检查
     */
    private Integer checkType;

    /**
     * 检查类型名称
     */
    private String checkTypeName;

    /**
     * 检查人ID
     */
    private Long checkerId;

    /**
     * 检查人姓名
     */
    private String checkerName;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    // ==================== 统计信息 ====================

    /**
     * 涉及班级总数
     */
    private Integer totalClasses;

    /**
     * 扣分条数
     */
    private Integer totalDeductionCount;

    /**
     * 总扣分
     */
    private BigDecimal totalDeductionScore;

    /**
     * 平均扣分
     */
    private BigDecimal avgScore;

    /**
     * 最高扣分
     */
    private BigDecimal maxScore;

    /**
     * 最低扣分
     */
    private BigDecimal minScore;

    // ==================== 状态信息 ====================

    /**
     * 状态：1=已发布 2=已归档
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 归档时间
     */
    private LocalDateTime archiveTime;

    // ==================== 申诉统计 ====================

    /**
     * 申诉总数
     */
    private Integer totalAppealCount;

    /**
     * 待处理申诉数
     */
    private Integer appealPendingCount;

    /**
     * 通过的申诉数
     */
    private Integer appealApprovedCount;

    /**
     * 驳回的申诉数
     */
    private Integer appealRejectedCount;

    // ==================== 快照元数据 ====================

    /**
     * 快照版本
     */
    private Integer snapshotVersion;

    /**
     * 最后重算时间
     */
    private LocalDateTime lastRecalcAt;

    // ==================== 备注 ====================

    /**
     * 检查说明
     */
    private String description;

    /**
     * 备注
     */
    private String remarks;

    // ==================== 时间戳 ====================

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ==================== 关联数据 ====================

    /**
     * 班级统计列表
     */
    private List<CheckRecordClassStatsDTO> classStats;
}
