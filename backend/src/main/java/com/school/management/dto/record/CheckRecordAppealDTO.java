package com.school.management.dto.record;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 检查记录申诉响应DTO
 *
 * @author system
 * @since 2.0.0
 */
@Data
public class CheckRecordAppealDTO {

    private Long id;

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
     * 申诉人角色
     */
    private String appellantRole;

    /**
     * 申诉人角色名称
     */
    private String appellantRoleName;

    // ==================== 申诉内容 ====================

    /**
     * 申诉理由
     */
    private String appealReason;

    /**
     * 申诉证据（图片URLs）
     */
    private List<String> appealEvidenceList;

    // ==================== 处理信息 ====================

    /**
     * 状态：0=待处理 1=处理中 2=已通过 3=已驳回
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

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
     * 调整后扣分
     */
    private BigDecimal adjustedScore;

    /**
     * 扣分差值
     */
    private BigDecimal scoreDiff;

    // ==================== 时间戳 ====================

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ==================== 关联数据 ====================

    /**
     * 关联的扣分明细
     */
    private CheckRecordDeductionDTO deduction;
}
