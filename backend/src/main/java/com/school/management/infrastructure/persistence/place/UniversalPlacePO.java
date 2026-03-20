package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通用空间持久化对象
 */
@Data
@TableName("places")
public class UniversalPlacePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 空间编码
     */
    private String placeCode;

    /**
     * 空间名称
     */
    private String placeName;

    /**
     * 空间类型编码
     */
    private String typeCode;

    /**
     * 描述
     */
    private String description;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 物化路径
     */
    private String path;

    /**
     * 层级深度
     */
    private Integer level;

    /**
     * 容量
     */
    private Integer capacity;

    /**
     * 当前占用数
     */
    private Integer currentOccupancy;

    /**
     * 所属组织单元ID
     */
    private Long orgUnitId;

    /**
     * 负责人ID
     */
    private Long responsibleUserId;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 性别限制
     */
    private String gender;

    /**
     * 扩展属性（JSON）
     */
    private String attributes;

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

    // ==================== 非数据库字段（关联查询用） ====================

    /**
     * 类型名称
     */
    @TableField(exist = false)
    private String typeName;

    /**
     * 父级名称
     */
    @TableField(exist = false)
    private String parentName;

    /**
     * 组织单元名称
     */
    @TableField(exist = false)
    private String orgUnitName;

    /**
     * 负责人名称
     */
    @TableField(exist = false)
    private String responsibleUserName;
}
