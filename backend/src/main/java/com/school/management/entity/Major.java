package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专业实体类
 */
@Data
@TableName("majors")
public class Major {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 专业编码
     */
    private String majorCode;

    /**
     * 所属组织单元ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orgUnitId;

    /**
     * 专业描述
     */
    private String description;

    /**
     * 状态(0-禁用 1-启用)
     */
    private Integer status;

    /**
     * 删除标志(0-未删除 1-已删除)
     */
    @TableLogic
    private Integer deleted;

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
     * 组织单元名称(非数据库字段)
     */
    @TableField(exist = false)
    private String orgUnitName;
}
