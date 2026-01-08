package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类别加权规则实体类
 * 支持不同检查类别使用不同的加权策略
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
@TableName("category_weight_rules")
public class CategoryWeightRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 加权配置ID
     */
    private Long weightConfigId;

    /**
     * 量化类型ID(quantification_types)
     */
    private Long categoryId;

    /**
     * 该类别是否启用加权
     */
    private Integer enableWeight;

    /**
     * 该类别的加权模式(覆盖全局配置)
     */
    private String weightMode;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 类别名称
     */
    @TableField(exist = false)
    private String categoryName;

    /**
     * 加权配置名称
     */
    @TableField(exist = false)
    private String configName;
}
