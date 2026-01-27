package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专业方向持久化对象
 * 映射到 major_directions 表
 */
@Data
@TableName("major_directions")
public class MajorDirectionPO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属专业ID
     */
    private Long majorId;

    /**
     * 方向名称
     */
    private String directionName;

    /**
     * 方向编码
     */
    private String directionCode;

    /**
     * 层次(中级工/高级工/预备技师/技师)
     */
    @TableField("level")
    private String level;

    /**
     * 学制(年)
     */
    private Integer years;

    /**
     * 是否分段注册(0=否,1=是)
     */
    private Integer isSegmented;

    /**
     * 第一阶段层次
     */
    private String phase1Level;

    /**
     * 第一阶段学制(年)
     */
    private Integer phase1Years;

    /**
     * 第二阶段层次
     */
    private String phase2Level;

    /**
     * 第二阶段学制(年)
     */
    private Integer phase2Years;

    /**
     * 备注
     */
    private String remarks;

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
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 更新人ID
     */
    private Long updatedBy;

    /**
     * 删除标志(0=未删除,1=已删除)
     */
    @TableLogic
    private Integer deleted;
}
