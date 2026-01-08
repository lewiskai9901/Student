package com.school.management.entity.record;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检查记录申诉表实体（重构版）
 * 用于存储扣分项的申诉记录
 *
 * @author system
 * @since 2.0.0
 */
@Data
@TableName("check_record_appeals_new")
@Alias("RecordCheckRecordAppealNew")
public class CheckRecordAppealNew {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // ==================== 关联信息 ====================

    /**
     * 检查记录ID
     */
    private Long recordId;

    /**
     * 扣分明细ID
     */
    private Long deductionId;

    /**
     * 班级ID
     */
    private Long classId;

    // ==================== 申诉人信息 ====================

    /**
     * 申诉人ID
     */
    private Long appellantId;

    /**
     * 申诉人姓名
     */
    private String appellantName;

    /**
     * 申诉人角色：TEACHER/STUDENT
     */
    private String appellantRole;

    // ==================== 申诉内容 ====================

    /**
     * 申诉理由
     */
    private String appealReason;

    /**
     * 申诉证据（JSON格式，图片URLs等）
     */
    private String appealEvidence;

    // ==================== 处理信息 ====================

    /**
     * 状态：0=待处理 1=处理中 2=已通过 3=已驳回
     */
    private Integer status;

    /**
     * 处理人ID
     */
    private Long handlerId;

    /**
     * 处理人姓名
     */
    private String handlerName;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 处理结果说明
     */
    private String handleResult;

    // ==================== 扣分调整 ====================

    /**
     * 原扣分
     */
    private BigDecimal originalScore;

    /**
     * 调整后扣分（通过时）
     */
    private BigDecimal adjustedScore;

    // ==================== 时间戳 ====================

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ==================== 常量定义 ====================

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PROCESSING = 1;
    public static final int STATUS_APPROVED = 2;
    public static final int STATUS_REJECTED = 3;

    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_STUDENT = "STUDENT";
}
