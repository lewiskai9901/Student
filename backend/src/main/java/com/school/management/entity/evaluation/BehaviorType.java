package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 行为类型字典实体类
 * 作为量化系统与综测系统的桥梁
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("behavior_types")
public class BehaviorType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 行为编码
     */
    private String behaviorCode;

    /**
     * 行为名称
     */
    private String behaviorName;

    /**
     * 行为大类: ATTENDANCE/DISCIPLINE/HYGIENE/STUDY/ACTIVITY/HONOR
     */
    private String behaviorCategory;

    /**
     * 行为性质: 1正向, 2负向, 3中性
     */
    private Integer behaviorNature;

    /**
     * 默认影响范围: 1仅当事人, 2宿舍全员, 3班级全员
     */
    private Integer defaultAffectScope;

    /**
     * 行为描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态: 1启用, 0禁用
     */
    private Integer status;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    private Long updatedBy;

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

    // ==================== 行为类别常量 ====================

    public static final String CATEGORY_ATTENDANCE = "ATTENDANCE";  // 考勤
    public static final String CATEGORY_DISCIPLINE = "DISCIPLINE";  // 纪律
    public static final String CATEGORY_HYGIENE = "HYGIENE";        // 卫生
    public static final String CATEGORY_STUDY = "STUDY";            // 学习
    public static final String CATEGORY_ACTIVITY = "ACTIVITY";      // 活动
    public static final String CATEGORY_HONOR = "HONOR";            // 荣誉

    // ==================== 行为性质常量 ====================

    public static final int NATURE_POSITIVE = 1;   // 正向
    public static final int NATURE_NEGATIVE = 2;   // 负向
    public static final int NATURE_NEUTRAL = 3;    // 中性

    // ==================== 影响范围常量 ====================

    public static final int SCOPE_INDIVIDUAL = 1;  // 仅当事人
    public static final int SCOPE_DORMITORY = 2;   // 宿舍全员
    public static final int SCOPE_CLASS = 3;       // 班级全员
}
