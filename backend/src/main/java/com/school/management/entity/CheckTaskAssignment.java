package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检查任务分配实体
 */
@Data
@TableName("check_task_assignments")
public class CheckTaskAssignment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 日常检查ID
     */
    private Long dailyCheckId;

    /**
     * 检查计划ID
     */
    private Long planId;

    /**
     * 被分配的打分人员用户ID
     */
    private Long userId;

    /**
     * 分配的检查类别ID列表JSON
     */
    private String categoryIds;

    /**
     * 分配的班级ID列表JSON
     */
    private String classIds;

    /**
     * 任务状态：0待处理 1进行中 2已完成
     */
    private Integer status;

    /**
     * 是否已通知：0否 1是
     */
    private Integer notified;

    /**
     * 通知时间
     */
    private LocalDateTime notifiedAt;

    /**
     * 开始检查时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // ========== 非数据库字段 ==========

    /**
     * 用户姓名（关联查询）
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 日常检查名称（关联查询）
     */
    @TableField(exist = false)
    private String checkName;

    /**
     * 检查日期（关联查询）
     */
    @TableField(exist = false)
    private String checkDate;

    /**
     * 计划名称（关联查询）
     */
    @TableField(exist = false)
    private String planName;

    // ========== 任务状态常量 ==========
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_IN_PROGRESS = 1;
    public static final int STATUS_COMPLETED = 2;
}
