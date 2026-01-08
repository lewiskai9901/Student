package com.school.management.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 申诉响应
 *
 * @author system
 * @since 1.0.6
 */
@Data
public class AppealResponse {

    /**
     * 申诉ID
     */
    private Long id;

    /**
     * 检查ID
     */
    private Long checkId;

    /**
     * 明细ID
     */
    private Long detailId;

    /**
     * 类别名称
     */
    private String categoryName;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 扣分项名称
     */
    private String deductionItemName;

    /**
     * 原扣分分数
     */
    private BigDecimal originalScore;

    /**
     * 申诉原因
     */
    private String appealReason;

    /**
     * 申诉人ID
     */
    private Long appealUserId;

    /**
     * 申诉人姓名
     */
    private String appealUserName;

    /**
     * 申诉时间
     */
    private LocalDateTime appealTime;

    /**
     * 申诉照片URL
     */
    private String appealPhotoUrls;

    /**
     * 状态 (0=待处理, 1=处理中, 2=通过, 3=驳回)
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 修正后的分数
     */
    private BigDecimal revisedScore;

    /**
     * 审核意见
     */
    private String reviewOpinion;

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
    private LocalDateTime reviewTime;
}
