package com.school.management.dto.record;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 扣分明细响应DTO
 *
 * @author system
 * @since 2.0.0
 */
@Data
public class CheckRecordDeductionDTO {

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

    /**
     * 原日常检查明细ID
     */
    private Long dailyCheckDetailId;

    // ==================== 班级信息 ====================

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    // ==================== 检查类别 ====================

    /**
     * 检查类别ID
     */
    private Long categoryId;

    /**
     * 类别名称
     */
    private String categoryName;

    /**
     * 检查轮次
     */
    private Integer checkRound;

    // ==================== 扣分项信息 ====================

    /**
     * 扣分项ID
     */
    private Long deductionItemId;

    /**
     * 扣分项编码
     */
    private String deductionItemCode;

    /**
     * 扣分项名称
     */
    private String deductionItemName;

    // ==================== 扣分模式与计算 ====================

    /**
     * 扣分模式：1=固定扣分 2=按人数扣分 3=区间扣分
     */
    private Integer deductMode;

    /**
     * 扣分模式名称
     */
    private String deductModeName;

    /**
     * 基础分
     */
    private BigDecimal baseScore;

    /**
     * 每人扣分
     */
    private BigDecimal perPersonScore;

    /**
     * 最小扣分
     */
    private BigDecimal minScore;

    /**
     * 最大扣分
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
     * 涉及学生ID列表
     */
    private List<Long> studentIdList;

    /**
     * 涉及学生姓名列表
     */
    private List<String> studentNameList;

    // ==================== 关联对象 ====================

    /**
     * 关联类型：0=无 1=宿舍 2=教室
     */
    private Integer linkType;

    /**
     * 关联类型名称
     */
    private String linkTypeName;

    /**
     * 关联对象ID
     */
    private Long linkId;

    /**
     * 关联对象编号
     */
    private String linkCode;

    /**
     * 关联对象名称
     */
    private String linkName;

    // ==================== 证据材料 ====================

    /**
     * 照片URL列表
     */
    private List<String> photoUrlList;

    /**
     * 照片数量
     */
    private Integer photoCount;

    // ==================== 备注说明 ====================

    /**
     * 扣分备注
     */
    private String remark;

    // ==================== 检查人信息 ====================

    /**
     * 实际打分人ID
     */
    private Long checkerId;

    /**
     * 实际打分人姓名
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
     * 申诉状态名称
     */
    private String appealStatusName;

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
     * 是否已修订
     */
    private Boolean isRevised;

    /**
     * 原始扣分
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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
