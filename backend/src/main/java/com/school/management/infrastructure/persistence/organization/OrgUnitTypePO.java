package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织类型持久化对象
 */
@Data
@TableName("org_unit_types")
public class OrgUnitTypePO {

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
     * 父类型编码
     */
    private String parentTypeCode;

    /**
     * 层级顺序
     */
    private Integer levelOrder;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否教学单位
     */
    private Boolean isAcademic;

    /**
     * 是否可被检查
     */
    private Boolean canBeInspected;

    /**
     * 是否可有子级
     */
    private Boolean canHaveChildren;

    /**
     * 最大子级深度
     */
    private Integer maxDepth;

    /**
     * 是否系统预置
     */
    private Boolean isSystem;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 排序号
     */
    private Integer sortOrder;

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
