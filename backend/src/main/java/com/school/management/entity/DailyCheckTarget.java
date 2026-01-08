package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日常检查目标实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@TableName("daily_check_targets")
public class DailyCheckTarget {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 检查ID
     */
    private Long checkId;

    /**
     * 目标类型 1班级 2年级 3院系
     */
    private Integer targetType;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 目标名称(冗余字段)
     */
    private String targetName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
