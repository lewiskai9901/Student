package com.school.management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申诉审批记录DTO
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class AppealApprovalRecordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录ID
     */
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
     * 步骤名称
     */
    private String stepName;

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
     * 审批人角色名称
     */
    private String approverRoleName;

    /**
     * 审批结果(1=待审批,2=同意,3=驳回,4=转交)
     */
    private Integer approvalStatus;

    /**
     * 审批结果描述
     */
    private String approvalStatusDesc;

    /**
     * 审批意见
     */
    private String approvalOpinion;

    /**
     * 建议调整分数
     */
    private BigDecimal suggestedScore;

    /**
     * 接收时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receivedTime;

    /**
     * 审批时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalTime;

    /**
     * 耗时(小时)
     */
    private Long durationHours;

    /**
     * 超时小时数
     */
    private Integer timeoutHours;

    /**
     * 是否超时
     */
    private Boolean isTimeout;

    /**
     * 转交给谁
     */
    private Long transferredTo;

    /**
     * 转交对象姓名
     */
    private String transferredToName;

    /**
     * 转交原因
     */
    private String transferReason;

    /**
     * 附件列表
     */
    private java.util.List<AttachmentDTO> attachments;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

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
     * 附件DTO
     */
    @Data
    public static class AttachmentDTO implements Serializable {
        private Long id;
        private String fileName;
        private String fileUrl;
        private String fileType;
        private Long fileSize;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime uploadTime;
    }
}
