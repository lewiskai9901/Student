package com.school.management.dto.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申诉列表VO
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class AppealListVO implements Serializable {

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
     * 申诉人姓名
     */
    private String applicantName;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 检查名称
     */
    private String checkName;

    /**
     * 扣分项目
     */
    private String itemName;

    /**
     * 申诉原因
     */
    private String reason;

    /**
     * 期望分数
     */
    private BigDecimal expectedScore;

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
     * 审批进度描述(如"第1步/共2步")
     */
    private String progressDesc;

    /**
     * 当前审批人姓名
     */
    private String currentApproverName;

    /**
     * 是否紧急
     */
    private Boolean isUrgent;

    /**
     * 是否超时
     */
    private Boolean isTimeout;

    /**
     * 是否公示中
     */
    private Boolean inPublicity;

    /**
     * 公示剩余天数
     */
    private Integer publicityRemainingDays;

    /**
     * 是否可撤销
     */
    private Boolean canWithdraw;

    /**
     * 申诉时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 审批结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalEndTime;

    /**
     * 生效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveTime;

    /**
     * 获取审批进度描述
     */
    public String getProgressDesc() {
        if (currentStep == null || totalSteps == null) {
            return "-";
        }
        return String.format("第%d步/共%d步", currentStep, totalSteps);
    }

    /**
     * 获取申诉类型描述
     */
    public String getAppealTypeDesc() {
        if (appealType == null) {
            return "未知";
        }
        switch (appealType) {
            case 1:
                return "分数有误";
            case 2:
                return "情节特殊";
            case 3:
                return "流程不当";
            case 4:
                return "其他";
            default:
                return "未知";
        }
    }

    /**
     * 获取状态描述
     */
    public String getStatusDesc() {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1:
                return "待审核";
            case 2:
                return "审核通过";
            case 3:
                return "审核驳回";
            case 4:
                return "已撤销";
            case 5:
                return "已过期";
            case 6:
                return "公示中";
            case 7:
                return "已生效";
            default:
                return "未知";
        }
    }
}
