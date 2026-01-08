package com.school.management.dto.record;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 类别检查统计响应DTO
 *
 * @author system
 * @since 2.0.0
 */
@Data
public class CheckRecordCategoryStatsDTO {

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
     * 班级ID
     */
    private Long classId;

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
     * 类别类型：HYGIENE/DISCIPLINE/ATTENDANCE/DORMITORY/OTHER
     */
    private String categoryType;

    /**
     * 类别类型名称
     */
    private String categoryTypeName;

    // ==================== 检查轮次 ====================

    /**
     * 检查轮次
     */
    private Integer checkRound;

    // ==================== 统计 ====================

    /**
     * 扣分项数量
     */
    private Integer deductionCount;

    /**
     * 该类别总扣分
     */
    private BigDecimal totalScore;

    /**
     * 该类别加权后扣分
     */
    private BigDecimal weightedTotalScore;

    // ==================== 关联数据 ====================

    /**
     * 该类别下的扣分明细
     */
    private List<CheckRecordDeductionDTO> deductions;
}
