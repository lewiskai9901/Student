package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 综测评定周期实体类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("evaluation_periods")
public class EvaluationPeriod implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 周期编码
     */
    private String periodCode;

    /**
     * 周期名称
     */
    private String periodName;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 学年
     */
    private String academicYear;

    /**
     * 学期类型
     */
    private Integer semesterType;

    /**
     * 数据采集开始日期
     */
    private LocalDate dataStartDate;

    /**
     * 数据采集截止日期
     */
    private LocalDate dataEndDate;

    /**
     * 荣誉申报开始日期
     */
    private LocalDate applicationStartDate;

    /**
     * 荣誉申报截止日期
     */
    private LocalDate applicationEndDate;

    /**
     * 审核开始日期
     */
    private LocalDate reviewStartDate;

    /**
     * 审核截止日期
     */
    private LocalDate reviewEndDate;

    /**
     * 公示开始日期
     */
    private LocalDate publicityStartDate;

    /**
     * 公示结束日期
     */
    private LocalDate publicityEndDate;

    /**
     * 申诉截止日期
     */
    private LocalDate appealEndDate;

    /**
     * 状态: 0未开始,1数据采集中,2荣誉申报中,3审核中,4公示中,5申诉处理中,6已结束
     */
    private Integer status;

    /**
     * 是否锁定
     */
    private Integer isLocked;

    /**
     * 锁定时间
     */
    private LocalDateTime lockedAt;

    /**
     * 锁定人
     */
    private Long lockedBy;

    /**
     * 维度权重配置(JSON)
     */
    private String dimensionWeights;

    /**
     * 特殊配置(JSON)
     */
    private String specialConfigs;

    /**
     * 备注
     */
    private String remarks;

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

    // ==================== 状态常量 ====================

    public static final int STATUS_NOT_STARTED = 0;       // 未开始
    public static final int STATUS_DATA_COLLECTION = 1;   // 数据采集中
    public static final int STATUS_APPLICATION = 2;       // 荣誉申报中
    public static final int STATUS_REVIEW = 3;            // 审核中
    public static final int STATUS_PUBLICITY = 4;         // 公示中
    public static final int STATUS_APPEAL = 5;            // 申诉处理中
    public static final int STATUS_FINISHED = 6;          // 已结束
}
