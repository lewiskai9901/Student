package com.school.management.entity.record;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 检查记录主表实体（重构版）
 * 用于存储日常检查完成后的完整快照数据
 *
 * @author system
 * @since 2.0.0
 */
@Data
@TableName("check_records_new")
@Alias("RecordCheckRecordNew")
public class CheckRecordNew {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属检查计划ID
     */
    private Long planId;

    // ==================== 基本信息 ====================

    /**
     * 记录编号，格式：CR + 日期 + 序号，如 CR20241130001
     */
    private String recordCode;

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

    // ==================== 检查人快照 ====================

    /**
     * 检查人ID
     */
    private Long checkerId;

    /**
     * 检查人姓名（快照）
     */
    private String checkerName;

    // ==================== 模板快照 ====================

    /**
     * 使用的模板ID
     */
    private Long templateId;

    /**
     * 模板名称（快照）
     */
    private String templateName;

    /**
     * 模板配置完整快照（JSON格式）
     */
    private String templateSnapshot;

    // ==================== 统计快照 ====================

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

    // ==================== 状态 ====================

    /**
     * 状态：1=已发布 2=已归档
     */
    private Integer status;

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
     * 快照版本（重算时递增）
     */
    private Integer snapshotVersion;

    /**
     * 快照创建时间
     */
    private LocalDateTime snapshotCreatedAt;

    /**
     * 最后重算时间
     */
    private LocalDateTime lastRecalcAt;

    /**
     * 重算原因
     */
    private String recalcReason;

    // ==================== 备注 ====================

    /**
     * 检查说明
     */
    private String description;

    /**
     * 备注
     */
    private String remarks;

    // ==================== 审计字段 ====================

    /**
     * 创建人ID
     */
    @TableField(value = "created_by")
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    // ==================== 状态常量 ====================

    public static final int STATUS_PUBLISHED = 1;
    public static final int STATUS_ARCHIVED = 2;

    public static final int CHECK_TYPE_DAILY = 1;
    public static final int CHECK_TYPE_SPECIAL = 2;
}
