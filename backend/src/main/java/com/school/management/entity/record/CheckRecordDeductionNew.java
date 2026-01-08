package com.school.management.entity.record;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 扣分明细表实体（重构版）
 * 用于存储最细粒度的扣分快照数据
 *
 * @author system
 * @since 2.0.0
 */
@Data
@TableName("check_record_deductions_new")
@Alias("RecordCheckRecordDeductionNew")
public class CheckRecordDeductionNew {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 检查记录ID
     */
    private Long recordId;

    /**
     * 班级统计ID
     */
    private Long classStatId;

    /**
     * 类别统计ID
     */
    private Long categoryStatId;

    // ==================== 来源追溯 ====================

    /**
     * 原日常检查明细ID
     */
    private Long dailyCheckDetailId;

    // ==================== 班级信息（快照） ====================

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称（快照）
     */
    private String className;

    // ==================== 检查类别快照 ====================

    /**
     * 检查类别ID
     */
    private Long categoryId;

    /**
     * 类别名称（快照）
     */
    private String categoryName;

    /**
     * 检查轮次
     */
    private Integer checkRound;

    // ==================== 扣分项快照 ====================

    /**
     * 扣分项ID
     */
    private Long deductionItemId;

    /**
     * 扣分项编码（快照）
     */
    private String deductionItemCode;

    /**
     * 扣分项名称（快照）
     */
    private String deductionItemName;

    // ==================== 扣分模式与计算 ====================

    /**
     * 扣分模式：1=固定扣分 2=按人数扣分 3=区间扣分
     */
    private Integer deductMode;

    /**
     * 基础分（模式2使用）
     */
    private BigDecimal baseScore;

    /**
     * 每人扣分（模式2使用）
     */
    private BigDecimal perPersonScore;

    /**
     * 最小扣分（模式3使用）
     */
    private BigDecimal minScore;

    /**
     * 最大扣分（模式3使用）
     */
    private BigDecimal maxScoreLimit;

    /**
     * 实际扣分
     */
    private BigDecimal actualScore;

    // ==================== 涉及人员 ====================

    /**
     * 违规人数
     */
    private Integer personCount;

    /**
     * 涉及学生ID列表（逗号分隔）
     */
    private String studentIds;

    /**
     * 涉及学生姓名列表（快照）
     */
    private String studentNames;

    // ==================== 关联对象（宿舍/教室） ====================

    /**
     * 关联类型：0=无 1=宿舍 2=教室
     */
    private Integer linkType;

    /**
     * 关联对象ID
     */
    private Long linkId;

    /**
     * 关联对象编号
     */
    private String linkCode;

    /**
     * 关联对象名称（快照）
     */
    private String linkName;

    // ==================== 证据材料 ====================

    /**
     * 照片URL列表（JSON数组）
     */
    private String photoUrls;

    /**
     * 照片数量
     */
    private Integer photoCount;

    // ==================== 备注说明 ====================

    /**
     * 扣分备注/说明
     */
    private String remark;

    // ==================== 检查人信息 ====================

    /**
     * 实际打分人ID
     */
    private Long checkerId;

    /**
     * 实际打分人姓名（快照）
     */
    private String checkerName;

    /**
     * 打分时间
     */
    private LocalDateTime checkTime;

    // ==================== 申诉状态 ====================

    /**
     * 申诉状态：0=未申诉 1=申诉中 2=已通过 3=已驳回
     */
    private Integer appealStatus;

    /**
     * 关联的申诉记录ID
     */
    private Long appealId;

    /**
     * 申诉结果说明
     */
    private String appealResultRemark;

    // ==================== 修订信息 ====================

    /**
     * 是否已修订：0=否 1=是
     */
    private Integer isRevised;

    /**
     * 原始扣分（修订前）
     */
    private BigDecimal originalScore;

    /**
     * 修订后扣分
     */
    private BigDecimal revisedScore;

    /**
     * 修订时间
     */
    private LocalDateTime revisionTime;

    /**
     * 修订原因
     */
    private String revisionReason;

    // ==================== 时间戳 ====================

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ==================== 常量定义 ====================

    public static final int DEDUCT_MODE_FIXED = 1;
    public static final int DEDUCT_MODE_PER_PERSON = 2;
    public static final int DEDUCT_MODE_RANGE = 3;

    public static final int LINK_TYPE_NONE = 0;
    public static final int LINK_TYPE_DORMITORY = 1;
    public static final int LINK_TYPE_CLASSROOM = 2;

    public static final int APPEAL_STATUS_NONE = 0;
    public static final int APPEAL_STATUS_PENDING = 1;
    public static final int APPEAL_STATUS_APPROVED = 2;
    public static final int APPEAL_STATUS_REJECTED = 3;

    public static final int NOT_REVISED = 0;
    public static final int REVISED = 1;
}
