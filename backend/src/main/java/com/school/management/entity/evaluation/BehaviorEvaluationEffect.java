package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 行为-综测映射实体类
 * 定义每种行为对综测各维度的影响规则
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("behavior_evaluation_effects")
public class BehaviorEvaluationEffect implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 行为类型ID
     */
    private Long behaviorTypeId;

    /**
     * 综测维度
     */
    private String evaluationDimension;

    /**
     * 计分类型: 1固定分, 2按次累计, 3按次递增, 4按等级映射
     */
    private Integer scoreType;

    /**
     * 基础分数(正数加分,负数扣分)
     */
    private BigDecimal baseScore;

    /**
     * 累计上限(正向行为)
     */
    private BigDecimal maxScore;

    /**
     * 累计下限(负向行为)
     */
    private BigDecimal minScore;

    /**
     * 递增分数(用于按次递增)
     */
    private BigDecimal incrementScore;

    /**
     * 触发条件(JSON格式)
     */
    private String triggerCondition;

    /**
     * 等级分数映射(JSON格式)
     */
    private String levelScoreMapping;

    /**
     * 优先级(同一事迹取最高)
     */
    private Integer priority;

    /**
     * 有效学期数(1=仅当期)
     */
    private Integer effectiveSemesters;

    /**
     * 状态: 1启用, 0禁用
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

    // ==================== 计分类型常量 ====================

    public static final int SCORE_TYPE_FIXED = 1;        // 固定分
    public static final int SCORE_TYPE_CUMULATIVE = 2;   // 按次累计
    public static final int SCORE_TYPE_INCREMENT = 3;    // 按次递增
    public static final int SCORE_TYPE_LEVEL = 4;        // 按等级映射
}
