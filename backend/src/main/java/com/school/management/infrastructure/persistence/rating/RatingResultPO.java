package com.school.management.infrastructure.persistence.rating;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 评价结果持久化对象
 */
@Data
@TableName("rating_results")
public class RatingResultPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 评价配置ID
     */
    private Long ratingConfigId;

    /**
     * 检查计划ID
     */
    private Long checkPlanId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 周期类型
     */
    private String periodType;

    /**
     * 周期开始日期
     */
    private LocalDate periodStart;

    /**
     * 周期结束日期
     */
    private LocalDate periodEnd;

    /**
     * 排名
     */
    private Integer ranking;

    /**
     * 最终分数
     */
    private BigDecimal finalScore;

    /**
     * 是否获奖
     */
    private Boolean awarded;

    /**
     * 状态: CALCULATED, SUBMITTED, APPROVED, REJECTED, PUBLISHED, REVOKED
     */
    private String status;

    /**
     * 计算时间
     */
    private LocalDateTime calculatedAt;

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;

    /**
     * 审批人
     */
    private Long approvedBy;

    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;

    /**
     * 审批意见
     */
    private String approvalComment;

    /**
     * 发布人
     */
    private Long publishedBy;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 撤销人
     */
    private Long revokedBy;

    /**
     * 撤销时间
     */
    private LocalDateTime revokedAt;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;
}
