package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 扣分项-学生关联实体类
 * 记录量化扣分项影响的具体学生及综测分数
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("check_record_item_students")
public class CheckRecordItemStudent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 扣分明细ID
     */
    private Long recordItemId;

    /**
     * 检查记录ID
     */
    private Long recordId;

    /**
     * 检查日期
     */
    private LocalDate checkDate;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 姓名
     */
    private String studentName;

    /**
     * 班级ID
     */
    private Long studentClassId;

    /**
     * 班级名称
     */
    private String studentClassName;

    /**
     * 宿舍ID
     */
    private Long studentDormitoryId;

    /**
     * 宿舍号
     */
    private String studentDormitoryNo;

    /**
     * 行为类型ID
     */
    private Long behaviorTypeId;

    /**
     * 行为编码
     */
    private String behaviorTypeCode;

    /**
     * 行为名称
     */
    private String behaviorTypeName;

    /**
     * 原始扣分
     */
    private BigDecimal originalDeductScore;

    /**
     * 扣分次数/人数
     */
    private Integer deductCount;

    /**
     * 德育影响分
     */
    private BigDecimal moralScore;

    /**
     * 智育影响分
     */
    private BigDecimal intellectualScore;

    /**
     * 体育影响分
     */
    private BigDecimal physicalScore;

    /**
     * 美育影响分
     */
    private BigDecimal aestheticScore;

    /**
     * 劳育影响分
     */
    private BigDecimal laborScore;

    /**
     * 发展素质影响分
     */
    private BigDecimal developmentScore;

    /**
     * 是否已同步到综测
     */
    private Integer syncedToEvaluation;

    /**
     * 同步时间
     */
    private LocalDateTime syncTime;

    /**
     * 综测周期ID
     */
    private Long evaluationPeriodId;

    /**
     * 确认状态: 0待确认,1已确认,2已排除
     */
    private Integer confirmed;

    /**
     * 确认人
     */
    private Long confirmedBy;

    /**
     * 确认时间
     */
    private LocalDateTime confirmedAt;

    /**
     * 排除原因
     */
    private String excludeReason;

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

    // ==================== 确认状态常量 ====================

    public static final int CONFIRMED_PENDING = 0;     // 待确认
    public static final int CONFIRMED_YES = 1;         // 已确认
    public static final int CONFIRMED_EXCLUDED = 2;    // 已排除
}
