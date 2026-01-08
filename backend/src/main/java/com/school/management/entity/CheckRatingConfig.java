package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 检查评级配置实体类
 *
 * @author system
 * @since 3.1.0
 */
@Data
@TableName("check_rating_configs")
public class CheckRatingConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置名称
     */
    private String configName;

    /**
     * 评级方式(1按比例 2按分数段)
     */
    private Integer ratingMethod;

    /**
     * 分数基准(1总扣分 2总得分)
     */
    private Integer scoreBasis;

    /**
     * 状态(0禁用 1启用)
     */
    private Integer status;

    /**
     * 是否默认配置
     */
    private Integer isDefault;

    /**
     * 配置说明
     */
    private String description;

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
}
