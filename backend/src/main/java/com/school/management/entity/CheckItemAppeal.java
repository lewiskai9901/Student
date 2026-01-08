package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检查项目申诉实体类
 * 统一的申诉管理,基于检查记录
 *
 * @author system
 * @since 2024-12-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("check_item_appeals")
public class CheckItemAppeal extends BaseEntity {

    // ========== 申诉基本信息 ==========

    /**
     * 申诉编号(格式:APPEAL-YYYYMMDD-XXX)
     */
    private String appealCode;

    // ========== 关联检查记录 ==========

    /**
     * 检查记录ID
     */
    private Long recordId;

    /**
     * 检查记录编号(冗余)
     */
    private String recordCode;

    /**
     * 班级统计ID(check_record_class_stats)
     */
    private Long classStatId;

    /**
     * 扣分明细ID
     */
    private Long itemId;

    // ========== 班级信息(冗余) ==========

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

    // ========== 扣分信息(冗余) ==========

    /**
     * 类别ID
     */
    private Long categoryId;

    /**
     * 类别名称
     */
    private String categoryName;

    /**
     * 扣分项名称
     */
    private String itemName;

    /**
     * 原始扣分
     */
    private BigDecimal originalScore;

    /**
     * 关联信息(宿舍/教室编号)
     */
    private String linkInfo;

    /**
     * 原始照片URLs(JSON数组)
     */
    private String originalPhotoUrls;

    /**
     * 原始备注
     */
    private String originalRemark;

    // ========== 申诉信息 ==========

    /**
     * 申诉类型(1=分数异议,2=事实异议,3=程序异议)
     */
    private Integer appealType;

    /**
     * 申诉理由
     */
    private String appealReason;

    /**
     * 期望分数
     */
    private BigDecimal expectedScore;

    /**
     * 申诉证据URLs(JSON数组)
     */
    private String evidenceUrls;

    /**
     * 申诉人ID(班主任)
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
     * 申诉时间
     */
    private LocalDateTime appealTime;

    /**
     * 申诉截止时间
     */
    private LocalDateTime appealDeadline;

    // ========== 审核信息 ==========

    /**
     * 申诉状态
     * 1=待审核, 2=审核通过, 3=审核驳回, 4=已撤销, 5=已过期, 6=公示中, 7=已生效
     */
    private Integer status;

    /**
     * 当前审批步骤
     */
    private Integer currentStep;

    /**
     * 最终审核人ID
     */
    private Long finalReviewerId;

    /**
     * 最终审核人姓名
     */
    private String finalReviewerName;

    /**
     * 最终审核时间
     */
    private LocalDateTime finalReviewTime;

    /**
     * 最终审核意见
     */
    private String finalReviewOpinion;

    // ========== 分数调整 ==========

    /**
     * 调整后分数
     */
    private BigDecimal adjustedScore;

    /**
     * 分数变化(调整后-原始)
     */
    private BigDecimal scoreChange;

    // ========== 公示信息 ==========

    /**
     * 公示开始时间
     */
    private LocalDateTime publicityStartTime;

    /**
     * 公示结束时间
     */
    private LocalDateTime publicityEndTime;

    /**
     * 公示天数
     */
    private Integer publicityDays;

    /**
     * 生效时间
     */
    private LocalDateTime effectiveTime;

    /**
     * 审批流程快照(JSON)
     */
    private String approvalFlowSnapshot;
}
