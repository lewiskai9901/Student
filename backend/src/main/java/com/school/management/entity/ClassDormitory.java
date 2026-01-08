package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 班级宿舍关联实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("class_dormitory_bindings")
public class ClassDormitory {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 班级ID
     */
    @TableField("class_id")
    private Long classId;

    /**
     * 宿舍ID
     */
    @TableField("dormitory_id")
    private Long dormitoryId;

    /**
     * 学生数量
     */
    @TableField("student_count")
    private Integer studentCount;

    /**
     * 创建时间
     */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 分配床位数 (非数据库字段，用于兼容性)
     */
    @TableField(exist = false)
    private Integer allocatedBeds;

    /**
     * 备注 (非数据库字段)
     */
    @TableField(exist = false)
    private String notes;

    /**
     * 状态 (非数据库字段)
     */
    @TableField(exist = false)
    private Integer status;

    /**
     * 逻辑删除标志 (非数据库字段，用于兼容BaseEntity的deleted字段使用)
     */
    @TableField(exist = false)
    private Integer deleted;

    /**
     * 创建人ID (非数据库字段)
     */
    @TableField(exist = false)
    private Long createdBy;

    /**
     * 更新人ID (非数据库字段)
     */
    @TableField(exist = false)
    private Long updatedBy;
}
