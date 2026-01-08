package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 荣誉等级配置实体类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("honor_level_configs")
public class HonorLevelConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 荣誉类型ID
     */
    private Long honorTypeId;

    /**
     * 级别编码: NATIONAL/PROVINCIAL/CITY/SCHOOL/DEPARTMENT
     */
    private String levelCode;

    /**
     * 级别名称
     */
    private String levelName;

    /**
     * 名次编码: FIRST/SECOND/THIRD/EXCELLENCE/PARTICIPATION
     */
    private String rankCode;

    /**
     * 名次名称
     */
    private String rankName;

    /**
     * 加分分值
     */
    private BigDecimal score;

    /**
     * 同类最大计次(null不限)
     */
    private Integer maxCount;

    /**
     * 优先级(同一事迹取最高)
     */
    private Integer priority;

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

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;

    // ==================== 级别编码常量 ====================

    public static final String LEVEL_NATIONAL = "NATIONAL";       // 国家级
    public static final String LEVEL_PROVINCIAL = "PROVINCIAL";   // 省级
    public static final String LEVEL_CITY = "CITY";               // 市级
    public static final String LEVEL_SCHOOL = "SCHOOL";           // 校级
    public static final String LEVEL_DEPARTMENT = "DEPARTMENT";   // 系级

    // ==================== 名次编码常量 ====================

    public static final String RANK_FIRST = "FIRST";              // 一等奖
    public static final String RANK_SECOND = "SECOND";            // 二等奖
    public static final String RANK_THIRD = "THIRD";              // 三等奖
    public static final String RANK_EXCELLENCE = "EXCELLENCE";    // 优秀奖
    public static final String RANK_PARTICIPATION = "PARTICIPATION"; // 参与奖
}
