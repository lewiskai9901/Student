package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 综测维度配置实体类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("evaluation_dimensions")
public class EvaluationDimension implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 维度编码: MORAL/INTELLECTUAL/PHYSICAL/AESTHETIC/LABOR/DEVELOPMENT
     */
    private String dimensionCode;

    /**
     * 维度名称
     */
    private String dimensionName;

    /**
     * 权重百分比
     */
    private BigDecimal weight;

    /**
     * 基础分
     */
    private BigDecimal baseScore;

    /**
     * 奖励分上限
     */
    private BigDecimal maxBonusScore;

    /**
     * 维度最低分
     */
    private BigDecimal minTotalScore;

    /**
     * 维度最高分
     */
    private BigDecimal maxTotalScore;

    /**
     * 计算公式说明
     */
    private String calculationFormula;

    /**
     * 排序
     */
    private Integer sortOrder;

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

    // ==================== 维度编码常量 ====================

    public static final String MORAL = "MORAL";                 // 德育
    public static final String INTELLECTUAL = "INTELLECTUAL";   // 智育
    public static final String PHYSICAL = "PHYSICAL";           // 体育
    public static final String AESTHETIC = "AESTHETIC";         // 美育
    public static final String LABOR = "LABOR";                 // 劳育
    public static final String DEVELOPMENT = "DEVELOPMENT";     // 发展素质
}
