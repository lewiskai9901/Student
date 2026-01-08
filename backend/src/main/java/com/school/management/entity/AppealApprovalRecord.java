package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申诉审批记录实体类
 * 每个审批步骤一条记录
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
@TableName("appeal_approval_records")
public class AppealApprovalRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 申诉ID
     */
    private Long appealId;

    /**
     * 审批步骤序号
     */
    private Integer stepOrder;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批人角色
     */
    private String approverRole;

    /**
     * 审批结果(1=待审批,2=同意,3=驳回,4=转交)
     */
    private Integer approvalStatus;

    /**
     * 审批意见
     */
    private String approvalOpinion;

    /**
     * 审批时间
     */
    private LocalDateTime approvalTime;

    /**
     * 建议调整分数
     */
    private BigDecimal suggestedScore;

    /**
     * 接收时间
     */
    private LocalDateTime receivedTime;

    /**
     * 超时小时数
     */
    private Integer timeoutHours;

    /**
     * 是否超时
     */
    private Integer isTimeout;

    /**
     * 转交给谁
     */
    private Long transferredTo;

    /**
     * 转交原因
     */
    private String transferReason;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 申诉信息
     */
    @TableField(exist = false)
    private CheckItemAppeal appeal;

    /**
     * 审批人信息
     */
    @TableField(exist = false)
    private User approver;

    /**
     * 转交对象信息
     */
    @TableField(exist = false)
    private User transferredUser;

    /**
     * 审批状态描述
     */
    @TableField(exist = false)
    private String approvalStatusDesc;

    /**
     * 耗时(小时)
     */
    @TableField(exist = false)
    private Long durationHours;

    /**
     * 获取审批状态描述
     */
    public String getApprovalStatusDesc() {
        if (approvalStatus == null) {
            return "未知";
        }
        switch (approvalStatus) {
            case 1:
                return "待审批";
            case 2:
                return "同意";
            case 3:
                return "驳回";
            case 4:
                return "转交";
            default:
                return "未知";
        }
    }

    /**
     * 计算耗时
     */
    public Long getDurationHours() {
        if (receivedTime == null || approvalTime == null) {
            return null;
        }
        return java.time.Duration.between(receivedTime, approvalTime).toHours();
    }
}
