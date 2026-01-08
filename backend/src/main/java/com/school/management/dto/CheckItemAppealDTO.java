package com.school.management.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 申诉详情DTO
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class CheckItemAppealDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 申诉ID
     */
    private Long id;

    /**
     * 申诉编号
     */
    private String appealCode;

    /**
     * 检查记录ID
     */
    private Long recordId;

    /**
     * 扣分明细ID
     */
    private Long itemId;

    /**
     * 申诉人ID
     */
    private Long appellantId;

    /**
     * 申诉人姓名
     */
    private String applicantName;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 原始分数
     */
    private BigDecimal originalScore;

    /**
     * 调整后分数
     */
    private BigDecimal adjustedScore;

    /**
     * 分数变化
     */
    private BigDecimal scoreChange;

    /**
     * 申诉原因
     */
    private String reason;

    /**
     * 期望分数
     */
    private BigDecimal expectedScore;

    /**
     * 申诉类型(1=分数有误,2=情节特殊,3=流程不当,4=其他)
     */
    private Integer appealType;

    /**
     * 申诉类型描述
     */
    private String appealTypeDesc;

    /**
     * 状态(1=待审核,2=审核通过,3=审核驳回,4=已撤销,5=已过期,6=公示中,7=已生效)
     */
    private Integer status;

    /**
     * 状态描述
     */
    private String statusDesc;

    /**
     * 当前审批步骤
     */
    private Integer currentStep;

    /**
     * 总审批步骤
     */
    private Integer totalSteps;

    /**
     * 当前审批人ID
     */
    private Long currentApproverId;

    /**
     * 当前审批人姓名
     */
    private String currentApproverName;

    /**
     * 审批开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalStartTime;

    /**
     * 审批结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalEndTime;

    /**
     * 公示开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publicityStartTime;

    /**
     * 公示结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publicityEndTime;

    /**
     * 生效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveTime;

    /**
     * 撤销时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime withdrawTime;

    /**
     * 撤销原因
     */
    private String withdrawReason;

    /**
     * 证明材料
     */
    private List<AttachmentDTO> evidences;

    /**
     * 证明材料描述
     */
    private String evidenceDescription;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 是否紧急
     */
    private Boolean isUrgent;

    /**
     * 是否公示中
     */
    private Boolean inPublicity;

    /**
     * 是否可撤销
     */
    private Boolean canWithdraw;

    /**
     * 是否可编辑
     */
    private Boolean canEdit;

    /**
     * 审批记录列表
     */
    private List<AppealApprovalRecordDTO> approvalRecords;

    /**
     * 审计日志列表
     */
    private List<AppealAuditLogDTO> auditLogs;

    /**
     * 检查记录信息
     */
    private CheckRecordBasicDTO checkRecord;

    /**
     * 扣分明细信息
     */
    private CheckRecordItemBasicDTO checkItem;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // ========== 内部DTO类 ==========

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

    /**
     * 检查记录基本信息DTO
     */
    @Data
    public static class CheckRecordBasicDTO implements Serializable {
        private Long id;
        private String checkCode;
        private String checkName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime checkTime;
        private String checkerName;
    }

    /**
     * 扣分明细基本信息DTO
     */
    @Data
    public static class CheckRecordItemBasicDTO implements Serializable {
        private Long id;
        private String itemName;
        private String categoryName;
        private BigDecimal deductionScore;
        private String deductionReason;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime recordTime;
    }
}
