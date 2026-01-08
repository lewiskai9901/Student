package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生处分实体类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("student_punishments")
public class StudentPunishment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 处分编号
     */
    private String punishmentCode;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 处分类型: 1警告,2严重警告,3记过,4留校察看,5开除学籍
     */
    private Integer punishmentType;

    /**
     * 处分名称
     */
    private String punishmentName;

    /**
     * 处分原因
     */
    private String reason;

    /**
     * 处分决定日期
     */
    private LocalDate decisionDate;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 是否已解除
     */
    private Integer revoked;

    /**
     * 解除日期
     */
    private LocalDate revokeDate;

    /**
     * 解除原因
     */
    private String revokeReason;

    /**
     * 综测影响类型: 1限制上限, 2直接扣分
     */
    private Integer evaluationEffectType;

    /**
     * 德育分上限
     */
    private BigDecimal moralScoreCap;

    /**
     * 德育扣分
     */
    private BigDecimal moralDeductScore;

    /**
     * 文件号
     */
    private String documentNo;

    /**
     * 附件列表(JSON)
     */
    private String attachments;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 录入人
     */
    private Long inputBy;

    /**
     * 录入时间
     */
    private LocalDateTime inputAt;

    /**
     * 状态: 1有效, 0无效
     */
    private Integer status;

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
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;

    // ==================== 处分类型常量 ====================

    public static final int TYPE_WARNING = 1;              // 警告
    public static final int TYPE_SERIOUS_WARNING = 2;      // 严重警告
    public static final int TYPE_DEMERIT = 3;              // 记过
    public static final int TYPE_PROBATION = 4;            // 留校察看
    public static final int TYPE_EXPULSION = 5;            // 开除学籍

    // ==================== 综测影响类型常量 ====================

    public static final int EFFECT_SCORE_CAP = 1;          // 限制上限
    public static final int EFFECT_DEDUCT = 2;             // 直接扣分

    /**
     * 获取处分对应的德育分数上限
     * 根据《学生综合素质测评办法》规定
     */
    public BigDecimal getDefaultMoralScoreCap() {
        switch (this.punishmentType) {
            case TYPE_WARNING:
                return new BigDecimal("85");
            case TYPE_SERIOUS_WARNING:
                return new BigDecimal("80");
            case TYPE_DEMERIT:
                return new BigDecimal("75");
            case TYPE_PROBATION:
                return new BigDecimal("70");
            case TYPE_EXPULSION:
                return new BigDecimal("0");
            default:
                return new BigDecimal("100");
        }
    }
}
