package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 通报记录实体
 *
 * @author system
 * @since 4.2.0
 */
@Data
@TableName("notification_records")
public class NotificationRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 检查计划ID
     */
    private Long planId;

    /**
     * 检查计划ID（评级通报使用）- 实际使用planId字段
     */
    @TableField(exist = false)
    private Long checkPlanId;

    /**
     * 使用的模板ID
     */
    private Long templateId;

    /**
     * 通报类型
     * SINGLE_CHECK - 单次检查通报
     * MULTI_ROUND - 多轮次通报
     * MULTI_CHECK - 多检查汇总通报
     * RATING - 评级通报
     */
    private String notificationType;

    /**
     * 关联的日常检查ID列表（JSON数组）
     */
    private String dailyCheckIds;

    /**
     * 关联的检查记录ID列表（JSON数组）
     */
    private String checkRecordIds;

    /**
     * 选择的轮次（JSON数组）
     */
    private String checkRounds;

    /**
     * 选择的扣分项ID列表（JSON数组）
     */
    private String deductionItemIds;

    /**
     * 评级结果ID列表（JSON数组，评级通报时使用）
     */
    private String ratingResultIds;

    /**
     * 通报标题
     */
    private String title;

    /**
     * 通报内容快照（渲染后的HTML）
     */
    private String contentSnapshot;

    /**
     * 使用的变量值（JSON格式）
     */
    private String variableValues;

    /**
     * 涉及人数/班级数
     */
    private Integer totalCount;

    /**
     * 涉及班级数
     */
    private Integer totalClasses;

    /**
     * 扣分条目数
     */
    private Integer totalDeductionCount;

    /**
     * 涉及班级数量（评级通报）- 冗余字段，使用totalClasses
     */
    @TableField(exist = false)
    private Integer classCount;

    /**
     * 统计周期开始日期（评级通报）- 业务字段，不映射数据库
     */
    @TableField(exist = false)
    private LocalDate periodStart;

    /**
     * 统计周期结束日期（评级通报）- 业务字段，不映射数据库
     */
    @TableField(exist = false)
    private LocalDate periodEnd;

    /**
     * 文件格式: PDF/WORD/EXCEL
     */
    private String fileFormat;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 状态: 0-编辑中, 1-已完成, 2-生成失败
     */
    private Integer status;

    /**
     * 发布状态: 0-草稿, 1-已发布
     */
    private Integer publishStatus;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 生成完成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

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

    // ========== 常量 ==========

    public static final String TYPE_SINGLE_CHECK = "SINGLE_CHECK";
    public static final String TYPE_MULTI_ROUND = "MULTI_ROUND";
    public static final String TYPE_MULTI_CHECK = "MULTI_CHECK";
    public static final String TYPE_RATING = "RATING";

    public static final int STATUS_EDITING = 0;
    public static final int STATUS_COMPLETED = 1;
    public static final int STATUS_FAILED = 2;

    // 发布状态
    public static final int PUBLISH_STATUS_DRAFT = 0;
    public static final int PUBLISH_STATUS_PUBLISHED = 1;
}
