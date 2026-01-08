package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检查扣分明细实体
 *
 * @author system
 * @since 1.0.6
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_check_details")
public class DailyCheckDetail extends BaseEntity {

    /**
     * 日常检查ID
     */
    private Long checkId;

    /**
     * 检查轮次(第几次检查)
     */
    private Integer checkRound;

    /**
     * 检查类别ID
     */
    private Long categoryId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 扣分项ID
     */
    private Long deductionItemId;

    /**
     * 扣分项名称(冗余)
     */
    private String deductionItemName;

    /**
     * 扣分模式 1=固定 2=按人数 3=区间
     */
    private Integer deductMode;

    /**
     * 关联类型 0=无关联 1=宿舍 2=教室
     */
    private Integer linkType;

    /**
     * 关联对象ID(宿舍ID或教室ID)
     */
    private Long linkId;

    /**
     * 关联对象编号
     */
    private String linkNo;

    /**
     * 实际扣分
     */
    private BigDecimal deductScore;

    /**
     * 违规人数(模式2使用)
     */
    private Integer personCount;

    /**
     * 关联学生ID列表(逗号分隔)
     */
    private String studentIds;

    /**
     * 关联学生姓名(逗号分隔)
     */
    private String studentNames;

    /**
     * 宿舍总人数(混寝分摊)
     */
    private Integer totalStudents;

    /**
     * 该班在宿舍的人数(混寝分摊)
     */
    private Integer classStudents;

    /**
     * 分数分摊比例(混寝分摊)
     */
    private BigDecimal scoreRatio;

    /**
     * 检查人ID
     */
    private Long checkerId;

    /**
     * 检查时间
     */
    private LocalDateTime checkTime;

    /**
     * 扣分说明/备注
     */
    private String description;

    /**
     * 证据图片URLs(JSON数组格式)
     */
    private String images;

    /**
     * 备注(与description字段功能重复,保留用于兼容性)
     */
    private String remark;

    /**
     * 图片URLs(与images字段功能重复,保留用于兼容性)
     */
    private String photoUrls;

    /**
     * 是否已修订 0=否 1=是
     */
    private Integer isRevised;

    /**
     * 原始扣分
     */
    private BigDecimal originalScore;

    /**
     * 修订后扣分
     */
    private BigDecimal revisedScore;

    /**
     * 修订原因
     */
    private String revisionReason;

    /**
     * 申诉状态 0=未申诉 1=申诉中 2=申诉通过 3=申诉驳回
     */
    private Integer appealStatus;
}
