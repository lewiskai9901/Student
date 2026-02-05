package com.school.management.infrastructure.persistence.space;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通用空间类型持久化对象
 */
@Data
@TableName("space_types")
public class UniversalSpaceTypePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 类型编码
     */
    private String typeCode;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 图标
     */
    private String icon;

    /**
     * 描述
     */
    private String description;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 是否系统预置
     */
    private Boolean isSystem;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 允许的子类型（JSON数组）
     */
    private String allowedChildTypes;

    /**
     * 是否可作为根节点
     */
    private Boolean isRootType;

    /**
     * 是否有容量
     */
    private Boolean hasCapacity;

    /**
     * 是否可预订
     */
    private Boolean bookable;

    /**
     * 是否可分配给组织
     */
    private Boolean assignable;

    /**
     * 是否可入住/占用
     */
    private Boolean occupiable;

    /**
     * 容量单位
     */
    private String capacityUnit;

    /**
     * 默认容量
     */
    private Integer defaultCapacity;

    /**
     * 扩展属性Schema
     */
    private String attributeSchema;

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
}
