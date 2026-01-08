package com.school.management.entity.record;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 类别检查统计表实体（重构版）
 * 用于按检查类别聚合统计
 *
 * @author system
 * @since 2.0.0
 */
@Data
@TableName("check_record_category_stats_new")
@Alias("RecordCheckRecordCategoryStatsNew")
public class CheckRecordCategoryStatsNew {

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
     * 班级ID
     */
    private Long classId;

    // ==================== 类别信息快照 ====================

    /**
     * 检查类别ID
     */
    private Long categoryId;

    /**
     * 类别编码（快照）
     */
    private String categoryCode;

    /**
     * 类别名称（快照）
     */
    private String categoryName;

    /**
     * 类别类型：HYGIENE/DISCIPLINE/ATTENDANCE/DORMITORY/OTHER
     */
    private String categoryType;

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

    // ==================== 时间戳 ====================

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // ==================== 类别类型常量 ====================

    public static final String TYPE_HYGIENE = "HYGIENE";
    public static final String TYPE_DISCIPLINE = "DISCIPLINE";
    public static final String TYPE_ATTENDANCE = "ATTENDANCE";
    public static final String TYPE_DORMITORY = "DORMITORY";
    public static final String TYPE_OTHER = "OTHER";
}
