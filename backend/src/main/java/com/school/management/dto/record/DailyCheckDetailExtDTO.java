package com.school.management.dto.record;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 日常检查明细扩展DTO（用于生成检查记录时携带关联信息）
 *
 * @author system
 * @since 2.0.0
 */
@Data
public class DailyCheckDetailExtDTO {

    private Long id;

    /**
     * 日常检查ID
     */
    private Long dailyCheckId;

    /**
     * 检查轮次
     */
    private Integer checkRound;

    // ==================== 班级信息 ====================

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
     * 院系ID
     */
    private Long departmentId;

    /**
     * 院系名称
     */
    private String departmentName;

    /**
     * 班主任ID
     */
    private Long teacherId;

    /**
     * 班主任姓名
     */
    private String teacherName;

    // ==================== 类别信息 ====================

    /**
     * 检查类别ID
     */
    private Long categoryId;

    /**
     * 类别编码
     */
    private String categoryCode;

    /**
     * 类别名称
     */
    private String categoryName;

    /**
     * 类别类型
     */
    private String categoryType;

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

    /**
     * 扣分模式 1=固定 2=按人数 3=区间
     */
    private Integer deductMode;

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
    private BigDecimal maxScore;

    // ==================== 实际扣分 ====================

    /**
     * 实际扣分
     */
    private BigDecimal deductScore;

    /**
     * 违规人数
     */
    private Integer personCount;

    /**
     * 关联学生ID列表
     */
    private String studentIds;

    /**
     * 关联学生姓名
     */
    private String studentNames;

    // ==================== 关联对象 ====================

    /**
     * 关联类型 0=无 1=宿舍 2=教室
     */
    private Integer linkType;

    /**
     * 关联对象ID
     */
    private Long linkId;

    /**
     * 关联对象编号
     */
    private String linkNo;

    /**
     * 关联对象名称
     */
    private String linkName;

    // ==================== 检查人信息 ====================

    /**
     * 检查人ID
     */
    private Long checkerId;

    /**
     * 检查人姓名
     */
    private String checkerName;

    /**
     * 检查时间
     */
    private LocalDateTime checkTime;

    // ==================== 其他 ====================

    /**
     * 扣分说明
     */
    private String description;

    /**
     * 备注
     */
    private String remark;

    /**
     * 证据图片URLs
     */
    private String photoUrls;

    /**
     * 图片数量
     */
    private Integer photoCount;
}
