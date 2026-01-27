package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专业持久化对象
 * 映射到 majors 表
 */
@Data
@TableName("majors")
public class MajorPO {

    @TableId(value = "id", type = IdType.AUTO)
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
     * 所属组织单元ID（映射到OrgUnit）
     */
    @TableField("org_unit_id")
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
}
