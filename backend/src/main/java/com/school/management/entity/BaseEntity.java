package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类
 *
 * @author system
 * @since 1.0.0
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 创建人ID
     * 注意: 数据库中不存在此字段,仅用于业务逻辑
     */
    @TableField(exist = false)
    private Long createdBy;

    /**
     * 更新人ID
     * 注意: 数据库中不存在此字段,仅用于业务逻辑
     */
    @TableField(exist = false)
    private Long updatedBy;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    // Alias methods for compatibility
    public LocalDateTime getCreatedTime() {
        return createdAt;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdAt = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedAt;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedAt = updatedTime;
    }
}